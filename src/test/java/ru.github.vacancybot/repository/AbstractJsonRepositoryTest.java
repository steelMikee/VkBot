package ru.github.vacancybot.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.github.vacancybot.exception.StorageException;

@Tag("integration")
@DisplayName("AbstractJsonRepository — базовые файловые операции")
class AbstractJsonRepositoryTest {

  private static class TestRepo extends AbstractJsonRepository {
    TestRepo(Path dir) {
      super(dir);
    }
  }

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("конструктор создаёт вложенные директории")
  void createsDirectories() {
    Path nested = tempDir.resolve("a/b/c");

    new TestRepo(nested);

    assertThat(Files.isDirectory(nested)).isTrue();
  }

  @Test
  @DisplayName("write → read: round-trip с отступами, временный файл не остаётся")
  @SuppressWarnings("unchecked")
  void writeAndRead() throws Exception {
    TestRepo repo = new TestRepo(tempDir);
    Path file = tempDir.resolve("x.json");

    repo.write(file, Map.of("k", "v"));

    assertThat(Files.readString(file)).contains("\"k\" : \"v\"");
    assertThat(Files.exists(tempDir.resolve("x.json.tmp"))).isFalse();
    Map<String, Object> result = repo.read(file, Map.class);
    assertThat(result).containsEntry("k", "v");
  }

  @Test
  @DisplayName("write перезаписывает существующий файл атомарно")
  @SuppressWarnings("unchecked")
  void overwrites() throws Exception {
    TestRepo repo = new TestRepo(tempDir);
    Path file = tempDir.resolve("x.json");
    repo.write(file, List.of(1, 2, 3));

    repo.write(file, List.of(9));

    List<Integer> result = repo.read(file, List.class);
    assertThat(result).containsExactly(9);
  }

  @Test
  @DisplayName("deleteIfExists — не падает на отсутствующем файле")
  void deleteMissing() {
    TestRepo repo = new TestRepo(tempDir);

    repo.deleteIfExists(tempDir.resolve("nope.json"));
  }

  @Test
  @DisplayName("ошибка записи (директория вместо файла) → StorageException")
  void writeErrorWrapped() throws Exception {
    TestRepo repo = new TestRepo(tempDir);
    Path dirAsFile = Files.createDirectory(tempDir.resolve("dir.json"));
    Files.createFile(dirAsFile.resolve("child"));

    assertThatThrownBy(() -> repo.write(dirAsFile, "x")).isInstanceOf(StorageException.class);
  }
}
