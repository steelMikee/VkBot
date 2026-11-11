package ru.github.vacancybot.model;

public record Vacancy(String jobName, int salaryMin, int salaryMax, String vacUrl,
                      String employer, String region) {}
