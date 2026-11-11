package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;
import ru.github.vacancybot.util.TaskFormatter;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultCommand — ответы на нераспознанный ввод по состояниям")
class DefaultCommandTest {

  @Mock VkMessageSender sender;
  @Mock UserService userService;
  @Mock BotScreens screens;
  @Mock KeywordCommand keywordCommand;
  @Mock DeleteTaskCommand deleteTaskCommand;
  @Mock UpdateTaskCommand updateTaskCommand;
  @InjectMocks DefaultCommand command;

  private void state(DialogState s) {
    when(userService.getDialogState(USER)).thenReturn(s);
  }

  @Nested
  class Screen {

    @ParameterizedTest
    @EnumSource(value = DialogState.class, names = {"SCREEN_1", "SCREEN_2"})
    @DisplayName("на экранах 1/2 — «Команда не распознана»")
    void unknownOnScreens(DialogState s) {
      state(s);

      command.handle(text("abracadabra"));

      verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.startsWith("Команда не распознана"));
    }
  }

  @Nested
  class CreatingTask {

    @ParameterizedTest
    @ValueSource(strings = {"Создать заявку на поиск вакансий", "/newTask", "/current"})
    @DisplayName("глобальная команда во время оформления → «уже в режиме оформления»")
    void globalCommandBlocked(String input) {
      state(DialogState.CREATING_TASK);

      command.handle(text(input));

      verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.startsWith("❌ Вы уже находитесь"));
    }

    @Test
    @DisplayName("прочий текст → «используйте кнопки фильтров»")
    void otherText() {
      state(DialogState.CREATING_TASK);

      command.handle(text("hello"));

      verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.contains("Используйте кнопки фильтров"));
    }
  }

  @Nested
  class FilterChoice {

    @Test
    @DisplayName("AWAIT_EXPERIENCE: мусор → повтор клавиатуры опыта")
    void experienceRepeatsKeyboard() {
      state(DialogState.AWAIT_EXPERIENCE);

      command.handle(text("???"));

      verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.startsWith("Неверная команда"), eq(KeyboardBuilder.experienceOptions()));
    }

    @Test
    @DisplayName("AWAIT_SALARY: мусор → повтор клавиатуры зарплат")
    void salaryRepeatsKeyboard() {
      state(DialogState.AWAIT_SALARY);

      command.handle(text("???"));

      verify(sender).sendWithKeyboard(eq(USER), anyString(), eq(KeyboardBuilder.salaryOptions()));
    }

    @Test
    @DisplayName("AWAIT_REGION: мусор → первая страница регионов с ❌ и подсказкой")
    void regionRepeatsFirstPage() {
      state(DialogState.AWAIT_REGION);

      command.handle(text("???"));

      verify(screens).regionPageWithText(USER, 1, "❌ " + TaskFormatter.regionPrompt());
    }

    @Test
    @DisplayName("«Готово» при выборе фильтра → «кнопка здесь не работает… выберите»")
    void doneBlocked() {
      state(DialogState.AWAIT_SALARY);

      command.handle(text("Готово"));

      verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.contains("Сначала выберите значение фильтра"));
    }

    @Test
    @DisplayName("глобальная команда при выборе фильтра → «уже в режиме оформления»")
    void globalBlocked() {
      state(DialogState.AWAIT_REGION);

      command.handle(text("/current"));

      verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.startsWith("❌ Вы уже находитесь"));
    }
  }

  @Nested
  class AwaitKeyword {

    @Test
    @DisplayName("обычный текст передаётся в KeywordCommand.handleText")
    void textGoesToKeyword() {
      state(DialogState.AWAIT_KEYWORD);
      IncomingMessage m = text("  Java dev ");

      command.handle(m);

      verify(keywordCommand).handleText(m, "Java dev");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Регион: Любой", "Мин. опыт: Любой", "Зарплата: Любая", "Вакансия: Любая"})
    @DisplayName("нажатие кнопки фильтра вместо ввода текста → подсказка")
    void menuButtonInsteadOfText(String input) {
      state(DialogState.AWAIT_KEYWORD);

      command.handle(text(input));

      verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.startsWith("При вводе Наименования Вакансии"));
      verify(keywordCommand, never()).handleText(any(), anyString());
    }

    @Test
    @DisplayName("«В главное меню» → «кнопка здесь не работает… введите»")
    void menuActionBlocked() {
      state(DialogState.AWAIT_KEYWORD);

      command.handle(text("В главное меню"));

      verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.contains("Сначала введите значение фильтра"));
    }

    @Test
    @DisplayName("глобальная команда → «уже в режиме оформления»")
    void globalBlocked() {
      state(DialogState.AWAIT_KEYWORD);

      command.handle(text("/newtask"));

      verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.startsWith("❌ Вы уже находитесь"));
    }
  }

  @Nested
  class AwaitTaskId {

    @Test
    @DisplayName("AWAIT_TASK_ID_DELETE → DeleteTaskCommand.handleTaskId")
    void delete() {
      state(DialogState.AWAIT_TASK_ID_DELETE);
      IncomingMessage m = text(" abc ");

      command.handle(m);

      verify(deleteTaskCommand).handleTaskId(m, "abc");
    }

    @Test
    @DisplayName("AWAIT_TASK_ID_UPDATE → UpdateTaskCommand.handleTaskId")
    void update() {
      state(DialogState.AWAIT_TASK_ID_UPDATE);
      IncomingMessage m = text("xyz");

      command.handle(m);

      verify(updateTaskCommand).handleTaskId(m, "xyz");
    }
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.NONE);
  }
}
