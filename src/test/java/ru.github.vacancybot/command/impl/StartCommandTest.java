package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;

@ExtendWith(MockitoExtension.class)
@DisplayName("StartCommand")
class StartCommandTest {

  @Mock BotScreens screens;

  @Test
  @DisplayName("показывает главное меню с приветствием")
  void showsMainMenu() {
    new StartCommand(screens).handle(message(CommandType.DEFAULT, null));

    verify(screens).mainMenu(eq(USER), org.mockito.ArgumentMatchers.startsWith("👋 Привет!"));
  }

  @Test
  void type() {
    assertThat(new StartCommand(screens).getType()).isEqualTo(CommandType.DEFAULT);
  }
}
