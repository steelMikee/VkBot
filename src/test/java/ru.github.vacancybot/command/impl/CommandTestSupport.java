package ru.github.vacancybot.command.impl;

import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.dto.ParsedCommand;
import ru.github.vacancybot.model.SearchTask;

final class CommandTestSupport {

  static final long USER = 10L;

  private CommandTestSupport() {}

  static IncomingMessage message(CommandType type, String argument) {
    return new IncomingMessage(USER, "raw", new ParsedCommand(type, argument), null);
  }

  static IncomingMessage text(String rawText) {
    return new IncomingMessage(USER, rawText, new ParsedCommand(CommandType.NONE, rawText), null);
  }

  static SearchTask newTask(String id) {
    return SearchTask.builder().id(id).ownerId(USER).regionCode(0)
        .regionName(SearchTask.ALL_REGIONS).salaryMin(0).salaryMax(SearchTask.NO_SALARY_LIMIT)
        .keyword("").createdAt(1_700_000_000_000L).build();
  }
}
