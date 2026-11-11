package ru.github.vacancybot.command.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.exception.TaskNotFoundException;
import ru.github.vacancybot.model.BotUser;
import ru.github.vacancybot.model.EditingRef;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftEditor")
class DraftEditorTest {

  @Mock UserService userService;
  @Mock TaskService taskService;
  @Mock BotScreens screens;
  @InjectMocks DraftEditor editor;

  private void userEditing(String compositeId) {
    when(userService.getOrCreate(1L))
        .thenReturn(BotUser.builder().vkUserId(1L).editingTaskId(compositeId).build());
  }

  private static SearchTask task(String id) {
    return SearchTask.builder().id(id).ownerId(1L).keyword("").salaryMax(-1)
        .regionName(SearchTask.ALL_REGIONS).build();
  }

  @Test
  @DisplayName("editingRef — пусто, если пользователь ничего не редактирует")
  void noRef() {
    userEditing(null);

    assertThat(editor.editingRef(1L)).isEmpty();
    assertThat(editor.findDraft(1L)).isEmpty();
  }

  @Test
  @DisplayName("editingRef — разбирает композитный id")
  void compositeRef() {
    userEditing("d:o");

    assertThat(editor.editingRef(1L)).contains(new EditingRef("d", "o"));
  }

  @Test
  @DisplayName("update — изменяет, сохраняет и показывает экран заявки с подтверждением")
  void updateAppliesChange() {
    userEditing("d:o");
    SearchTask draft = task("d");
    when(taskService.findById("d")).thenReturn(Optional.of(draft));

    editor.update(1L, t -> t.setKeyword("java"), t -> "✅ ok " + t.getKeyword());

    verify(taskService).save(draft);
    assertThat(draft.getKeyword()).isEqualTo("java");
    ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
    verify(screens).taskScreen(eq(1L), text.capture(), eq(draft));
    assertThat(text.getValue()).startsWith("✅ ok java\n\n").contains("Ключевое слово: java");
  }

  @Test
  @DisplayName("update — без черновика ничего не делает")
  void updateWithoutDraftIsNoop() {
    userEditing("missing");
    when(taskService.findById("missing")).thenReturn(Optional.empty());

    editor.update(1L, t -> t.setKeyword("x"), t -> "");

    verify(taskService, never()).save(any());
    verify(screens, never()).taskScreen(anyLong(), anyString(), any());
  }

  @Test
  @DisplayName("discardDraft — удаляет черновик и сбрасывает ссылку")
  void discardDraft() {
    userEditing("d:o");

    editor.discardDraft(1L);

    verify(taskService).delete("d");
    verify(taskService, never()).delete("o");
    verify(userService).setEditingTaskId(1L, null);
  }

  @Test
  @DisplayName("discardDraft — без ссылки ничего не трогает")
  void discardWithoutRef() {
    userEditing(null);

    editor.discardDraft(1L);

    verify(taskService, never()).delete(anyString());
    verify(userService, never()).setEditingTaskId(anyLong(), any());
  }

  @Test
  @DisplayName("deleteQuietly — TaskNotFoundException не пробрасывается")
  void deleteQuietlySwallows() {
    doThrow(new TaskNotFoundException("x")).when(taskService).delete("x");

    assertThatCode(() -> editor.deleteQuietly("x")).doesNotThrowAnyException();
  }
}
