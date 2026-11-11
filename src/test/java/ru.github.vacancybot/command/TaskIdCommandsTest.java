package ru.github.vacancybot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.command.impl.DeleteTaskCommand;
import ru.github.vacancybot.command.impl.UpdateTaskCommand;

@DisplayName("TaskIdCommands")
class TaskIdCommandsTest {

  @Test
  @DisplayName("all() возвращает delete и update")
  void allReturnsBoth() {
    DeleteTaskCommand d = mock(DeleteTaskCommand.class);
    UpdateTaskCommand u = mock(UpdateTaskCommand.class);

    assertThat(new TaskIdCommands(d, u).all()).containsExactly(d, u);
  }
}
