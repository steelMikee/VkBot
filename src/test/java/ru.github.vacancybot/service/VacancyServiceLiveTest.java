package ru.github.vacancybot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.github.vacancybot.client.TrudvsemApiClient;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;
import ru.github.vacancybot.repository.JsonVacancyRepository;
import ru.github.vacancybot.service.vacancy.VacancyParser;

@Tag("live-network")
@DisplayName("VacancyService — реальное API «Работа России»")
class VacancyServiceLiveTest {

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("Москва, «водитель»: результат непустой, все записи соответствуют фильтрам, файл записан")
  void moscowDriverSearch() {
    VacancyServiceImpl service = new VacancyServiceImpl(new TrudvsemApiClient(),
        new VacancyParser(), new JsonVacancyRepository(tempDir.toString()));
    SearchTask task = SearchTask.builder().id("live").regionCode(77).regionName("Москва")
        .keyword("водитель").salaryMax(-1).build();

    List<Vacancy> result = service.fetchAndSave(task);

    assertThat(result).isNotEmpty();
    assertThat(result).allSatisfy(v -> {
      assertThat(v.jobName().toLowerCase()).contains("водитель");
      assertThat(v.region().toLowerCase()).contains("москва");
      assertThat(v.vacUrl()).startsWith("http");
    });
    assertThat(Files.exists(tempDir.resolve("vacancies/live.json"))).isTrue();
  }
}
