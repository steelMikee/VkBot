package ru.github.vacancybot.repository;

import java.util.List;
import java.util.Optional;
import ru.github.vacancybot.model.SearchTask;

public interface TaskRepository {

  Optional<SearchTask> findById(String id);

  List<SearchTask> findAllByOwnerId(long ownerId);

  List<SearchTask> findAll();

  void save(SearchTask task);

  void delete(String id);
}
