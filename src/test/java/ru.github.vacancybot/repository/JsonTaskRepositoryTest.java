package ru.github.vacancybot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.github.vacancybot.model.SearchTask;

@Tag("integration")
@DisplayName("JsonTaskRepository — tasks.json + кэш")
class JsonTaskRepositoryTest {

  @TempDir
  Path tempDir;

  private JsonTaskRepository repo;

  @BeforeEach
  void setUp() {
    repo = new JsonTaskRepository(tempDir.toString());
  }

  private static SearchTask task(String id, long owner) {
    return SearchTask.builder().id(id).ownerId(owner).keyword("").salaryMax(-1)
        .regionName(SearchTask.ALL_REGIONS).build();
  }

  @Test
  @DisplayName("пустое хранилище")
  void empty() {
    assertThat(repo.findAll()).isEmpty();
    assertThat(repo.findById("x")).isEmpty();
    assertThat(repo.findAllByOwnerId(1)).isEmpty();
  }

  @Test
  @DisplayName("save → файл tasks.json создан, findById/findAllByOwnerId работают")
  void saveAndFind() {
    repo.save(task("a", 1));
    repo.save(task("b", 2));

    assertThat(Files.exists(tempDir.resolve("tasks/tasks.json"))).isTrue();
    assertThat(repo.findById("a")).isPresent();
    assertThat(repo.findAllByOwnerId(1)).extracting(SearchTask::getId).containsExactly("a");
    assertThat(repo.findAll()).hasSize(2);
  }

  @Test
  @DisplayName("save существующего id — замена, а не дубликат; порядок как в исходной версии")
  void saveReplacesAndMovesToEnd() {
    repo.save(task("a", 1));
    repo.save(task("b", 1));
    SearchTask updated = task("a", 1);
    updated.setKeyword("java");

    repo.save(updated);

    assertThat(repo.findAll()).extracting(SearchTask::getId).containsExactly("b", "a");
    assertThat(repo.findById("a").orElseThrow().getKeyword()).isEqualTo("java");
  }

  @Test
  @DisplayName("возвращаются копии: изменение без save не влияет на хранилище")
  void returnsDefensiveCopies() {
    repo.save(task("a", 1));

    repo.findById("a").orElseThrow().setKeyword("hacked");

    assertThat(repo.findById("a").orElseThrow().getKeyword()).isEmpty();
  }

  @Test
  @DisplayName("delete удаляет и сохраняет; повторный delete — без записи и ошибок")
  void delete() {
    repo.save(task("a", 1));

    repo.delete("a");
    repo.delete("a");

    assertThat(repo.findAll()).isEmpty();
  }

  @Test
  @DisplayName("данные переживают перезапуск (новый экземпляр читает файл)")
  void persistsAcrossInstances() {
    repo.save(task("a", 1));

    JsonTaskRepository fresh = new JsonTaskRepository(tempDir.toString());

    assertThat(fresh.findById("a")).isPresent();
  }

  @Test
  @DisplayName("повреждённый tasks.json — пустое хранилище без исключения")
  void corruptedFile() throws Exception {
    Files.createDirectories(tempDir.resolve("tasks"));
    Files.writeString(tempDir.resolve("tasks/tasks.json"), "{not json");

    JsonTaskRepository fresh = new JsonTaskRepository(tempDir.toString());

    assertThat(fresh.findAll()).isEmpty();
  }

  @Test
  @DisplayName("конкурентные save из нескольких потоков не теряют записи")
  void concurrentSaves() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(8);
    List<Runnable> jobs = IntStream.range(0, 50)
        .<Runnable>mapToObj(i -> () -> repo.save(task("t" + i, i % 3))).toList();

    jobs.forEach(pool::submit);
    pool.shutdown();
    assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

    assertThat(repo.findAll()).hasSize(50);
    assertThat(new JsonTaskRepository(tempDir.toString()).findAll()).hasSize(50);
  }
}
