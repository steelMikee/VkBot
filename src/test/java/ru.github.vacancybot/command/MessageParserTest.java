package ru.github.vacancybot.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.github.vacancybot.dto.ParsedCommand;

@DisplayName("MessageParser")
class MessageParserTest {

  private final MessageParser parser = new MessageParser();

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("пустой текст → NONE без аргумента")
  void blankIsNone(String text) {
    assertThat(parser.parse(text)).isEqualTo(new ParsedCommand(CommandType.NONE, null));
  }

  @Test
  @DisplayName("«Назад к заявке» → MAIN_MENU back_to_task")
  void backToTask() {
    assertThat(parser.parse("назад к заявке"))
        .isEqualTo(new ParsedCommand(CommandType.MAIN_MENU, MessageParser.ARG_BACK_TO_TASK));
  }

  @Test
  @DisplayName("«В главное меню» → MAIN_MENU root_menu")
  void rootMenu() {
    assertThat(parser.parse("В главное меню"))
        .isEqualTo(new ParsedCommand(CommandType.MAIN_MENU, MessageParser.ARG_ROOT_MENU));
  }

  @Test
  @DisplayName("«Сначала» → NEXT 1")
  void firstPage() {
    assertThat(parser.parse("Сначала")).isEqualTo(new ParsedCommand(CommandType.NEXT, "1"));
  }

  @Test
  @DisplayName("стрелки пагинации извлекают номер страницы")
  void arrows() {
    assertThat(parser.parse("-> [3]")).isEqualTo(new ParsedCommand(CommandType.NEXT, "3"));
    assertThat(parser.parse("<- [2]")).isEqualTo(new ParsedCommand(CommandType.NEXT, "2"));
    assertThat(parser.parse("->")).isEqualTo(new ParsedCommand(CommandType.NEXT, "1"));
  }

  @Test
  @DisplayName("подпись кнопки → команда без аргумента")
  void label() {
    assertThat(parser.parse("Готово")).isEqualTo(new ParsedCommand(CommandType.DONE, null));
  }

  @Test
  @DisplayName("алиас с аргументом")
  void aliasWithArgument() {
    assertThat(parser.parse("/region 77")).isEqualTo(new ParsedCommand(CommandType.REGION, "77"));
    assertThat(parser.parse("/keyword   Java  dev "))
        .isEqualTo(new ParsedCommand(CommandType.KEYWORD, "Java  dev"));
  }

  @Test
  @DisplayName("алиас без аргумента")
  void aliasWithoutArgument() {
    assertThat(parser.parse("/start")).isEqualTo(new ParsedCommand(CommandType.DEFAULT, null));
  }

  @Test
  @DisplayName("свободный текст → NONE с текстом в аргументе")
  void freeText() {
    assertThat(parser.parse("  Москва ")).isEqualTo(new ParsedCommand(CommandType.NONE, "Москва"));
  }
}
