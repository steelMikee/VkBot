package ru.github.vacancybot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.github.vacancybot.model.Vacancy;

@Tag("integration")
@DisplayName("JsonVacancyRepository — файл на заявку")
class JsonVacancyRepositoryTest {

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("saveAll пишет vacancies/<taskId>.json, delete удаляет")
  void saveAndDelete() throws Exception {
    JsonVacancyRepository repo = new JsonVacancyRepository(tempDir.toString());
    List<Vacancy> list = List.of(new Vacancy("Dev", 1, 2, "u", "e", "r"));

    repo.saveAll("t1", list);

    Path file = tempDir.resolve("vacancies/t1.json");
    assertThat(Files.exists(file)).isTrue();
    List<Vacancy> read = new ObjectMapper().readValue(file.toFile(), new TypeReference<>() {});
    assertThat(read).isEqualTo(list);

    repo.delete("t1");
    assertThat(Files.exists(file)).isFalse();
    repo.delete("t1");
  }
}
