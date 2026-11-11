package ru.github.vacancybot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.client.VacancyApiClient;
import ru.github.vacancybot.exception.VacancyApiException;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;
import ru.github.vacancybot.repository.VacancyRepository;
import ru.github.vacancybot.service.vacancy.VacancyParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("VacancyServiceImpl — оркестрация поиска")
class VacancyServiceImplTest {

  @Mock VacancyApiClient apiClient;
  @Mock VacancyRepository repository;

  private VacancyServiceImpl service;

  private static String page(int count, int total, String prefix) {
    String items = IntStream.range(0, count).mapToObj(i -> """
        {"vacancy":{"job-name":"%s%d","vac_url":"http://v/%d",
          "salary_min":%d,"salary_max":0,"region_name":"Москва",
          "requirement":{"experience":"Без опыта"}}}""".formatted(prefix, i, i, 1000 * (i + 1)))
        .collect(Collectors.joining(","));
    return "{\"status\":\"200\",\"meta\":{\"total\":" + total + "},\"results\":{\"vacancies\":[" + items + "]}}";
  }

  private static SearchTask keywordTask() {
    return SearchTask.builder().id("t").keyword("job").regionName(SearchTask.ALL_REGIONS)
        .salaryMax(-1).build();
  }

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    service = new VacancyServiceImpl(apiClient, new VacancyParser(), repository);
  }

  @Test
  void deleteForTaskDelegates() {
    service.deleteForTask("x");

    verify(repository).delete("x");
  }

  @Nested
  @DisplayName("поиск с фильтрами (постранично)")
  class Filtered {

    @Test
    @DisplayName("несколько страниц: пока offset < total и страницы не пустые")
    void paginates() {
      SearchTask task = keywordTask();
      when(apiClient.fetchPage(task, 0)).thenReturn(page(100, 150, "job"));
      when(apiClient.fetchPage(task, 100)).thenReturn(page(50, 150, "job"));

      List<Vacancy> result = service.fetchAndSave(task);

      assertThat(result).hasSize(150);
      verify(apiClient, never()).fetchPage(task, 200);
      verify(repository).saveAll(eq("t"), eq(result));
    }

    @Test
    @DisplayName("total=0 — одна страница, пустой результат сохраняется")
    void zeroTotal() {
      SearchTask task = keywordTask();
      when(apiClient.fetchPage(task, 0)).thenReturn(page(0, 0, "job"));

      assertThat(service.fetchAndSave(task)).isEmpty();
      verify(apiClient, times(1)).fetchPage(any(), anyInt());
      verify(repository).saveAll("t", List.of());
    }

    @Test
    @DisplayName("пустая страница после фильтрации прерывает цикл")
    void emptyPageStops() {
      SearchTask task = keywordTask();
      when(apiClient.fetchPage(task, 0)).thenReturn(page(100, 1000, "other"));

      assertThat(service.fetchAndSave(task)).isEmpty();
      verify(apiClient, times(1)).fetchPage(any(), anyInt());
    }

    @Test
    @DisplayName("ошибка API на второй странице — результат первой сохраняется")
    void apiErrorKeepsPartialResult() {
      SearchTask task = keywordTask();
      when(apiClient.fetchPage(task, 0)).thenReturn(page(100, 500, "job"));
      when(apiClient.fetchPage(task, 100)).thenThrow(new VacancyApiException("x"));

      assertThat(service.fetchAndSave(task)).hasSize(100);
    }

    @Test
    @DisplayName("не более MAX_PAGES=10 страниц")
    void maxPages() {
      SearchTask task = keywordTask();
      when(apiClient.fetchPage(eq(task), anyInt())).thenReturn(page(100, 100_000, "job"));

      assertThat(service.fetchAndSave(task)).hasSize(1000);
      verify(apiClient, times(10)).fetchPage(eq(task), anyInt());
    }

    @Test
    @DisplayName("результат отсортирован по убыванию зарплаты")
    void sorted() {
      SearchTask task = keywordTask();
      when(apiClient.fetchPage(task, 0)).thenReturn(page(3, 3, "job"));

      List<Vacancy> result = service.fetchAndSave(task);

      assertThat(result).extracting(Vacancy::salaryMin).containsExactly(3000, 2000, 1000);
    }
  }

  @Nested
  @DisplayName("широкий поиск (все регионы параллельно)")
  class Broad {

    private final SearchTask broad = SearchTask.builder().id("b").keyword("")
        .regionName(SearchTask.ALL_REGIONS).salaryMax(-1).build();

    @Test
    @DisplayName("опрашивает регионы 1..95 через fetchRegion, fetchPage не используется")
    void fetchesAllRegions() {
      when(apiClient.fetchRegion(anyInt())).thenReturn(page(2, 2, "v"));

      List<Vacancy> result = service.fetchAndSave(broad);

      assertThat(result).hasSize(190);
      verify(apiClient, times(95)).fetchRegion(anyInt());
      verify(apiClient, never()).fetchPage(any(), anyInt());
    }

    @Test
    @DisplayName("результат обрезается до 1000")
    void cappedAtThousand() {
      when(apiClient.fetchRegion(anyInt())).thenReturn(page(100, 100, "v"));

      assertThat(service.fetchAndSave(broad)).hasSize(1000);
    }

    @Test
    @DisplayName("ошибки отдельных регионов пропускаются")
    void regionErrorsSkipped() {
      when(apiClient.fetchRegion(anyInt())).thenAnswer(inv -> {
        int code = inv.getArgument(0);
        if (code % 2 == 0) {
          throw new VacancyApiException("down");
        }
        return page(1, 1, "v");
      });

      assertThat(service.fetchAndSave(broad)).hasSize(48);
    }

    @Test
    @DisplayName("HTML вместо JSON (капча/ошибка) — регион даёт пустой результат")
    @SuppressWarnings("unchecked")
    void htmlResponseIgnored() {
      when(apiClient.fetchRegion(anyInt())).thenReturn("<html>blocked</html>");

      assertThat(service.fetchAndSave(broad)).isEmpty();
      ArgumentCaptor<List<Vacancy>> saved = ArgumentCaptor.forClass(List.class);
      verify(repository).saveAll(eq("b"), saved.capture());
      assertThat(saved.getValue()).isEmpty();
    }
  }
}
