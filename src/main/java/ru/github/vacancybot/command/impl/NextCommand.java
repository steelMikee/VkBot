package ru.github.vacancybot.command.impl;

import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.dto.IncomingMessage;

@RequiredArgsConstructor
public class NextCommand implements BotCommand {

  private final BotScreens screens;

  @Override
  public void handle(IncomingMessage message) {
    screens.regionPage(message.userId(), parsePage(message.parsedCommand().argument()));
  }

  private static int parsePage(String arg) {
    if (arg == null) {
      return 1;
    }
    try {
      return Integer.parseInt(arg);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  @Override
  public CommandType getType() {
    return CommandType.NEXT;
  }
}
