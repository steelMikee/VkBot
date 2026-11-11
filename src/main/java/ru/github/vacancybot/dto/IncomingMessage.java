package ru.github.vacancybot.dto;

import java.util.Locale;
import ru.github.vacancybot.command.CommandType;

public record IncomingMessage(long userId, String rawText, ParsedCommand parsedCommand,
                              String payload) {

  public IncomingMessage {
    rawText = rawText == null ? "" : rawText;
  }

  public String trimmedText() {
    return rawText.trim();
  }

  public String normalizedText() {
    return rawText.trim().toLowerCase(Locale.ROOT);
  }

  public IncomingMessage withCommand(CommandType type, String argument) {
    return new IncomingMessage(userId, rawText, new ParsedCommand(type, argument), payload);
  }
}
