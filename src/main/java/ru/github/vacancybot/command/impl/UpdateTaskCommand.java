package ru.github.vacancybot.command.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.exception.TaskNotFoundException;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.EditingRef;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;
import ru.github.vacancybot.util.TaskFormatter;

@RequiredArgsConstructor
public class UpdateTaskCommand implements BotCommand {

  private static final String NO_TASKS = "📭 У вас нет ни одной заявки на поиск вакансий.";

  private final VkMessageSender sender;
  private final UserService userService;
  private final TaskService taskService;
  private final BotScreens screens;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    if (taskService.findAllByOwner(userId).isEmpty()) {
      sender.sendWithKeyboard(userId, NO_TASKS, KeyboardBuilder.screen2());
      return;
    }
    userService.setDialogState(userId, DialogState.AWAIT_TASK_ID_UPDATE);
    sender.sendText(userId, "Введите ID задачи для обновления:");
  }

  public void handleTaskId(IncomingMessage message, String taskId) {
    long userId = message.userId();
    Optional<SearchTask> original = taskService.findById(taskId);
    if (original.isEmpty()) {
      sender.sendText(userId,
          "❌ " + TaskNotFoundException.messageFor(taskId) + "\nПопробуйте ещё раз.");
      return;
    }
    SearchTask draft = taskService.createTask(userId);
    draft.copyFiltersFrom(original.get());
    taskService.save(draft);
    userService.setEditingTaskId(userId, EditingRef.compose(draft.getId(), taskId));
    screens.taskScreen(userId,
        "✏️ Редактирование задачи:\n\n" + TaskFormatter.taskInfo(draft), draft);
  }

  @Override
  public CommandType getType() {
    return CommandType.UPDATE_TASK;
  }
}
