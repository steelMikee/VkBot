package ru.github.vacancybot.command.impl;

import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.UserService;

@RequiredArgsConstructor
public class KeywordCommand implements BotCommand {

  private final VkMessageSender sender;
  private final UserService userService;
  private final DraftEditor draftEditor;
  private final BotScreens screens;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    String arg = message.parsedCommand().argument();
    if (arg != null && !arg.isBlank()) {
      handleText(message, arg);
      return;
    }
    userService.setDialogState(userId, DialogState.AWAIT_KEYWORD);
    sender.sendText(userId, "🔍 Введите наименование вакансии (например: Java разработчик):");
  }

  public void handleText(IncomingMessage message, String text) {
    long userId = message.userId();
    if (draftEditor.editingRef(userId).isEmpty()) {
      screens.mainMenu(userId, "❌ Нет активной задачи для редактирования.");
      return;
    }
    String keyword = text.trim();
    draftEditor.update(userId,
        task -> task.setKeyword(keyword),
        task -> "✅ Ключевое слово: «" + keyword + "»");
  }

  @Override
  public CommandType getType() {
    return CommandType.KEYWORD;
  }
}
