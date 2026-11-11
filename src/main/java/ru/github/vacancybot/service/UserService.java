package ru.github.vacancybot.service;

import ru.github.vacancybot.model.BotUser;
import ru.github.vacancybot.model.DialogState;

public interface UserService {

  BotUser getOrCreate(long userId);

  DialogState getDialogState(long userId);

  void setDialogState(long userId, DialogState state);

  void setEditingTaskId(long userId, String taskId);
}
