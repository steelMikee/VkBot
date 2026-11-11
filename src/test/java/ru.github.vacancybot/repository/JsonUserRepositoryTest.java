package ru.github.vacancybot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.github.vacancybot.model.BotUser;
import ru.github.vacancybot.model.DialogState;

@Tag("integration")
@DisplayName("JsonUserRepository — файл на пользователя")
class JsonUserRepositoryTest {

  @TempDir
  Path tempDir;

  private JsonUserRepository repo;

  @BeforeEach
  void setUp() {
    repo = new JsonUserRepository(tempDir.toString());
  }

  @Test
  @DisplayName("save создаёт users/<id>.json, findById читает его")
  void saveAndFind() {
    repo.save(BotUser.builder().vkUserId(42L).dialogState(DialogState.AWAIT_SALARY)
        .editingTaskId("d:o").build());

    assertThat(Files.exists(tempDir.resolve("users/42.json"))).isTrue();
    BotUser user = repo.findById(42L).orElseThrow();
    assertThat(user.getDialogState()).isEqualTo(DialogState.AWAIT_SALARY);
    assertThat(user.getEditingTaskId()).isEqualTo("d:o");
  }

  @Test
  @DisplayName("отсутствующий пользователь — Optional.empty")
  void missing() {
    assertThat(repo.findById(1L)).isEmpty();
  }

  @Test
  @DisplayName("повреждённый файл — Optional.empty без исключения")
  void corrupted() throws Exception {
    Files.writeString(tempDir.resolve("users/7.json"), "???");

    assertThat(repo.findById(7L)).isEmpty();
  }

  @Test
  @DisplayName("старый формат файла (строковое состояние + firstName) читается")
  void legacyFormat() throws Exception {
    Files.writeString(tempDir.resolve("users/9.json"),
        "{\"vkUserId\":9,\"firstName\":\"X\",\"dialogState\":\"SCREEN_2\",\"editingTaskId\":null}");

    assertThat(repo.findById(9L).orElseThrow().getDialogState()).isEqualTo(DialogState.SCREEN_2);
  }
}
