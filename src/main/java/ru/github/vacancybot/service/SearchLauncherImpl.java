package ru.github.vacancybot.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;
import ru.github.vacancybot.util.TaskFormatter;
import ru.github.vacancybot.util.VacancyFormatter;

@Slf4j
@RequiredArgsConstructor
public class SearchLauncherImpl implements SearchLauncher {

  private final VacancyService vacancyService;
  private final SchedulerService schedulerService;
  private final NotificationService notificationService;
  private final VkMessageSender sender;
  private final long notificationPeriodHours;
  private final long initialDelayMinutes;

  @Override
  public void launch(SearchTask task) {
    Thread.ofVirtual().name("vacancy-search-" + task.getId()).start(() -> runSearch(task));
  }

  @Override
  public void scheduleUpdates(SearchTask task) {
    schedulerService.schedule(task.getOwnerId(),
        () -> notificationService.notifyUser(task),
        notificationPeriodHours, initialDelayMinutes);
  }

  private void runSearch(SearchTask task) {
    long userId = task.getOwnerId();
    try {
      sendResult(userId, vacancyService.fetchAndSave(task), task);
    } catch (RuntimeException e) {
      log.error("Background search failed for task {}", task.getId(), e);
      sender.sendText(userId, "❌ Фоновая ошибка при поиске вакансий: " + e.getMessage());
    }
    schedulerService.cancel(userId);
    scheduleUpdates(task);
  }

  private void sendResult(long userId, List<Vacancy> vacancies, SearchTask task) {
    String description = TaskFormatter.describe(task);
    if (vacancies.isEmpty()) {
      sender.sendText(userId, "🔍 По выбранным критериям (« " + description
          + "») вакансий в регионе не найдено.");
      return;
    }
    sender.sendText(userId, "🎉 Найдено вакансий: " + vacancies.size()
        + "\nПо запросу: " + description);
    sender.sendChunked(userId, "", vacancies.stream().map(VacancyFormatter::detailed).toList());
  }
}
