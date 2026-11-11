package ru.github.vacancybot.repository;

import java.nio.file.Path;
import java.util.List;
import ru.github.vacancybot.model.Vacancy;

public class JsonVacancyRepository extends AbstractJsonRepository implements VacancyRepository {

  private static final String SUBDIR = "vacancies";

  public JsonVacancyRepository(String dataDir) {
    super(Path.of(dataDir, SUBDIR));
  }

  @Override
  public void saveAll(String taskId, List<Vacancy> vacancies) {
    write(fileFor(taskId), vacancies);
  }

  @Override
  public void delete(String taskId) {
    deleteIfExists(fileFor(taskId));
  }

  private Path fileFor(String taskId) {
    return dataDir.resolve(taskId + ".json");
  }
}
