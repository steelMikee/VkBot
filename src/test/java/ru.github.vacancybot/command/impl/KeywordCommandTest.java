package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.message;
import static ru.github.vacancybot.command.impl.CommandTestSupport.newTask;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.EditingRef;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordCommand")
class KeywordCommandTest {

  @Mock VkMessageSender sender;
  @Mock UserService userService;
  @Mock DraftEditor draftEditor;
  @Mock BotScreens screens;
  @InjectMocks KeywordCommand command;

  @Test
  @DisplayName("без аргумента — AWAIT_KEYWORD и приглашение ввести текст")
  void asksForKeyword() {
    command.handle(message(CommandType.KEYWORD, null));

    verify(userService).setDialogState(USER, DialogState.AWAIT_KEYWORD);
    verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.startsWith("🔍 Введите"));
  }

  @Test
  @DisplayName("с аргументом — сразу применяется как ключевое слово")
  @SuppressWarnings("unchecked")
  void argumentApplied() {
    when(draftEditor.editingRef(USER)).thenReturn(Optional.of(new EditingRef("d", null)));

    command.handle(message(CommandType.KEYWORD, "  Java  "));

    ArgumentCaptor<Consumer<SearchTask>> change = ArgumentCaptor.forClass(Consumer.class);
    ArgumentCaptor<Function<SearchTask, String>> text = ArgumentCaptor.forClass(Function.class);
    verify(draftEditor).update(eq(USER), change.capture(), text.capture());
    SearchTask task = newTask("d");
    change.getValue().accept(task);
    assertThat(task.getKeyword()).isEqualTo("Java");
    assertThat(text.getValue().apply(task)).isEqualTo("✅ Ключевое слово: «Java»");
  }

  @Test
  @DisplayName("handleText без активной задачи — главное меню с ошибкой")
  void noActiveTask() {
    when(draftEditor.editingRef(USER)).thenReturn(Optional.empty());

    command.handleText(message(CommandType.NONE, null), "x");

    verify(screens).mainMenu(eq(USER), org.mockito.ArgumentMatchers.contains("Нет активной задачи"));
    verify(draftEditor, never()).update(anyLong(), any(), any());
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.KEYWORD);
  }
}
