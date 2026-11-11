package ru.github.vacancybot.repository;

import java.util.List;
import ru.github.vacancybot.model.Vacancy;

public interface VacancyRepository {

  void saveAll(String taskId, List<Vacancy> vacancies);

  void delete(String taskId);
}
