package ru.github.vacancybot.command.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandDispatcher;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.DispatchPolicy;
import ru.github.vacancybot.command.FilterCommands;
import ru.github.vacancybot.command.MessageParser;
import ru.github.vacancybot.command.TaskIdCommands;
import ru.github.vacancybot.command.impl.DeleteTaskCommand;
import ru.github.vacancybot.command.impl.KeywordCommand;
import ru.github.vacancybot.command.impl.MinExpCommand;
import ru.github.vacancybot.command.impl.MinZpCommand;
import ru.github.vacancybot.command.impl.RegionCommand;
import ru.github.vacancybot.command.impl.UpdateTaskCommand;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.BotUser;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamicHandlerRegistrar — контекстные обработчики кнопок")
class DynamicHandlerRegistrarTest {

  @Mock UserService userService;
  @Mock TaskService taskService;
  @Mock VkMessageSender sender;
  @Mock BotCommand defaultCommand;
  @Mock RegionCommand regionCmd;
  @Mock MinExpCommand minExpCmd;
  @Mock MinZpCommand minZpCmd;
  @Mock KeywordCommand keywordCmd;
  @Mock DeleteTaskCommand deleteCmd;
  @Mock UpdateTaskCommand updateCmd;

  private CommandDispatcher dispatcher;
  private final MessageParser parser = new MessageParser();

  @BeforeEach
  void setUp() {
    BotScreens screens = new BotScreens(sender, userService);
    DraftEditor editor = new DraftEditor(userService, taskService, screens);
    dispatcher = new CommandDispatcher(defaultCommand, userService, new DispatchPolicy());
    new DynamicHandlerRegistrar(userService, editor, screens,
        new FilterCommands(regionCmd, minExpCmd, minZpCmd, keywordCmd),
        new TaskIdCommands(deleteCmd, updateCmd)).registerAll(dispatcher);
  }

  private void state(DialogState state) {
    when(userService.getDialogState(1L)).thenReturn(state);
  }

  private IncomingMessage msg(String text) {
    return new IncomingMessage(1L, text, parser.parse(text), null);
  }

  private SearchTask draft() {
    SearchTask task = SearchTask.builder().id("d").ownerId(1L).keyword("").salaryMax(-1)
        .regionName(SearchTask.ALL_REGIONS).build();
    when(userService.getOrCreate(1L))
        .thenReturn(BotUser.builder().vkUserId(1L).editingTaskId("d").build());
    when(taskService.findById("d")).thenReturn(Optional.of(task));
    return task;
  }

  @Nested
  @DisplayName("inline payload")
  class InlinePayload {

    @Test
    @DisplayName("delete_task → DeleteTaskCommand.handleTaskId")
    void deletePayload() {
      state(DialogState.SCREEN_2);
      IncomingMessage m = new IncomingMessage(1L, "❌ Удалить", parser.parse("❌ Удалить"),
          "{\"command\":\"delete_task\",\"id\":\"t1\"}");

      dispatcher.dispatch(m);

      verify(deleteCmd).handleTaskId(m, "t1");
    }

    @Test
    @DisplayName("update_task → UpdateTaskCommand.handleTaskId")
    void updatePayload() {
      state(DialogState.SCREEN_2);
      IncomingMessage m = new IncomingMessage(1L, "✏️ Изменить", parser.parse("✏️ Изменить"),
          "{\"command\":\"update_task\",\"id\":\"t2\"}");

      dispatcher.dispatch(m);

      verify(updateCmd).handleTaskId(m, "t2");
    }

    @Test
    @DisplayName("повреждённый payload — без исключения, команды не вызываются")
    void brokenPayload() {
      state(DialogState.SCREEN_2);
      IncomingMessage m = new IncomingMessage(1L, "x", parser.parse("x"), "{delete_task");

      dispatcher.dispatch(m);

      verify(deleteCmd, never()).handleTaskId(any(), anyString());
      verify(updateCmd, never()).handleTaskId(any(), anyString());
    }
  }

  @Nested
  @DisplayName("кнопки экрана 3")
  class Screen3Buttons {

    @Test
    @DisplayName("«Регион: Любой» в CREATING_TASK → RegionCommand c типом REGION")
    void regionButton() {
      state(DialogState.CREATING_TASK);

      dispatcher.dispatch(msg("Регион: Любой"));

      ArgumentCaptor<IncomingMessage> c = ArgumentCaptor.forClass(IncomingMessage.class);
      verify(regionCmd).handle(c.capture());
      assertThat(c.getValue().parsedCommand().type()).isEqualTo(CommandType.REGION);
      assertThat(c.getValue().parsedCommand().argument()).isNull();
    }

    @Test
    @DisplayName("«Мин. опыт: …», «Зарплата: …», «Вакансия: …» → соответствующие команды")
    void otherButtons() {
      state(DialogState.CREATING_TASK);

      dispatcher.dispatch(msg("Мин. опыт: Любой"));
      dispatcher.dispatch(msg("Зарплата: Любая"));
      dispatcher.dispatch(msg("Вакансия: Любая"));

      verify(minExpCmd).handle(any());
      verify(minZpCmd).handle(any());
      verify(keywordCmd).handle(any());
    }

