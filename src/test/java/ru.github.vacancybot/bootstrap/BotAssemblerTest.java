package ru.github.vacancybot.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.github.vacancybot.bot.VkBot;
import ru.github.vacancybot.config.AppConfig;
import ru.github.vacancybot.config.AppConfigTestSupport;

@Tag("integration")
@DisplayName("BotAssembler — сборка графа объектов")
class BotAssemblerTest {

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("собирает бота на чистой директории и создаёт подпапки хранилища")
  void assemblesBotAndCreatesStorageLayout() {
    AppConfig config = AppConfigTestSupport.config("token", "1", tempDir.toString(), "24", "1");

    VkBot bot = new BotAssembler(config).assemble();

    assertThat(bot).isNotNull();
    assertThat(Files.isDirectory(tempDir.resolve("users"))).isTrue();
    assertThat(Files.isDirectory(tempDir.resolve("tasks"))).isTrue();
    assertThat(Files.isDirectory(tempDir.resolve("vacancies"))).isTrue();
  }
}
