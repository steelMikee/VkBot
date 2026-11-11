package ru.github.vacancybot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.command.impl.KeywordCommand;
import ru.github.vacancybot.command.impl.MinExpCommand;
import ru.github.vacancybot.command.impl.MinZpCommand;
import ru.github.vacancybot.command.impl.RegionCommand;

@DisplayName("FilterCommands")
class FilterCommandsTest {

  @Test
  @DisplayName("all() возвращает четыре команды в порядке объявления")
  void allReturnsFour() {
    RegionCommand r = mock(RegionCommand.class);
    MinExpCommand e = mock(MinExpCommand.class);
    MinZpCommand z = mock(MinZpCommand.class);
    KeywordCommand k = mock(KeywordCommand.class);

    assertThat(new FilterCommands(r, e, z, k).all()).containsExactly(r, e, z, k);
  }
}
