package ru.github.vacancybot.command;

import java.util.Map;
import ru.github.vacancybot.dto.ParsedCommand;
import ru.github.vacancybot.model.DialogState;

public class DispatchPolicy {

  private static final Map<DialogState, FilterKind> OWN_FILTER = Map.of(
      DialogState.AWAIT_REGION, FilterKind.REGION,
      DialogState.AWAIT_EXPERIENCE, FilterKind.EXPERIENCE,
      DialogState.AWAIT_SALARY, FilterKind.SALARY,
      DialogState.AWAIT_KEYWORD, FilterKind.KEYWORD);

  public boolean requiresDefault(DialogState state, ParsedCommand command, String normalizedText) {
    CommandType type = command.type();
    boolean filterMisuse = FilterKind.detect(normalizedText)
        .map(intent -> violatesFilterContext(state, intent))
        .orElse(false);
    if (filterMisuse) {
      return true;
    }
    if (type == CommandType.NEXT && state != DialogState.AWAIT_REGION) {
      return true;
    }
    if (state.isTaskEditing()
        && (type == CommandType.NEW_TASK || type == CommandType.CURRENT_TASKS)) {
      return true;
    }
    return state.isFilterChoice() && isMenuExitAttempt(command);
  }

  private static boolean violatesFilterContext(DialogState state, FilterKind intent) {
    if (!state.isTaskEditing()) {
      return true;
    }
    FilterKind own = OWN_FILTER.get(state);
    return own != null && own != intent;
  }

  private static boolean isMenuExitAttempt(ParsedCommand command) {
    boolean backToTask = command.type() == CommandType.MAIN_MENU
        && MessageParser.ARG_BACK_TO_TASK.equals(command.argument());
    return !backToTask
        && (command.type() == CommandType.DONE || command.type() == CommandType.MAIN_MENU);
  }
}
