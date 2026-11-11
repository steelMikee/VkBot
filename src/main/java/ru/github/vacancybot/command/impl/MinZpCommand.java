package ru.github.vacancybot.command.impl;

import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;
import ru.github.vacancybot.util.TaskFormatter;

@RequiredArgsConstructor
public class MinZpCommand implements BotCommand {

  private final VkMessageSender sender;
  private final UserService userService;
  private final DraftEditor draftEditor;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    String arg = message.parsedCommand().argument();
    userService.setDialogState(userId, DialogState.AWAIT_SALARY);
    if (arg == null) {
      sender.sendWithKeyboard(userId, "💰 Выберите желаемую зарплатную вилку:",
          KeyboardBuilder.salaryOptions());
      return;
    }
    applyFromArg(userId, arg);
  }

  public void applySalaryRange(long userId, int min, int max) {
    draftEditor.update(userId,
        task -> task.setSalaryRange(min, max),
        task -> "✅ Зарплата: " + TaskFormatter.formatSalary(task));
  }

  private void applyFromArg(long userId, String arg) {
    try {
      applySalaryRange(userId, Integer.parseInt(arg.trim()), SearchTask.NO_SALARY_LIMIT);
    } catch (NumberFormatException e) {
      sender.sendText(userId,
          "❌ Некорректный формат зарплаты. Используйте, например: /minZp 70000");
    }
  }

  @Override
  public CommandType getType() {
    return CommandType.MIN_ZP;
  }
}
