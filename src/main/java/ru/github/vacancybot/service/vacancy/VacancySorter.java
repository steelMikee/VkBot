package ru.github.vacancybot.service.vacancy;

import java.util.Comparator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VacancySorter {

  private static final Comparator<Vacancy> BY_SALARY_MIN_DESC =
      Comparator.comparingInt(Vacancy::salaryMin).reversed();
  private static final Comparator<Vacancy> BY_EFFECTIVE_SALARY_DESC =
      Comparator.comparingInt(VacancySorter::effectiveSalary).reversed();
  private static final Comparator<Vacancy> NO_SALARY_LAST =
      Comparator.comparing(VacancySorter::hasNoSalary);

  public static Comparator<Vacancy> forTask(SearchTask task) {
    if (task.getSalaryMin() <= 0) {
      return BY_SALARY_MIN_DESC;
    }
    if (task.hasSalaryCeiling()) {
      double idealCenter = (task.getSalaryMin() + task.getSalaryMax()) / 2.0;
      return NO_SALARY_LAST
          .thenComparingDouble(v -> Math.abs(effectiveSalary(v) - idealCenter))
          .thenComparing(BY_SALARY_MIN_DESC);
    }
    return NO_SALARY_LAST.thenComparing(BY_EFFECTIVE_SALARY_DESC);
  }

  private static boolean hasNoSalary(Vacancy v) {
    return v.salaryMin() == 0 && v.salaryMax() == 0;
  }

  private static int effectiveSalary(Vacancy v) {
    return v.salaryMin() > 0 ? v.salaryMin() : v.salaryMax();
  }
}
