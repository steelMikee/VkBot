package ru.github.vacancybot.service.vacancy;

import java.util.List;

public record VacancyPage(int total, List<RawVacancy> vacancies) {

    public static VacancyPage empty() {
        return new VacancyPage(0, List.of());
    }
}
