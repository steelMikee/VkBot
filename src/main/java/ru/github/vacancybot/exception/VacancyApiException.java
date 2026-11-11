package ru.github.vacancybot.exception;

public class VacancyApiException extends RuntimeException {

  public VacancyApiException(String message) {
    super(message);
  }

  public VacancyApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
