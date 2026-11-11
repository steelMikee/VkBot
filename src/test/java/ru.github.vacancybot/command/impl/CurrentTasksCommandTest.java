package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.util.KeyboardBuilder;
import ru.github.vacancybot.util.TaskFormatter;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrentTasksCommand")
class CurrentTasksCommandTest {

  @Mock VkMessageSender sender;
  @Mock TaskService taskService;
  @InjectMocks CurrentTasksCommand command;

  @Test
  @DisplayName("нет заявок — сообщение с клавиатурой экрана 2")
  void noTasks() {
    when(taskService.findAllByOwner(USER)).thenReturn(List.of());

    command.handle(message(CommandType.CURRENT_TASKS, null));

    verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.startsWith("📭"), eq(KeyboardBuilder.screen2()));
  }

  @Test
  @DisplayName("заголовок, затем карточка каждой заявки с inline-кнопками")
  void listsTasks() {
    SearchTask a = newTask("a");
    SearchTask b = newTask("b");
    when(taskService.findAllByOwner(USER)).thenReturn(List.of(a, b));

    command.handle(message(CommandType.CURRENT_TASKS, null));

    InOrder order = Mockito.inOrder(sender);
    order.verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.startsWith("📋"), eq(KeyboardBuilder.screen2()));
    order.verify(sender).sendWithKeyboard(USER, TaskFormatter.taskCard(a), KeyboardBuilder.taskInlineKeyboard("a"));
    order.verify(sender).sendWithKeyboard(USER, TaskFormatter.taskCard(b), KeyboardBuilder.taskInlineKeyboard("b"));
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.CURRENT_TASKS);
  }
}
