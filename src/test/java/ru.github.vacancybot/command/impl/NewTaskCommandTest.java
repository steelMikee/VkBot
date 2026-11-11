package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.message;
import static ru.github.vacancybot.command.impl.CommandTestSupport.newTask;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.TaskFormatter;

@ExtendWith(MockitoExtension.class)
@DisplayName("NewTaskCommand")
class NewTaskCommandTest {

  @Mock UserService userService;
  @Mock TaskService taskService;
  @Mock BotScreens screens;
  @InjectMocks NewTaskCommand command;

  @Test
  @DisplayName("создаёт заявку, запоминает её id и показывает экран 3")
  void createsTask() {
    SearchTask task = newTask("new");
    when(taskService.createTask(USER)).thenReturn(task);

    command.handle(message(CommandType.NEW_TASK, null));

    verify(userService).setEditingTaskId(USER, "new");
    verify(screens).taskScreen(eq(USER), eq(TaskFormatter.taskInfo(task)), eq(task));
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.NEW_TASK);
  }
}
