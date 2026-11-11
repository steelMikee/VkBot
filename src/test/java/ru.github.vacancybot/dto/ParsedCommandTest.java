package ru.github.vacancybot.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.command.CommandType;

@DisplayName("ParsedCommand")
class ParsedCommandTest {

  @Test
  @DisplayName("record: равенство по значению")
  void valueEquality() {
    assertThat(new ParsedCommand(CommandType.DONE, null))
        .isEqualTo(new ParsedCommand(CommandType.DONE, null))
        .isNotEqualTo(new ParsedCommand(CommandType.DONE, "x"));
  }
}
