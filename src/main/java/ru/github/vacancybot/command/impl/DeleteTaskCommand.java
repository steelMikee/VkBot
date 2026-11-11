package ru.github.vacancybot.command.impl;

import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.exception.TaskNotFoundException;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.SchedulerService;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.service.VacancyService;
import ru.github.vacancybot.util.KeyboardBuilder;

@RequiredArgsConstructor
public class DeleteTaskCommand implements BotCommand {

  private static final String NO_TASKS = "📭 У вас нет ни одной заявки на поиск вакансий.";

  private final VkMessageSender sender;
  private final UserService userService;
  private final TaskService taskService;
  private final VacancyService vacancyService;
  private final SchedulerService schedulerService;
  private final BotScreens screens;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    if (taskService.findAllByOwner(userId).isEmpty()) {
      sender.sendWithKeyboard(userId, NO_TASKS, KeyboardBuilder.screen2());
      return;
    }
    userService.setDialogState(userId, DialogState.AWAIT_TASK_ID_DELETE);
    sender.sendText(userId, "Введите ID задачи для удаления:");
  }

  public void handleTaskId(IncomingMessage message, String taskId) {
    long userId = message.userId();
    try {
      taskService.delete(taskId);
      vacancyService.deleteForTask(taskId);
      schedulerService.cancel(userId);
      screens.mainMenu(userId, "✅ Задача " + taskId + " успешно удалена.");
    } catch (TaskNotFoundException e) {
      sender.sendText(userId, "❌ " + e.getMessage() + "\nПопробуйте ещё раз или нажмите /showMenu.");
    }
  }

  @Override
  public CommandType getType() {
    return CommandType.DELETE_TASK;
  }
}
