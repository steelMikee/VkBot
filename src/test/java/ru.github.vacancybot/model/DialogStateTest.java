package ru.github.vacancybot.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("DialogState")
class DialogStateTest {

  @ParameterizedTest
  @EnumSource(value = DialogState.class, names = {"SCREEN_1", "SCREEN_2"})
  @DisplayName("экраны 1/2 — не оформление заявки")
  void screensAreNotEditing(DialogState s) {
    assertThat(s.isTaskEditing()).isFalse();
    assertThat(s.isFilterChoice()).isFalse();
  }

  @ParameterizedTest
  @EnumSource(value = DialogState.class, names = {"SCREEN_1", "SCREEN_2"}, mode = EnumSource.Mode.EXCLUDE)
  @DisplayName("все остальные — оформление заявки")
  void othersAreEditing(DialogState s) {
    assertThat(s.isTaskEditing()).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = DialogState.class, names = {"AWAIT_REGION", "AWAIT_EXPERIENCE", "AWAIT_SALARY"})
  @DisplayName("выбор фильтра — только регион/опыт/зарплата")
  void filterChoice(DialogState s) {
    assertThat(s.isFilterChoice()).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = DialogState.class, names = {"AWAIT_KEYWORD", "CREATING_TASK", "AWAIT_TASK_ID_DELETE"})
  void notFilterChoice(DialogState s) {
    assertThat(s.isFilterChoice()).isFalse();
  }
}