    @Test
    @DisplayName("та же кнопка вне CREATING_TASK не срабатывает как фильтр")
    void buttonOutsideCreatingTask() {
      state(DialogState.SCREEN_2);

      dispatcher.dispatch(msg("Регион: Любой"));

      verify(regionCmd, never()).handle(any());
      verify(defaultCommand).handle(any());
    }
  }

  @Nested
  @DisplayName("выбор опыта и зарплаты")
  class ExperienceAndSalary {

    @Test
    @DisplayName("варианты опыта маппятся в годы, «Любой опыт» → 0")
    void experienceOptions() {
      state(DialogState.AWAIT_EXPERIENCE);

      dispatcher.dispatch(msg("Без опыта"));
      dispatcher.dispatch(msg("1 год"));
      dispatcher.dispatch(msg("2 ГОДА"));
      dispatcher.dispatch(msg("3 года и более"));
      dispatcher.dispatch(msg("Любой опыт"));

      verify(minExpCmd, org.mockito.Mockito.times(2)).applyExp(1L, 0);
      verify(minExpCmd).applyExp(1L, 1);
      verify(minExpCmd).applyExp(1L, 2);
      verify(minExpCmd).applyExp(1L, 3);
    }

    @Test
    @DisplayName("вилки зарплаты")
    void salaryOptions() {
      state(DialogState.AWAIT_SALARY);

      dispatcher.dispatch(msg("50000 - 70000р"));
      dispatcher.dispatch(msg("70000 - 100000р"));
      dispatcher.dispatch(msg("100000р и более"));
      dispatcher.dispatch(msg("Любая зарплата"));

      verify(minZpCmd).applySalaryRange(1L, 50_000, 70_000);
      verify(minZpCmd).applySalaryRange(1L, 70_000, 100_000);
      verify(minZpCmd).applySalaryRange(1L, 100_000, -1);
      verify(minZpCmd).applySalaryRange(1L, 0, -1);
    }

    @Test
    @DisplayName("вариант опыта в чужом состоянии не применяется")
    void experienceInWrongState() {
      state(DialogState.AWAIT_SALARY);

      dispatcher.dispatch(msg("1 год"));

      verify(minExpCmd, never()).applyExp(anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }
  }

  @Nested
  @DisplayName("регион")
  class Region {

    @Test
    @DisplayName("«Любой регион» сбрасывает регион заявки")
    void anyRegion() {
      state(DialogState.AWAIT_REGION);
      SearchTask task = draft();

      dispatcher.dispatch(msg("Любой регион"));

      assertThat(task.getRegionCode()).isZero();
      assertThat(task.getRegionName()).isEqualTo(SearchTask.ALL_REGIONS);
      verify(taskService).save(task);
      verify(sender).sendWithKeyboard(eq(1L), org.mockito.ArgumentMatchers.startsWith("✅ Выбраны все регионы"), anyString());
    }

    @Test
    @DisplayName("ввод «77 Москва» устанавливает регион")
    void freeTextRegion() {
      state(DialogState.AWAIT_REGION);
      SearchTask task = draft();

      dispatcher.dispatch(msg("77 Москва"));

      assertThat(task.getRegionCode()).isEqualTo(77);
      assertThat(task.getRegionName()).isEqualTo("Москва");
      verify(sender).sendWithKeyboard(eq(1L), org.mockito.ArgumentMatchers.startsWith("✅ Регион выбран: 77 Москва"), anyString());
    }

    @Test
    @DisplayName("неизвестный регион — подсказка с первой страницей")
    void unknownRegion() {
      state(DialogState.AWAIT_REGION);

      dispatcher.dispatch(msg("Атлантида"));

      verify(taskService, never()).save(any());
      verify(sender).sendWithKeyboard(eq(1L), org.mockito.ArgumentMatchers.contains("несуществующий регион"), anyString());
    }

    @Test
    @DisplayName("переполнение int в коде региона — безопасно «не найден»")
    void hugeNumberIsSafe() {
      state(DialogState.AWAIT_REGION);

      dispatcher.dispatch(msg("99999999999"));

      verify(sender).sendWithKeyboard(eq(1L), org.mockito.ArgumentMatchers.contains("несуществующий регион"), anyString());
    }

    @Test
    @DisplayName("навигационные кнопки не считаются вводом региона")
    void navigationIsNotFreeText() {
      state(DialogState.AWAIT_REGION);

      dispatcher.dispatch(msg("-> [2]"));
      dispatcher.dispatch(msg("/region"));

      verify(sender, never()).sendWithKeyboard(anyLong(), org.mockito.ArgumentMatchers.contains("несуществующий"), anyString());
    }
  }
}
