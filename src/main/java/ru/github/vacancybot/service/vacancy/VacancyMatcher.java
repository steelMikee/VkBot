package ru.github.vacancybot.service.vacancy;

import java.util.List;
import java.util.Locale;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;
import ru.github.vacancybot.util.RegionDirectory;

public final class VacancyMatcher {

  private final SearchTask task;
  private final String requiredRegion;
  private final String keyword;

  public VacancyMatcher(SearchTask task) {
    this.task = task;
    this.requiredRegion = task.hasRegion() && task.getRegionName() != null
        ? RegionDirectory.NOISE_WORDS.matcher(task.getRegionName()).replaceAll("").trim()
            .toLowerCase(Locale.ROOT)
        : null;
    this.keyword = task.hasKeyword() ? task.getKeyword().toLowerCase(Locale.ROOT).trim() : null;
  }

  public List<Vacancy> select(List<RawVacancy> raws) {
    return raws.stream()
        .map(this::applySalaryFloor)
        .filter(this::matches)
        .map(RawVacancy::toVacancy)
        .toList();
  }

  private RawVacancy applySalaryFloor(RawVacancy raw) {
    return task.getSalaryMin() > 0 ? raw.withSalaryFloor(task.getSalaryMin()) : raw;
  }

  boolean matches(RawVacancy v) {
    return matchesExperience(v) && matchesSalary(v)
        && matchesRegion(v.region()) && matchesKeyword(v.jobName());
  }

  private boolean matchesExperience(RawVacancy v) {
    return task.getExperienceFrom() <= 0 || v.experienceYears() >= task.getExperienceFrom();
  }

  private boolean matchesSalary(RawVacancy v) {
    int required = task.getSalaryMin();
    if (required > 0) {
      if (v.salaryMin() > 0 && v.salaryMin() < required) {
        return false;
      }
      if (v.salaryMin() == 0 && v.salaryMax() > 0 && v.salaryMax() < required) {
        return false;
      }
    }
    return !task.hasSalaryCeiling() || v.salaryMin() <= task.getSalaryMax();
  }

  private boolean matchesRegion(String address) {
    return requiredRegion == null || address.toLowerCase(Locale.ROOT).contains(requiredRegion);
  }

  private boolean matchesKeyword(String title) {
    return keyword == null || title.toLowerCase(Locale.ROOT).trim().contains(keyword);
  }
}
