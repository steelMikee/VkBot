package ru.github.vacancybot.bot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LongPollSession")
class LongPollSessionTest {

  @Test
  @DisplayName("формирует URL проверки с wait=25")
  void buildsCheckUrl() {
    LongPollSession s = new LongPollSession("https://lp.vk.com/x", "k", "1");

    assertThat(s.checkUrl()).isEqualTo("https://lp.vk.com/x?act=a_check&key=k&ts=1&wait=25");
  }

  @Test
  @DisplayName("withTs создаёт копию с новым ts, не меняя исходную")
  void withTsIsImmutable() {
    LongPollSession s = new LongPollSession("srv", "k", "1");

    LongPollSession next = s.withTs("2");

    assertThat(next.ts()).isEqualTo("2");
    assertThat(next.server()).isEqualTo("srv");
    assertThat(s.ts()).isEqualTo("1");
  }
}
