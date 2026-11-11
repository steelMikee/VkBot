package ru.github.vacancybot.service.vacancy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.model.Vacancy;

@DisplayName("RawVacancy")
class RawVacancyTest {

  private static final RawVacancy NO_SALARY = new RawVacancy("j", "u", "e", "r", 0, 0, 1);

  @Test
  void hasNoSalary() {
    assertThat(NO_SALARY.hasNoSalary()).isTrue();
    assertThat(new RawVacancy("j", "u", "e", "r", 0, 10, 1).hasNoSalary()).isFalse();
  }

  @Test
  @DisplayName("withSalaryFloor подставляет минимум только при отсутствии зарплаты")
  void withSalaryFloor() {
    assertThat(NO_SALARY.withSalaryFloor(500).salaryMin()).isEqualTo(500);
    RawVacancy paid = new RawVacancy("j", "u", "e", "r", 100, 0, 1);
    assertThat(paid.withSalaryFloor(500)).isSameAs(paid);
  }

  @Test
  void toVacancy() {
    assertThat(new RawVacancy("j", "u", "e", "r", 1, 2, 3).toVacancy())
        .isEqualTo(new Vacancy("j", 1, 2, "u", "e", "r"));
  }
}
