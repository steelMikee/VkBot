package ru.github.vacancybot.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Исключения проекта")
class ExceptionsTest {

  @Test
  @DisplayName("TaskNotFoundException — текст совпадает с messageFor()")
  void taskNotFoundMessage() {
    assertThat(new TaskNotFoundException("abc"))
        .hasMessage("Задача с ID 'abc' не найдена.")
        .hasMessage(TaskNotFoundException.messageFor("abc"));
  }

  @Test
  @DisplayName("VacancyApiException — с причиной и без")
  void vacancyApiException() {
    RuntimeException cause = new RuntimeException("c");

    assertThat(new VacancyApiException("m")).hasMessage("m").hasNoCause();
    assertThat(new VacancyApiException("m", cause)).hasMessage("m").hasCause(cause);
  }

  @Test
  @DisplayName("StorageException — сохраняет причину")
  void storageException() {
    java.io.IOException cause = new java.io.IOException("io");

    assertThat(new StorageException("s", cause)).hasMessage("s").hasCause(cause);
  }
}
