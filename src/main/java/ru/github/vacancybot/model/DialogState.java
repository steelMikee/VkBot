package ru.github.vacancybot.model;

public enum DialogState {
  SCREEN_1,
  SCREEN_2,
  CREATING_TASK,
  AWAIT_REGION,
  AWAIT_EXPERIENCE,
  AWAIT_SALARY,
  AWAIT_KEYWORD,
  AWAIT_TASK_ID_DELETE,
  AWAIT_TASK_ID_UPDATE;

  public boolean isTaskEditing() {
    return this != SCREEN_1 && this != SCREEN_2;
  }

  public boolean isFilterChoice() {
    return this == AWAIT_REGION || this == AWAIT_EXPERIENCE || this == AWAIT_SALARY;
  }
}
