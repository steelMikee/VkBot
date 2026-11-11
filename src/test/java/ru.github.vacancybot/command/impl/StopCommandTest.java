package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.SchedulerService;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;

@ExtendWith(MockitoExtension.class)
@DisplayName("StopCommand")
class StopCommandTest {

  @Mock VkMessageSender sender;
  @Mock UserService userService;
  @Mock SchedulerService schedulerService;
  @InjectMocks StopCommand command;

  @Test
  @DisplayName("отменяет задачи планировщика, переводит в SCREEN_1 и показывает кнопку «Начать»")
  void stops() {
    command.handle(message(CommandType.STOP, null));

    verify(schedulerService).cancel(USER);
    verify(userService).setDialogState(USER, DialogState.SCREEN_1);
    verify(sender).sendWithKeyboard(eq(USER), org.mockito.ArgumentMatchers.startsWith("⛔"), eq(KeyboardBuilder.screen1()));
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.STOP);
  }
}
