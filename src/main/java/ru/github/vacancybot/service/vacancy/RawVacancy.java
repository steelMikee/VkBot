package ru.github.vacancybot.service.vacancy;

import ru.github.vacancybot.model.Vacancy;

public record RawVacancy(String jobName, String vacUrl, String employer, String region,
                         int salaryMin, int salaryMax, int experienceYears) {

  public boolean hasNoSalary() {
    return salaryMin == 0 && salaryMax == 0;
  }

  public RawVacancy withSalaryFloor(int floor) {
    return hasNoSalary()
        ? new RawVacancy(jobName, vacUrl, employer, region, floor, salaryMax, experienceYears)
        : this;
  }

  public Vacancy toVacancy() {
    return new Vacancy(jobName, salaryMin, salaryMax, vacUrl, employer, region);
  }
}
