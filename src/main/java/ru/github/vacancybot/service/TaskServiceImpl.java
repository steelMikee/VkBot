package ru.github.vacancybot.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.exception.TaskNotFoundException;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.repository.TaskRepository;

@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

  private final TaskRepository taskRepository;

  @Override
  public SearchTask createTask(long ownerId) {
    SearchTask task = SearchTask.builder()
        .id(UUID.randomUUID().toString())
        .ownerId(ownerId)
        .regionName(SearchTask.ALL_REGIONS)
        .salaryMax(SearchTask.NO_SALARY_LIMIT)
        .keyword("")
        .createdAt(System.currentTimeMillis())
        .build();
    taskRepository.save(task);
    return task;
  }

  @Override
  public Optional<SearchTask> findById(String id) {
    return taskRepository.findById(id);
  }

  @Override
  public List<SearchTask> findAllByOwner(long ownerId) {
    return taskRepository.findAllByOwnerId(ownerId);
  }

  @Override
  public List<SearchTask> findAll() {
    return taskRepository.findAll();
  }

  @Override
  public void save(SearchTask task) {
    taskRepository.save(task);
  }

  @Override
  public void delete(String id) {
    taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    taskRepository.delete(id);
  }
}
