package ru.github.vacancybot.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ButtonLabels — подписи синхронизированы с клавиатурой и парсером")
class ButtonLabelsTest {

  @Test
  @DisplayName("все подписи не длиннее лимита VK в 40 символов")
  void labelsFitVkLimit() {
    assertThat(ButtonLabels.BACK_TO_TASK).hasSizeLessThanOrEqualTo(40);
    assertThat(ButtonLabels.ROOT_MENU).hasSizeLessThanOrEqualTo(40);
    assertThat(ButtonLabels.THREE_YEARS_PLUS).hasSizeLessThanOrEqualTo(40);
    assertThat(ButtonLabels.SALARY_100_PLUS).hasSizeLessThanOrEqualTo(40);
  }

  @Test
  @DisplayName("подписи присутствуют в соответствующих клавиатурах")
  void labelsAppearInKeyboards() {
    assertThat(KeyboardBuilder.experienceOptions()).contains(ButtonLabels.NO_EXPERIENCE,
        ButtonLabels.ONE_YEAR, ButtonLabels.TWO_YEARS, ButtonLabels.THREE_YEARS_PLUS,
        ButtonLabels.ANY_EXPERIENCE, ButtonLabels.BACK_TO_TASK);
    assertThat(KeyboardBuilder.salaryOptions()).contains(ButtonLabels.SALARY_50_70,
        ButtonLabels.SALARY_70_100, ButtonLabels.SALARY_100_PLUS, ButtonLabels.ANY_SALARY);
  }
}
