package ru.github.vacancybot.command;

import ru.github.vacancybot.dto.IncomingMessage;

public interface BotCommand {

  void handle(IncomingMessage message);

  CommandType getType();
}
