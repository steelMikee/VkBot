package ru.github.vacancybot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AppConfig")
class AppConfigTest {

  @Test
  @DisplayName("читает все значения и приводит числа")
  void readsAllValues() {
    AppConfig config = AppConfigTestSupport.config("tok", "123", "/tmp/x", "12", "5");

    assertThat(config.getVkGroupToken()).isEqualTo("tok");
    assertThat(config.getVkGroupId()).isEqualTo(123L);
    assertThat(config.getDataDir()).isEqualTo("/tmp/x");
    assertThat(config.getNotificationPeriodHours()).isEqualTo(12L);
    assertThat(config.getSchedulerInitialDelayMinutes()).isEqualTo(5L);
  }

  @Test
  @DisplayName("подставляет значения по умолчанию для необязательных параметров")
  void usesDefaults() {
    Dotenv dotenv = mock(Dotenv.class);
    when(dotenv.get("VK_ACCESS_TOKEN")).thenReturn("tok");
    when(dotenv.get("VK_GROUP_ID")).thenReturn("1");
    when(dotenv.get("DATA_DIR", "data")).thenReturn("data");
    when(dotenv.get("NOTIFICATION_PERIOD_HOURS", "24")).thenReturn("24");
    when(dotenv.get("SCHEDULER_INITIAL_DELAY_MINUTES", "1")).thenReturn("1");

    AppConfig config = new AppConfig(dotenv);

    assertThat(config.getDataDir()).isEqualTo("data");
    assertThat(config.getNotificationPeriodHours()).isEqualTo(24L);
    assertThat(config.getSchedulerInitialDelayMinutes()).isEqualTo(1L);
  }

  @Test
  @DisplayName("пустой токен — IllegalStateException с именем переменной")
  void blankTokenFails() {
    assertThatThrownBy(() -> AppConfigTestSupport.config("  ", "1", "d", "24", "1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("VK_ACCESS_TOKEN");
  }

  @Test
  @DisplayName("нечисловой GROUP_ID — понятная ошибка")
  void nonNumericGroupIdFails() {
    assertThatThrownBy(() -> AppConfigTestSupport.config("tok", "abc", "d", "24", "1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("VK_GROUP_ID")
        .hasMessageContaining("abc");
  }

  @Test
  @DisplayName("нечисловой период уведомлений — понятная ошибка")
  void nonNumericPeriodFails() {
    assertThatThrownBy(() -> AppConfigTestSupport.config("tok", "1", "d", "day", "1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("NOTIFICATION_PERIOD_HOURS");
  }
}
