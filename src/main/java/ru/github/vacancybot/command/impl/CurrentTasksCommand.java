package ru.github.vacancybot.command.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.util.KeyboardBuilder;
import ru.github.vacancybot.util.TaskFormatter;

@RequiredArgsConstructor
public class CurrentTasksCommand implements BotCommand {

  private static final String NO_TASKS = "📭 У вас пока нет ни одной заявки на поиск вакансий.\n"
      + "Нажмите «Создать заявку на поиск вакансий», чтобы создать первую.";
  private static final String HEADER = "📋 Ваши действующие заявки на поиск вакансий:";

  private final VkMessageSender sender;
  private final TaskService taskService;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    List<SearchTask> tasks = taskService.findAllByOwner(userId);
    if (tasks.isEmpty()) {
      sender.sendWithKeyboard(userId, NO_TASKS, KeyboardBuilder.screen2());
      return;
    }
    sender.sendWithKeyboard(userId, HEADER, KeyboardBuilder.screen2());
    tasks.forEach(task -> sender.sendWithKeyboard(userId,
        TaskFormatter.taskCard(task), KeyboardBuilder.taskInlineKeyboard(task.getId())));
  }

  @Override
  public CommandType getType() {
    return CommandType.CURRENT_TASKS;
  }
}
