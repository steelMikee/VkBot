package ru.github.vacancybot.command.impl;

import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.SchedulerService;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;

@RequiredArgsConstructor
public class StopCommand implements BotCommand {

  private static final String STOPPED = "⛔ Все задачи поиска вакансий остановлены. "
      + "Нажмите «Начать», чтобы продолжить работу с ботом.";

  private final VkMessageSender sender;
  private final UserService userService;
  private final SchedulerService schedulerService;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    schedulerService.cancel(userId);
    userService.setDialogState(userId, DialogState.SCREEN_1);
    sender.sendWithKeyboard(userId, STOPPED, KeyboardBuilder.screen1());
  }

  @Override
  public CommandType getType() {
    return CommandType.STOP;
  }
}
