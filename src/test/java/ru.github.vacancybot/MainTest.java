package ru.github.vacancybot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@DisplayName("Main")
class MainTest {

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  @DisplayName("без обязательных переменных окружения приложение падает fail-fast")
  void failsFastWithoutConfiguration() {

    Throwable thrown = catchThrowable(() -> Main.main(new String[0]));

    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("VK_ACCESS_TOKEN");

    System.out.println("\n========================================================");
    System.out.println("[TEST PROOF] Тест успешно перехватил фатальную ошибку!");
    System.out.println("[TEST PROOF] Текст исключения: " + thrown.getMessage());
    System.out.println("========================================================\n");
  }
}
