package ru.github.vacancybot.service.vacancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;

@DisplayName("VacancySorter — компаратор по заявке")
class VacancySorterTest {

  private static Vacancy v(String name, int min, int max) {
    return new Vacancy(name, min, max, "u", "e", "r");
  }

  private static List<String> sorted(SearchTask task, Vacancy... items) {
    return Stream.of(items).sorted(VacancySorter.forTask(task)).map(Vacancy::jobName).toList();
  }

  @Test
  @DisplayName("без требований к зарплате — по salaryMin по убыванию")
  void noSalaryRequirement() {
    SearchTask task = SearchTask.builder().salaryMin(0).salaryMax(-1).build();

    assertThat(sorted(task, v("a", 100, 0), v("b", 300, 0), v("c", 200, 500)))
        .containsExactly("b", "c", "a");
  }

  @Test
  @DisplayName("только минимум — без зарплаты в конец, остальные по эффективной зарплате (min или max)")
  void minOnly() {
    SearchTask task = SearchTask.builder().salaryMin(50).salaryMax(-1).build();

    assertThat(sorted(task, v("none", 0, 0), v("maxOnly", 0, 400), v("min", 300, 0)))
        .containsExactly("maxOnly", "min", "none");
  }

  @Test
  @DisplayName("вилка — ближе к центру вилки выше; при равенстве — больший salaryMin")
  void range() {
    SearchTask task = SearchTask.builder().salaryMin(100).salaryMax(200).build();

    assertThat(sorted(task, v("far", 250, 0), v("center", 150, 0), v("none", 0, 0),
        v("near1", 140, 0), v("near2", 160, 0)))
        .containsExactly("center", "near2", "near1", "far", "none");
  }
}
