package ru.github.vacancybot.client;

import ru.github.vacancybot.model.SearchTask;

public interface VacancyApiClient {

  int PAGE_LIMIT = 100;

  String fetchPage(SearchTask task, int offset);

  String fetchRegion(int regionCode);
}
