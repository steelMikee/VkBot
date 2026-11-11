package ru.github.vacancybot.service;

import java.util.List;
import java.util.Optional;
import ru.github.vacancybot.model.SearchTask;

public interface TaskService {

  SearchTask createTask(long ownerId);

  Optional<SearchTask> findById(String id);

  List<SearchTask> findAllByOwner(long ownerId);

  List<SearchTask> findAll();

  void save(SearchTask task);

  void delete(String id);
}
