package ru.github.vacancybot.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.model.Vacancy;

@DisplayName("VacancyFormatter.detailed")
class VacancyFormatterTest {

  @Test
  @DisplayName("вилка: «от min до max» только если max > min")
  void salaryRange() {
    assertThat(VacancyFormatter.detailed(new Vacancy("Dev", 100, 200, "u", "", "")))
        .contains("💰 Зарплата: от 100 до 200 руб.");
  }

  @Test
  @DisplayName("min == max или max == 0 — только «от min»")
  void salaryMinOnly() {
    assertThat(VacancyFormatter.detailed(new Vacancy("Dev", 100, 100, "u", "", "")))
        .contains("💰 Зарплата: от 100 руб.").doesNotContain("до");
    assertThat(VacancyFormatter.detailed(new Vacancy("Dev", 100, 0, "u", "", "")))
        .contains("💰 Зарплата: от 100 руб.");
  }

  @Test
  @DisplayName("нет зарплаты — строка отсутствует")
  void noSalary() {
    assertThat(VacancyFormatter.detailed(new Vacancy("Dev", 0, 0, "u", "", ""))).doesNotContain("💰");
  }

  @Test
  @DisplayName("работодатель и регион выводятся только если непустые; null безопасен")
  void optionalLines() {
    String full = VacancyFormatter.detailed(new Vacancy("Dev", 0, 0, "http://v", "Corp", "Москва"));
    String bare = VacancyFormatter.detailed(new Vacancy("Dev", 0, 0, "http://v", "", ""));

    assertThat(full).isEqualTo("📌 Dev\n🏢 Corp\n📍 Москва\nhttp://v\n\n");
    assertThat(bare).isEqualTo("📌 Dev\nhttp://v\n\n");
  }
}
