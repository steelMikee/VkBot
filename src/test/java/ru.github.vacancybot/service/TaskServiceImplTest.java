package ru.github.vacancybot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.exception.TaskNotFoundException;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl")
class TaskServiceImplTest {

  @Mock TaskRepository repository;
  @InjectMocks TaskServiceImpl service;

  @Test
  @DisplayName("createTask — уникальный id, дефолты фильтров, сохранение")
  void createTask() {
    SearchTask a = service.createTask(7L);
    SearchTask b = service.createTask(7L);

    assertThat(a.getId()).isNotBlank().isNotEqualTo(b.getId());
    assertThat(a.getOwnerId()).isEqualTo(7L);
    assertThat(a.getRegionCode()).isZero();
    assertThat(a.getRegionName()).isEqualTo(SearchTask.ALL_REGIONS);
    assertThat(a.getExperienceFrom()).isZero();
    assertThat(a.getSalaryMin()).isZero();
    assertThat(a.getSalaryMax()).isEqualTo(SearchTask.NO_SALARY_LIMIT);
    assertThat(a.getKeyword()).isEmpty();
    assertThat(a.getCreatedAt()).isPositive();
    verify(repository).save(a);
  }

  @Test
  @DisplayName("делегирование чтения в репозиторий")
  void delegatesReads() {
    SearchTask t = SearchTask.builder().id("x").ownerId(1).build();
    when(repository.findById("x")).thenReturn(Optional.of(t));
    when(repository.findAllByOwnerId(1)).thenReturn(List.of(t));
    when(repository.findAll()).thenReturn(List.of(t));

    assertThat(service.findById("x")).contains(t);
    assertThat(service.findAllByOwner(1)).containsExactly(t);
    assertThat(service.findAll()).containsExactly(t);
  }

  @Test
  @DisplayName("delete существующей — удаляет")
  void deleteExisting() {
    when(repository.findById("x")).thenReturn(Optional.of(SearchTask.builder().id("x").build()));

    service.delete("x");

    verify(repository).delete("x");
  }

  @Test
  @DisplayName("delete несуществующей — TaskNotFoundException, repository.delete не вызывается")
  void deleteMissing() {
    when(repository.findById("x")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete("x")).isInstanceOf(TaskNotFoundException.class);
    verify(repository, never()).delete(anyString());
  }
}
