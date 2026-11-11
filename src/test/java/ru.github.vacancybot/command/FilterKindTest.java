package ru.github.vacancybot.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("FilterKind.detect")
class FilterKindTest {

  @ParameterizedTest(name = "«{0}» → {1}")
  @CsvSource({
      "регион: 77, REGION",
      "/region 5, REGION",
      "минимальный опыт, EXPERIENCE",
      "/minexp 2, EXPERIENCE",
      "минимальная зарплата, SALARY",
      "/minzp 100, SALARY",
      "наименование вакансии, KEYWORD",
      "/keyword java, KEYWORD"})
  void detects(String text, FilterKind expected) {
    assertThat(FilterKind.detect(text)).contains(expected);
  }

  @Test
  @DisplayName("не фильтр — пусто")
  void nonFilterIsEmpty() {
    assertThat(FilterKind.detect("готово")).isEmpty();
    assertThat(FilterKind.detect("мин. опыт")).isEmpty();
  }
}
