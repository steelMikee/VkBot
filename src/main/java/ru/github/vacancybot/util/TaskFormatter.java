package ru.github.vacancybot.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.github.vacancybot.model.SearchTask;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskFormatter {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

  private static final String REGION_PROMPT = """
          📍 Выберите Регион по кнопкам в меню ниже.
          Можете ввести Код региона и/или его Наименование.

          Примеры ввода:
          77
          Москва
          77 Москва""";

  public static String taskInfo(SearchTask task) {
    String region = task.hasRegion()
        ? "[" + task.getRegionCode() + "] " + task.getRegionName()
        : "не выбран (все регионы)";
    String keyword = task.getKeyword() == null || task.getKeyword().isBlank()
        ? "не задано" : task.getKeyword();
    return "🆕 Новая заявка на поиск вакансий.\n"
        + "Задайте параметры поиска (в любом порядке):\n\n"
        + "📍 Регион: " + region + "\n"
        + "🎓 Мин. опыт: " + formatExp(task.getExperienceFrom()) + "\n"
        + "💰 Зарплата: " + formatSalary(task) + "\n"
        + "🔍 Ключевое слово: " + keyword;
  }

  public static String taskCard(SearchTask task) {
    String region = task.hasRegion()
        ? task.getRegionCode() + " " + task.getRegionName()
        : SearchTask.ALL_REGIONS;
    return "🆔 ID заявки: " + task.getId() + "\n"
        + "📅 Создана: " + DATE_FORMATTER.format(Instant.ofEpochMilli(task.getCreatedAt())) + "\n"
        + "📍 Регион: " + region + "\n"
        + "🎓 Мин. опыт: " + formatExp(task.getExperienceFrom()) + "\n"
        + "💰 Зарплата: " + formatSalary(task) + "\n"
        + "🔍 Ключевое слово: " + (task.hasKeyword() ? task.getKeyword() : "Любое");
  }

  public static String formatExp(int exp) {
    return switch (exp) {
      case 0 -> "Без опыта";
      case 1 -> "от 1 года";
      case 2 -> "от 2 лет";
      default -> "от 3 лет и более";
    };
  }

  public static String formatSalary(SearchTask task) {
    if (task.getSalaryMin() == 0 && task.getSalaryMax() == SearchTask.NO_SALARY_LIMIT) {
      return "не задана";
    }
    if (task.getSalaryMax() == SearchTask.NO_SALARY_LIMIT) {
      return "от " + task.getSalaryMin() + " руб.";
    }
    return task.getSalaryMin() + " – " + task.getSalaryMax() + " руб.";
  }

  public static String describe(SearchTask task) {
    String keyword = task.getKeyword() == null || task.getKeyword().isBlank()
        ? "все вакансии" : task.getKeyword();
    return keyword + ", " + task.getRegionName();
  }

  public static String regionPrompt() {
    return REGION_PROMPT;
  }

  public static String regionPageHeader(int page) {
    return "📍 Регионы — страница " + page + ". Выберите по кнопкам ниже:";
  }
}
