package ru.github.vacancybot.command;

import java.util.List;
import ru.github.vacancybot.command.impl.DeleteTaskCommand;
import ru.github.vacancybot.command.impl.UpdateTaskCommand;

public record TaskIdCommands(DeleteTaskCommand delete, UpdateTaskCommand update) {

  public List<BotCommand> all() {
    return List.of(delete, update);
  }
}
