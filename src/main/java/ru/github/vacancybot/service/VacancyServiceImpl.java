package ru.github.vacancybot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.github.vacancybot.client.VacancyApiClient;
import ru.github.vacancybot.exception.VacancyApiException;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;
import ru.github.vacancybot.repository.VacancyRepository;
import ru.github.vacancybot.service.vacancy.VacancyMatcher;
import ru.github.vacancybot.service.vacancy.VacancyPage;
import ru.github.vacancybot.service.vacancy.VacancyParser;
import ru.github.vacancybot.service.vacancy.VacancySorter;

@Slf4j
@RequiredArgsConstructor
public class VacancyServiceImpl implements VacancyService {

  private static final int MAX_PAGES = 10;
  private static final int BROAD_SEARCH_LIMIT = 1000;
  private static final int FIRST_REGION_CODE = 1;
  private static final int LAST_REGION_CODE = 95;

  private final VacancyApiClient apiClient;
  private final VacancyParser parser;
  private final VacancyRepository vacancyRepository;

  @Override
  public List<Vacancy> fetchAndSave(SearchTask task) {
    VacancyMatcher matcher = new VacancyMatcher(task);
    List<Vacancy> found = task.isBroadSearch()
        ? fetchAllRegions(matcher)
        : fetchByFilters(task, matcher);
    List<Vacancy> sorted = found.stream().sorted(VacancySorter.forTask(task)).toList();
    vacancyRepository.saveAll(task.getId(), sorted);
    return sorted;
  }

  @Override
  public void deleteForTask(String taskId) {
    vacancyRepository.delete(taskId);
  }

  private List<Vacancy> fetchAllRegions(VacancyMatcher matcher) {
    List<Vacancy> result = new ArrayList<>();
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<List<Vacancy>>> futures = IntStream
          .rangeClosed(FIRST_REGION_CODE, LAST_REGION_CODE)
          .mapToObj(code -> executor.submit(() -> fetchRegionQuietly(code, matcher)))
          .toList();
      for (Future<List<Vacancy>> future : futures) {
        if (result.size() >= BROAD_SEARCH_LIMIT) {
          future.cancel(true);
          continue;
        }
        result.addAll(awaitQuietly(future));
      }
    }
    return result.size() > BROAD_SEARCH_LIMIT
        ? new ArrayList<>(result.subList(0, BROAD_SEARCH_LIMIT))
        : result;
  }

  private List<Vacancy> fetchRegionQuietly(int regionCode, VacancyMatcher matcher) {
    try {
      return matcher.select(parser.parse(apiClient.fetchRegion(regionCode)));
    } catch (VacancyApiException e) {
      log.debug("Region {} skipped: {}", regionCode, e.getMessage());
      return List.of();
    }
  }

  private static List<Vacancy> awaitQuietly(Future<List<Vacancy>> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return List.of();
    } catch (ExecutionException e) {
      log.warn("Region fetch failed: {}", e.getCause().getMessage());
      return List.of();
    }
  }


  private List<Vacancy> fetchByFilters(SearchTask task, VacancyMatcher matcher) {
    List<Vacancy> result = new ArrayList<>();
    int total = Integer.MAX_VALUE;
    int offset = 0;
    int page = 0;
    while (page < MAX_PAGES && offset < total) {
      try {
        String json = apiClient.fetchPage(task, offset);
        VacancyPage pageData = parser.parsePage(json);
        if (page == 0) {
          total = pageData.total();
          if (total == 0) {
            break;
          }
        }
        List<Vacancy> pageVacancies = matcher.select(pageData.vacancies());
        if (pageVacancies.isEmpty()) {
          break;
        }
        result.addAll(pageVacancies);
        offset += VacancyApiClient.PAGE_LIMIT;
        page++;
      } catch (VacancyApiException e) {
        log.error("Error on page {}: {}", page, e.getMessage());
        break;
      }
    }
    return result;
  }
}
