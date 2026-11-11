package ru.github.vacancybot.command.impl;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;
import ru.github.vacancybot.util.TaskFormatter;

@RequiredArgsConstructor
public class DefaultCommand implements BotCommand {

  private static final Set<String> GLOBAL_COMMANDS = Set.of(
      "создать заявку на поиск вакансий", "/newtask", "текущие заявки на поиск", "/current");
  private static final Set<String> MENU_ACTIONS = Set.of("готово", "в главное меню");
  private static final List<String> FILTER_BUTTON_PREFIXES =
      List.of("Регион", "Мин. опыт", "Зарплата", "Вакансия");

  private static final String ALREADY_EDITING = "❌ Вы уже находитесь в режиме оформления заявки."
      + " Завершите её («Готово») или вернитесь («В главное меню»).";
  private static final String WRONG_COMMAND_IN_TASK =
      "Неверная команда. Используйте кнопки фильтров или завершите оформление заявки.";
  private static final String WRONG_COMMAND_USE_MENU =
      "Неверная команда. Выберите необходимую из кнопок меню ниже.";
  private static final String BUTTON_UNAVAILABLE_CHOOSE_FILTER = "❌ Эта кнопка здесь не работает."
      + " Сначала выберите значение фильтра или нажмите «Назад к заявке».";
  private static final String BUTTON_UNAVAILABLE_ENTER_FILTER = "❌ Эта кнопка здесь не работает."
      + " Сначала введите значение фильтра или нажмите «Назад к заявке».";
  private static final String MENU_BUTTON_INSTEAD_OF_KEYWORD =
      "При вводе Наименования Вакансии, Вы нажали одну из кнопок меню."
          + " Введите вручную текст Наименования Вакансии";
  private static final String UNKNOWN_COMMAND =
      "Команда не распознана. Нажмите «Начать» или введите /start.";

  private final VkMessageSender sender;
  private final UserService userService;
  private final BotScreens screens;
  private final KeywordCommand keywordCommand;
  private final DeleteTaskCommand deleteTaskCommand;
  private final UpdateTaskCommand updateTaskCommand;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    DialogState state = userService.getDialogState(userId);
    String text = message.trimmedText();
    switch (state) {
      case CREATING_TASK -> handleCreatingTask(userId, message.normalizedText());
      case AWAIT_REGION, AWAIT_EXPERIENCE, AWAIT_SALARY ->
          handleFilterChoice(userId, state, message.normalizedText());
      case AWAIT_KEYWORD -> handleKeywordInput(message);
      case AWAIT_TASK_ID_DELETE -> deleteTaskCommand.handleTaskId(message, text);
      case AWAIT_TASK_ID_UPDATE -> updateTaskCommand.handleTaskId(message, text);
      default -> sender.sendText(userId, UNKNOWN_COMMAND);
    }
  }

  private void handleCreatingTask(long userId, String lower) {
    sender.sendText(userId,
        GLOBAL_COMMANDS.contains(lower) ? ALREADY_EDITING : WRONG_COMMAND_IN_TASK);
  }

  private void handleFilterChoice(long userId, DialogState state, String lower) {
    if (GLOBAL_COMMANDS.contains(lower)) {
      sender.sendText(userId, ALREADY_EDITING);
      return;
    }
    if (MENU_ACTIONS.contains(lower)) {
      sender.sendText(userId, BUTTON_UNAVAILABLE_CHOOSE_FILTER);
      return;
    }
    sendWrongCommandMessage(userId, state);
  }

  private void handleKeywordInput(IncomingMessage message) {
    long userId = message.userId();
    String lower = message.normalizedText();
    if (GLOBAL_COMMANDS.contains(lower)) {
      sender.sendText(userId, ALREADY_EDITING);
      return;
    }
    if (MENU_ACTIONS.contains(lower)) {
      sender.sendText(userId, BUTTON_UNAVAILABLE_ENTER_FILTER);
      return;
    }
    String text = message.trimmedText();
    if (FILTER_BUTTON_PREFIXES.stream().anyMatch(text::startsWith)) {
      sender.sendText(userId, MENU_BUTTON_INSTEAD_OF_KEYWORD);
      return;
    }
    keywordCommand.handleText(message, text);
  }

  private void sendWrongCommandMessage(long userId, DialogState state) {
    switch (state) {
      case AWAIT_EXPERIENCE -> sender.sendWithKeyboard(userId, WRONG_COMMAND_USE_MENU,
          KeyboardBuilder.experienceOptions());
      case AWAIT_SALARY -> sender.sendWithKeyboard(userId, WRONG_COMMAND_USE_MENU,
          KeyboardBuilder.salaryOptions());
      case AWAIT_REGION ->
          screens.regionPageWithText(userId, 1, "❌ " + TaskFormatter.regionPrompt());
      default -> sender.sendText(userId, WRONG_COMMAND_USE_MENU);
    }
  }

  @Override
  public CommandType getType() {
    return CommandType.NONE;
  }
}
