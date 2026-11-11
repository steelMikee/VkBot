package ru.github.vacancybot.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import ru.github.vacancybot.model.SearchTask;

@Slf4j
public class JsonTaskRepository extends AbstractJsonRepository implements TaskRepository {

  private static final String SUBDIR = "tasks";
  private static final String FILENAME = "tasks.json";
  private static final TypeReference<List<SearchTask>> LIST_TYPE = new TypeReference<>() {};

  private final Path tasksFile;
  private final Map<String, SearchTask> cache = new LinkedHashMap<>();

  public JsonTaskRepository(String dataDir) {
    super(Path.of(dataDir, SUBDIR));
    this.tasksFile = this.dataDir.resolve(FILENAME);
    load();
  }

  private void load() {
    if (!Files.exists(tasksFile)) {
      return;
    }
    try {
      read(tasksFile, LIST_TYPE).forEach(task -> cache.put(task.getId(), task));
    } catch (IOException e) {
      log.error("Failed to read tasks file {}: {}", tasksFile, e.getMessage());
    }
  }

  @Override
  public synchronized Optional<SearchTask> findById(String id) {
    return Optional.ofNullable(cache.get(id)).map(this::copy);
  }

  @Override
  public synchronized List<SearchTask> findAllByOwnerId(long ownerId) {
    return cache.values().stream()
        .filter(task -> task.getOwnerId() == ownerId)
        .map(this::copy)
        .toList();
  }

  @Override
  public synchronized List<SearchTask> findAll() {
    return cache.values().stream().map(this::copy).toList();
  }

  @Override
  public synchronized void save(SearchTask task) {
    cache.remove(task.getId());
    cache.put(task.getId(), copy(task));
    persist();
  }

  @Override
  public synchronized void delete(String id) {
    if (cache.remove(id) != null) {
      persist();
    }
  }

  private void persist() {
    write(tasksFile, new ArrayList<>(cache.values()));
  }

  private SearchTask copy(SearchTask task) {
    return task.toBuilder().build();
  }
}
