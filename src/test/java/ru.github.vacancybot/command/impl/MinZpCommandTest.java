package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.message;
import static ru.github.vacancybot.command.impl.CommandTestSupport.newTask;

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
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinZpCommand")
class MinZpCommandTest {

  @Mock VkMessageSender sender;
  @Mock UserService userService;
  @Mock DraftEditor draftEditor;
  @InjectMocks MinZpCommand command;

  @Test
  @DisplayName("без аргумента — AWAIT_SALARY и клавиатура вилок")
  void showsOptions() {
    command.handle(message(CommandType.MIN_ZP, null));

    verify(userService).setDialogState(USER, DialogState.AWAIT_SALARY);
    verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.startsWith("💰"), eq(KeyboardBuilder.salaryOptions()));
  }

  @Test
  @DisplayName("числовой аргумент — «от N», без верхней границы")
  @SuppressWarnings("unchecked")
  void numericArgument() {
    command.handle(message(CommandType.MIN_ZP, " 70000 "));

    ArgumentCaptor<Consumer<SearchTask>> change = ArgumentCaptor.forClass(Consumer.class);
    ArgumentCaptor<Function<SearchTask, String>> text = ArgumentCaptor.forClass(Function.class);
    verify(draftEditor).update(eq(USER), change.capture(), text.capture());
    SearchTask task = newTask("x");
    change.getValue().accept(task);
    assertThat(task.getSalaryMin()).isEqualTo(70_000);
    assertThat(task.getSalaryMax()).isEqualTo(SearchTask.NO_SALARY_LIMIT);
    assertThat(text.getValue().apply(task)).isEqualTo("✅ Зарплата: от 70000 руб.");
  }

  @Test
  @DisplayName("нечисловой аргумент — сообщение об ошибке формата")
  void invalidArgument() {
    command.handle(message(CommandType.MIN_ZP, "много"));

    verify(sender).sendText(eq(USER), org.mockito.ArgumentMatchers.contains("Некорректный формат зарплаты"));
    verify(draftEditor, never()).update(anyLong(), any(), any());
  }

  @Test
  @DisplayName("applySalaryRange — диапазон")
  @SuppressWarnings("unchecked")
  void applyRange() {
    command.applySalaryRange(USER, 50_000, 70_000);

    ArgumentCaptor<Consumer<SearchTask>> change = ArgumentCaptor.forClass(Consumer.class);
    verify(draftEditor).update(eq(USER), change.capture(), any());
    SearchTask task = newTask("x");
    change.getValue().accept(task);
    assertThat(task.getSalaryMin()).isEqualTo(50_000);
    assertThat(task.getSalaryMax()).isEqualTo(70_000);
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.MIN_ZP);
  }
}
