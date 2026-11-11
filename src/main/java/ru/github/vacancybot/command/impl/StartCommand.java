package ru.github.vacancybot.command.impl;

import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.dto.IncomingMessage;

@RequiredArgsConstructor
public class StartCommand implements BotCommand {

  private static final String GREETING =
      "👋 Привет! Я бот по подбору вакансий.\n📋 Главное меню. Выберите действие:";

  private final BotScreens screens;

  @Override
  public void handle(IncomingMessage message) {
    screens.mainMenu(message.userId(), GREETING);
  }

  @Override
  public CommandType getType() {
    return CommandType.DEFAULT;
  }
}
