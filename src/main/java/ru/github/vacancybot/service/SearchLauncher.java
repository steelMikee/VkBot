package ru.github.vacancybot.service;

import ru.github.vacancybot.model.SearchTask;

public interface SearchLauncher {

  void launch(SearchTask task);

  void scheduleUpdates(SearchTask task);
}
