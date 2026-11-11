package ru.github.vacancybot.service;

import ru.github.vacancybot.model.SearchTask;

public interface NotificationService {

  void notifyUser(SearchTask task);
}
