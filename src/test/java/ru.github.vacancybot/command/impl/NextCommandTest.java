package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
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
@DisplayName("NextCommand")
class NextCommandTest {

  @Mock BotScreens screens;

  @Test
  @DisplayName("листает на указанную страницу")
  void goesToPage() {
    new NextCommand(screens).handle(message(CommandType.NEXT, "4"));

    verify(screens).regionPage(USER, 4);
  }

  @Test
  @DisplayName("null и мусор — первая страница")
  void fallbackToFirst() {
    NextCommand command = new NextCommand(screens);

    command.handle(message(CommandType.NEXT, null));
    command.handle(message(CommandType.NEXT, "x"));

    verify(screens, org.mockito.Mockito.times(2)).regionPage(USER, 1);
  }

  @Test
  void type() {
    assertThat(new NextCommand(screens).getType()).isEqualTo(CommandType.NEXT);
  }
}
