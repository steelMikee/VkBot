package ru.github.vacancybot.command.impl;

import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.MessageParser;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.util.TaskFormatter;

@RequiredArgsConstructor
public class ShowMenuCommand implements BotCommand {

  private static final String MENU_TEXT = "📋 Главное меню. Выберите действие:";

  private final DraftEditor draftEditor;
  private final BotScreens screens;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    String argument = message.parsedCommand().argument();
    if (MessageParser.ARG_ROOT_MENU.equals(argument)) {
      draftEditor.discardDraft(userId);
      screens.mainMenu(userId, MENU_TEXT);
    } else if (MessageParser.ARG_BACK_TO_TASK.equals(argument)) {
      draftEditor.findDraft(userId).ifPresentOrElse(
          task -> screens.taskScreen(userId,
              "✏️ Продолжение заполнения заявки:\n\n" + TaskFormatter.taskInfo(task), task),
          () -> screens.mainMenu(userId, MENU_TEXT));
    } else {
      screens.mainMenu(userId, MENU_TEXT);
    }
  }

  @Override
  public CommandType getType() {
    return CommandType.MAIN_MENU;
  }
}
