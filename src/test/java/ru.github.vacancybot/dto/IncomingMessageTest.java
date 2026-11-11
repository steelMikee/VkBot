package ru.github.vacancybot.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.command.CommandType;

@DisplayName("IncomingMessage")
class IncomingMessageTest {

  @Test
  @DisplayName("null rawText нормализуется в пустую строку")
  void nullTextBecomesEmpty() {
    IncomingMessage m = new IncomingMessage(1L, null, new ParsedCommand(CommandType.NONE, null), null);

    assertThat(m.rawText()).isEmpty();
    assertThat(m.trimmedText()).isEmpty();
    assertThat(m.normalizedText()).isEmpty();
  }

  @Test
  @DisplayName("trimmedText / normalizedText")
  void textHelpers() {
    IncomingMessage m = new IncomingMessage(1L, "  ПрИвЕт ", null, null);

    assertThat(m.trimmedText()).isEqualTo("ПрИвЕт");
    assertThat(m.normalizedText()).isEqualTo("привет");
  }

  @Test
  @DisplayName("withCommand сохраняет всё, кроме команды")
  void withCommand() {
    IncomingMessage m = new IncomingMessage(1L, "t", new ParsedCommand(CommandType.NONE, "x"), "p");

    IncomingMessage copy = m.withCommand(CommandType.REGION, "77");

    assertThat(copy.userId()).isEqualTo(1L);
    assertThat(copy.rawText()).isEqualTo("t");
    assertThat(copy.payload()).isEqualTo("p");
    assertThat(copy.parsedCommand()).isEqualTo(new ParsedCommand(CommandType.REGION, "77"));
    assertThat(m.parsedCommand().type()).isEqualTo(CommandType.NONE);
  }
}
