package ru.github.vacancybot.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import ru.github.vacancybot.dto.ParsedCommand;
import ru.github.vacancybot.model.DialogState;

@DisplayName("DispatchPolicy — правила перенаправления в DefaultCommand")
class DispatchPolicyTest {

  private final DispatchPolicy policy = new DispatchPolicy();

  private boolean requiresDefault(DialogState state, CommandType type, String arg, String text) {
    return policy.requiresDefault(state, new ParsedCommand(type, arg), text);
  }

  @Nested
  @DisplayName("попытка открыть меню фильтра")
  class FilterIntent {

    @ParameterizedTest
    @EnumSource(value = DialogState.class, names = {"SCREEN_1", "SCREEN_2"})
    @DisplayName("вне оформления заявки любой фильтр → default")
    void filtersBlockedOutsideEditing(DialogState state) {
      assertThat(requiresDefault(state, CommandType.REGION, null, "регион")).isTrue();
      assertThat(requiresDefault(state, CommandType.KEYWORD, null, "/keyword x")).isTrue();
    }

    @Test
    @DisplayName("в CREATING_TASK любой фильтр разрешён")
    void allFiltersAllowedInCreatingTask() {
      assertThat(requiresDefault(DialogState.CREATING_TASK, CommandType.REGION, null, "регион"))
          .isFalse();
      assertThat(requiresDefault(DialogState.CREATING_TASK, CommandType.MIN_ZP, null, "/minzp 1"))
          .isFalse();
    }

    @ParameterizedTest(name = "{0}: свой «{1}» ok, чужой «{2}» → default")
    @CsvSource({
        "AWAIT_REGION, регион, минимальный опыт",
        "AWAIT_EXPERIENCE, /minexp, /minzp",
        "AWAIT_SALARY, минимальная зарплата, наименование вакансии",
        "AWAIT_KEYWORD, /keyword, регион"})
    @DisplayName("в AWAIT_* разрешён только свой фильтр")
    void onlyOwnFilterInAwaitState(DialogState state, String own, String foreign) {
      assertThat(requiresDefault(state, CommandType.NONE, null, own)).isFalse();
      assertThat(requiresDefault(state, CommandType.NONE, null, foreign)).isTrue();
    }
  }

  @Nested
  @DisplayName("прочие правила")
  class OtherRules {

    @Test
    @DisplayName("NEXT допустим только в AWAIT_REGION")
    void nextOnlyInAwaitRegion() {
      assertThat(requiresDefault(DialogState.AWAIT_REGION, CommandType.NEXT, "2", "-> [2]"))
          .isFalse();
      assertThat(requiresDefault(DialogState.CREATING_TASK, CommandType.NEXT, "2", "-> [2]"))
          .isTrue();
      assertThat(requiresDefault(DialogState.SCREEN_2, CommandType.NEXT, "1", "сначала")).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = DialogState.class, names = {"CREATING_TASK", "AWAIT_REGION",
        "AWAIT_KEYWORD", "AWAIT_TASK_ID_DELETE"})
    @DisplayName("NEW_TASK / CURRENT_TASKS заблокированы во время оформления заявки")
    void globalCommandsBlockedWhileEditing(DialogState state) {
      assertThat(requiresDefault(state, CommandType.NEW_TASK, null, "/newtask")).isTrue();
      assertThat(requiresDefault(state, CommandType.CURRENT_TASKS, null, "/current")).isTrue();
    }

    @Test
    @DisplayName("NEW_TASK разрешён на главном экране")
    void newTaskAllowedOnScreen2() {
      assertThat(requiresDefault(DialogState.SCREEN_2, CommandType.NEW_TASK, null, "/newtask"))
          .isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = DialogState.class, names = {"AWAIT_REGION", "AWAIT_EXPERIENCE",
        "AWAIT_SALARY"})
    @DisplayName("при выборе фильтра DONE и «В главное меню» → default, «Назад к заявке» — ок")
    void menuExitBlockedDuringFilterChoice(DialogState state) {
      assertThat(requiresDefault(state, CommandType.DONE, null, "готово")).isTrue();
      assertThat(requiresDefault(state, CommandType.MAIN_MENU, MessageParser.ARG_ROOT_MENU,
          "в главное меню")).isTrue();
      assertThat(requiresDefault(state, CommandType.MAIN_MENU, MessageParser.ARG_BACK_TO_TASK,
          "назад к заявке")).isFalse();
    }

    @Test
    @DisplayName("в AWAIT_KEYWORD «Готово» не блокируется политикой (решает DefaultCommand)")
    void doneNotBlockedInAwaitKeyword() {
      assertThat(requiresDefault(DialogState.AWAIT_KEYWORD, CommandType.DONE, null, "готово"))
          .isFalse();
    }

    @Test
    @DisplayName("обычные команды на главном экране проходят")
    void ordinaryCommandsPass() {
      assertThat(requiresDefault(DialogState.SCREEN_2, CommandType.DELETE_TASK, null, "/delete"))
          .isFalse();
      assertThat(requiresDefault(DialogState.SCREEN_1, CommandType.DEFAULT, null, "/start"))
          .isFalse();
    }
  }
}
