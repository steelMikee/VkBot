package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.message;
import static ru.github.vacancybot.command.impl.CommandTestSupport.newTask;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.EditingRef;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.SearchLauncher;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoneCommand")
class DoneCommandTest {

  @Mock VkMessageSender sender;
  @Mock UserService userService;
  @Mock TaskService taskService;
  @Mock DraftEditor draftEditor;
  @Mock SearchLauncher launcher;

  private DoneCommand command;
  private final IncomingMessage done = message(CommandType.DONE, null);

  @BeforeEach
  void setUp() {
    command = new DoneCommand(sender, userService, taskService, draftEditor, launcher, 12L);
  }

  @Test
  @DisplayName("нет активной задачи — ошибка, поиск не запускается")
  void noActiveTask() {
    when(draftEditor.editingRef(USER)).thenReturn(Optional.empty());

    command.handle(done);

    verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.contains("Нет активной задачи"), eq(KeyboardBuilder.screen2()));
    verify(launcher, never()).launch(any());
  }

  @Test
  @DisplayName("черновик не найден — ошибка")
  void draftMissing() {
    when(draftEditor.editingRef(USER)).thenReturn(Optional.of(new EditingRef("d", null)));
    when(taskService.findById("d")).thenReturn(Optional.empty());

    command.handle(done);

    verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.contains("Задача не найдена"), anyString());
    verify(launcher, never()).launch(any());
  }

  @Test
  @DisplayName("новая заявка — запускается поиск по черновику, состояние сбрасывается")
  void newTaskLaunchesDraft() {
    SearchTask draft = newTask("d");
    when(draftEditor.editingRef(USER)).thenReturn(Optional.of(new EditingRef("d", null)));
    when(taskService.findById("d")).thenReturn(Optional.of(draft));

    command.handle(done);

    verify(userService).setDialogState(USER, DialogState.SCREEN_2);
    verify(userService).setEditingTaskId(USER, null);
    verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.contains("каждые 12 ч"), eq(KeyboardBuilder.screen2()));
    verify(launcher).launch(draft);
    verify(draftEditor, never()).deleteQuietly(anyString());
  }

  @Test
  @DisplayName("обновление — фильтры черновика переносятся в оригинал, черновик удаляется")
  void updateMergesIntoOriginal() {
    SearchTask draft = newTask("d");
    draft.setRegion(78, "Санкт-Петербург");
    draft.setKeyword("qa");
    SearchTask original = newTask("o");
    when(draftEditor.editingRef(USER)).thenReturn(Optional.of(new EditingRef("d", "o")));
    when(taskService.findById("d")).thenReturn(Optional.of(draft));
    when(taskService.findById("o")).thenReturn(Optional.of(original));

    command.handle(done);

    assertThat(original.getRegionCode()).isEqualTo(78);
    assertThat(original.getKeyword()).isEqualTo("qa");
    assertThat(original.getId()).isEqualTo("o");
    verify(taskService).save(original);
    verify(draftEditor).deleteQuietly("d");
    verify(launcher).launch(original);
  }

  @Test
  @DisplayName("обновление, но оригинал исчез — поиск по черновику")
  void updateWithMissingOriginalFallsBackToDraft() {
    SearchTask draft = newTask("d");
    when(draftEditor.editingRef(USER)).thenReturn(Optional.of(new EditingRef("d", "o")));
    when(taskService.findById("d")).thenReturn(Optional.of(draft));
    when(taskService.findById("o")).thenReturn(Optional.empty());

    command.handle(done);

    verify(launcher).launch(draft);
    verify(taskService, never()).save(any());
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.DONE);
  }
}
