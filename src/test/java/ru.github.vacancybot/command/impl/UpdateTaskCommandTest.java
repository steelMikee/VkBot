package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.message;
import static ru.github.vacancybot.command.impl.CommandTestSupport.newTask;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateTaskCommand")
class UpdateTaskCommandTest {

  @Mock VkMessageSender sender;
  @Mock UserService userService;
  @Mock TaskService taskService;
  @Mock BotScreens screens;
  @InjectMocks UpdateTaskCommand command;

  @Test
  @DisplayName("нет заявок — сообщение")
  void noTasks() {
    when(taskService.findAllByOwner(USER)).thenReturn(List.of());

    command.handle(message(CommandType.UPDATE_TASK, null));

    verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.startsWith("📭"), any());
  }

  @Test
  @DisplayName("есть заявки — AWAIT_TASK_ID_UPDATE")
  void asksForId() {
    when(taskService.findAllByOwner(USER)).thenReturn(List.of(newTask("a")));

    command.handle(message(CommandType.UPDATE_TASK, null));

    verify(userService).setDialogState(USER, DialogState.AWAIT_TASK_ID_UPDATE);
  }

  @Test
  @DisplayName("handleTaskId — создаёт черновик-копию и композитную ссылку draft:original")
  void createsDraftCopy() {
    SearchTask original = newTask("orig");
    original.setRegion(77, "Москва");
    original.setExperienceFrom(2);
    original.setSalaryRange(50_000, 70_000);
    original.setKeyword("java");
    SearchTask draft = newTask("draft");
    when(taskService.findById("orig")).thenReturn(Optional.of(original));
    when(taskService.createTask(USER)).thenReturn(draft);

    command.handleTaskId(message(CommandType.NONE, null), "orig");

    assertThat(draft.getRegionCode()).isEqualTo(77);
    assertThat(draft.getRegionName()).isEqualTo("Москва");
    assertThat(draft.getExperienceFrom()).isEqualTo(2);
    assertThat(draft.getSalaryMin()).isEqualTo(50_000);
    assertThat(draft.getSalaryMax()).isEqualTo(70_000);
    assertThat(draft.getKeyword()).isEqualTo("java");
    assertThat(draft.getId()).isEqualTo("draft");
    verify(taskService).save(draft);
    verify(userService).setEditingTaskId(USER, "draft:orig");
    verify(screens).taskScreen(eq(USER), org.mockito.ArgumentMatchers.startsWith("✏️ Редактирование"), eq(draft));
  }

  @Test
  @DisplayName("handleTaskId — несуществующий id: ошибка, черновик не создаётся")
  void notFound() {
    when(taskService.findById("nope")).thenReturn(Optional.empty());

    command.handleTaskId(message(CommandType.NONE, null), "nope");

    verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.contains("'nope' не найдена"));
    verify(taskService, never()).createTask(USER);
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.UPDATE_TASK);
  }
}
