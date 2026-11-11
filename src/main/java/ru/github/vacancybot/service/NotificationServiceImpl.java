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
public class NotificationServiceImpl implements NotificationService {

  private final VacancyService vacancyService;
  private final VkMessageSender messageSender;

  @Override
  public void notifyUser(SearchTask task) {
    log.info("Updating vacancies for the task {} user {}", task.getId(), task.getOwnerId());
    try {
      List<Vacancy> vacancies = vacancyService.fetchAndSave(task);
      if (vacancies.isEmpty()) {
        messageSender.sendText(task.getOwnerId(),
            "🔔 По задаче «" + TaskFormatter.describe(task) + "» новых вакансий не найдено.");
      } else {
        sendVacancies(task, vacancies);
      }
    } catch (RuntimeException e) {
      log.error("Error updating task {}: {}", task.getId(), e.getMessage(), e);
    }
  }

  private void sendVacancies(SearchTask task, List<Vacancy> vacancies) {
    String header = "🔔 Обновление по задаче «" + TaskFormatter.describe(task) + "»\n"
        + "Найдено вакансий: " + vacancies.size() + "\n\n";
    messageSender.sendChunked(task.getOwnerId(), header,
        vacancies.stream().map(VacancyFormatter::detailed).toList());
  }
}
