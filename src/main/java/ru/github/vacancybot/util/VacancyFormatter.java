package ru.github.vacancybot.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.github.vacancybot.model.Vacancy;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VacancyFormatter {

  public static String detailed(Vacancy v) {
    StringBuilder sb = new StringBuilder("📌 ").append(v.jobName()).append('\n');
    if (v.salaryMin() > 0) {
      sb.append("💰 Зарплата: от ").append(v.salaryMin());
      if (v.salaryMax() > v.salaryMin()) {
        sb.append(" до ").append(v.salaryMax());
      }
      sb.append(" руб.\n");
    }
    appendEmployer(sb, v);
    if (hasText(v.region())) {
      sb.append("📍 ").append(v.region()).append('\n');
    }
    return sb.append(v.vacUrl()).append("\n\n").toString();
  }

  private static void appendEmployer(StringBuilder sb, Vacancy v) {
    if (hasText(v.employer())) {
      sb.append("🏢 ").append(v.employer()).append('\n');
    }
  }

  private static boolean hasText(String s) {
    return s != null && !s.isBlank();
  }
}
