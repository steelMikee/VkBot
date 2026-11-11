package ru.github.vacancybot.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("CommandType — индексы подписей и алиасов")
class CommandTypeTest {

  @ParameterizedTest(name = "label «{0}» → {1}")
  @CsvSource({
      "Начать, DEFAULT",
      "начать, DEFAULT",
      "ГОТОВО, DONE",
      "Создать заявку на поиск вакансий, NEW_TASK",
      "  Далее  , NEXT"})
  @DisplayName("fromLabel без учёта регистра и пробелов")
  void fromLabel(String label, CommandType expected) {
    assertThat(CommandType.fromLabel(label)).contains(expected);
  }

  @ParameterizedTest(name = "alias «{0}» → {1}")
  @CsvSource({"/start, DEFAULT", "/SHOWMENU, MAIN_MENU", "/minZp, MIN_ZP", "/stop, STOP"})
  @DisplayName("fromAlias без учёта регистра")
  void fromAlias(String alias, CommandType expected) {
    assertThat(CommandType.fromAlias(alias)).contains(expected);
  }

  @Test
  @DisplayName("неизвестный текст и null — Optional.empty")
  void unknownIsEmpty() {
    assertThat(CommandType.fromLabel("abc")).isEmpty();
    assertThat(CommandType.fromAlias("/abc")).isEmpty();
    assertThat(CommandType.fromLabel(null)).isEmpty();
    assertThat(CommandType.fromAlias(null)).isEmpty();
  }

  @Test
  @DisplayName("NONE не имеет ни подписи, ни алиаса")
  void noneHasNoLabel() {
    assertThat(CommandType.NONE.getLabel()).isNull();
    assertThat(CommandType.NONE.getAlias()).isNull();
  }
}
