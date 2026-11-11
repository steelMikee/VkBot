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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
@DisplayName("MinExpCommand")
class MinExpCommandTest {

  @Mock VkMessageSender sender;
  @Mock UserService userService;
  @Mock DraftEditor draftEditor;
  @InjectMocks MinExpCommand command;

  @Test
  @DisplayName("без аргумента — AWAIT_EXPERIENCE и клавиатура вариантов")
  void showsOptions() {
    command.handle(message(CommandType.MIN_EXP, null));

    verify(userService).setDialogState(USER, DialogState.AWAIT_EXPERIENCE);
    verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.startsWith("🎓"), eq(KeyboardBuilder.experienceOptions()));
    verify(draftEditor, never()).update(anyLong(), any(), any());
  }

  @ParameterizedTest(name = "«{0}» → {1} лет")
  @CsvSource({"без опыта, 0", "0, 0", "1 год, 1", "1, 1", "2 года, 2", "2, 2", "3, 3", "много, 3"})
  @SuppressWarnings("unchecked")
  void parsesArgument(String arg, int expected) {
    command.handle(message(CommandType.MIN_EXP, arg));

    ArgumentCaptor<Consumer<SearchTask>> change = ArgumentCaptor.forClass(Consumer.class);
    verify(draftEditor).update(eq(USER), change.capture(), any());
    SearchTask task = newTask("x");
    change.getValue().accept(task);
    assertThat(task.getExperienceFrom()).isEqualTo(expected);
  }

  @Test
  @DisplayName("applyExp — текст подтверждения")
  @SuppressWarnings("unchecked")
  void confirmationText() {
    command.applyExp(USER, 2);

    ArgumentCaptor<Function<SearchTask, String>> text = ArgumentCaptor.forClass(Function.class);
    verify(draftEditor).update(eq(USER), any(), text.capture());
    assertThat(text.getValue().apply(newTask("x"))).isEqualTo("✅ Минимальный опыт: от 2 лет");
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.MIN_EXP);
  }
}
