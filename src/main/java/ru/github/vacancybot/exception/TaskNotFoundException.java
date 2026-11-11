package ru.github.vacancybot.exception;

public class TaskNotFoundException extends RuntimeException {

  public TaskNotFoundException(String taskId) {
    super(messageFor(taskId));
  }

  public static String messageFor(String taskId) {
    return "Задача с ID '" + taskId + "' не найдена.";
  }
}
