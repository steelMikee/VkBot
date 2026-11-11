package ru.github.vacancybot.service;

import java.util.List;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;

public interface VacancyService {

  List<Vacancy> fetchAndSave(SearchTask task);

  void deleteForTask(String taskId);
}
