package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.message;
import static ru.github.vacancybot.command.impl.CommandTestSupport.newTask;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.exception.TaskNotFoundException;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.SchedulerService;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.service.VacancyService;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteTaskCommand")
class DeleteTaskCommandTest {

  @Mock VkMessageSender sender;
  @Mock UserService userService;
  @Mock TaskService taskService;
  @Mock VacancyService vacancyService;
  @Mock SchedulerService schedulerService;
  @Mock BotScreens screens;
  @InjectMocks DeleteTaskCommand command;

  @Test
  @DisplayName("нет заявок — сообщение, состояние не меняется")
  void noTasks() {
    when(taskService.findAllByOwner(USER)).thenReturn(List.of());

    command.handle(message(CommandType.DELETE_TASK, null));

    verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.startsWith("📭"), anyString());
    verify(userService, never()).setDialogState(anyLong(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("есть заявки — переход в AWAIT_TASK_ID_DELETE")
  void asksForId() {
    when(taskService.findAllByOwner(USER)).thenReturn(List.of(newTask("a")));

    command.handle(message(CommandType.DELETE_TASK, null));

    verify(userService).setDialogState(USER, DialogState.AWAIT_TASK_ID_DELETE);
    verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.contains("ID задачи для удаления"));
  }

  @Test
  @DisplayName("handleTaskId — удаляет заявку, вакансии, отменяет планировщик, показывает меню")
  void deletesEverything() {
    command.handleTaskId(message(CommandType.NONE, null), "a");

    verify(taskService).delete("a");
    verify(vacancyService).deleteForTask("a");
    verify(schedulerService).cancel(USER);
    verify(screens).mainMenu(eq(USER), org.mockito.ArgumentMatchers.contains("успешно удалена"));
  }

  @Test
  @DisplayName("handleTaskId — несуществующий id: ошибка, ничего не удаляется")
  void notFound() {
    doThrow(new TaskNotFoundException("zzz")).when(taskService).delete("zzz");

    command.handleTaskId(message(CommandType.NONE, null), "zzz");

    verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.contains("'zzz' не найдена"));
    verify(vacancyService, never()).deleteForTask(anyString());
    verify(schedulerService, never()).cancel(anyLong());
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.DELETE_TASK);
  }
}
