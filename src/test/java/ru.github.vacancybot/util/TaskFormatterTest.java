package ru.github.vacancybot.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.github.vacancybot.model.SearchTask;

@DisplayName("TaskFormatter")
class TaskFormatterTest {

  private static SearchTask.SearchTaskBuilder task() {
    return SearchTask.builder().id("id-1").keyword("").regionName(SearchTask.ALL_REGIONS)
        .salaryMax(-1).createdAt(0);
  }

  @Test
  @DisplayName("taskInfo — пустая заявка")
  void taskInfoEmpty() {
    assertThat(TaskFormatter.taskInfo(task().build()))
        .contains("не выбран (все регионы)", "Без опыта", "не задана", "не задано");
  }

  @Test
  @DisplayName("taskInfo — заполненная заявка")
  void taskInfoFilled() {
    SearchTask t = task().regionCode(77).regionName("Москва").experienceFrom(1)
        .salaryMin(50).salaryMax(70).keyword("java").build();

    assertThat(TaskFormatter.taskInfo(t))
        .contains("[77] Москва", "от 1 года", "50 – 70 руб.", "Ключевое слово: java");
  }

  @Test
  @DisplayName("taskCard — id, дата, регион, «Любое» для пустого/«Любая» ключевого слова")
  void taskCard() {
    SearchTask t = task().keyword("Любая").build();

    assertThat(TaskFormatter.taskCard(t))
        .contains("ID заявки: id-1", "Создана: ", "Регион: Все регионы", "Ключевое слово: Любое");
    assertThat(TaskFormatter.taskCard(task().regionCode(78).regionName("Санкт-Петербург").build()))
        .contains("Регион: 78 Санкт-Петербург");
  }

  @ParameterizedTest
  @CsvSource({"0, Без опыта", "1, от 1 года", "2, от 2 лет", "3, от 3 лет и более", "7, от 3 лет и более"})
  void formatExp(int exp, String expected) {
    assertThat(TaskFormatter.formatExp(exp)).isEqualTo(expected);
  }

  @Test
  void formatSalary() {
    assertThat(TaskFormatter.formatSalary(task().build())).isEqualTo("не задана");
    assertThat(TaskFormatter.formatSalary(task().salaryMin(100).build())).isEqualTo("от 100 руб.");
    assertThat(TaskFormatter.formatSalary(task().salaryMin(100).salaryMax(200).build()))
        .isEqualTo("100 – 200 руб.");
  }

  @Test
  void describe() {
    assertThat(TaskFormatter.describe(task().build())).isEqualTo("все вакансии, Все регионы");
    assertThat(TaskFormatter.describe(task().keyword("qa").regionName("Москва").build()))
        .isEqualTo("qa, Москва");
  }

  @Test
  @DisplayName("regionPrompt — полная подсказка с примерами; regionPageHeader — короткий заголовок")
  void regionTexts() {
    assertThat(TaskFormatter.regionPrompt()).contains("Примеры ввода", "77 Москва");
    assertThat(TaskFormatter.regionPageHeader(4)).contains("страница 4").doesNotContain("Примеры");
  }
}
