package ru.github.vacancybot.command.impl;

import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.TaskFormatter;

@RequiredArgsConstructor
public class NewTaskCommand implements BotCommand {

  private final UserService userService;
  private final TaskService taskService;
  private final BotScreens screens;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    SearchTask task = taskService.createTask(userId);
    userService.setEditingTaskId(userId, task.getId());
    screens.taskScreen(userId, TaskFormatter.taskInfo(task), task);
  }

  @Override
  public CommandType getType() {
    return CommandType.NEW_TASK;
  }
}
