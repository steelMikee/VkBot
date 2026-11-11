package ru.github.vacancybot.command;

import java.util.Optional;
import java.util.regex.Pattern;
import ru.github.vacancybot.dto.ParsedCommand;
import ru.github.vacancybot.util.ButtonLabels;

public class MessageParser {

  public static final String ARG_BACK_TO_TASK = "back_to_task";
  public static final String ARG_ROOT_MENU = "root_menu";

  private static final Pattern NON_DIGITS = Pattern.compile("[^0-9]");
  private static final Pattern FIRST_SPACE = Pattern.compile("\\s+");


  public ParsedCommand parse(String rawText) {
    if (rawText == null || rawText.isBlank()) {
      return new ParsedCommand(CommandType.NONE, null);
    }
    String text = rawText.trim();
    return parseNavigation(text)
        .or(() -> CommandType.fromLabel(text).map(type -> new ParsedCommand(type, null)))
        .orElseGet(() -> parseAlias(text));
  }

  private static Optional<ParsedCommand> parseNavigation(String text) {
    if (text.equalsIgnoreCase(ButtonLabels.BACK_TO_TASK)) {
      return Optional.of(new ParsedCommand(CommandType.MAIN_MENU, ARG_BACK_TO_TASK));
    }
    if (text.equalsIgnoreCase(ButtonLabels.ROOT_MENU)) {
      return Optional.of(new ParsedCommand(CommandType.MAIN_MENU, ARG_ROOT_MENU));
    }
    if (text.equalsIgnoreCase(ButtonLabels.FIRST_PAGE)) {
      return Optional.of(new ParsedCommand(CommandType.NEXT, "1"));
    }
    if (text.startsWith("->") || text.startsWith("<-")) {
      String page = NON_DIGITS.matcher(text).replaceAll("");
      return Optional.of(new ParsedCommand(CommandType.NEXT, page.isEmpty() ? "1" : page));
    }
    return Optional.empty();
  }

  private static ParsedCommand parseAlias(String text) {
    String[] parts = FIRST_SPACE.split(text, 2);
    String argument = parts.length > 1 ? parts[1].trim() : null;
    return CommandType.fromAlias(parts[0])
        .map(type -> new ParsedCommand(type, argument))
        .orElseGet(() -> new ParsedCommand(CommandType.NONE, text));
  }
}
