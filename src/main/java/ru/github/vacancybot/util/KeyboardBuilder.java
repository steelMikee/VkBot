package ru.github.vacancybot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.util.RegionDirectory.RegionPage;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyboardBuilder {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final int MAX_LABEL_LENGTH = 40;

  private static final String POSITIVE = "positive";
  private static final String PRIMARY = "primary";
  private static final String NEGATIVE = "negative";
  private static final String DEFAULT_COLOR = "default";

  private static final String SCREEN_1 = keyboard(
      row(btn(CommandType.DEFAULT.getLabel(), POSITIVE)));

  private static final String SCREEN_2 = keyboard(
      row(btn(CommandType.NEW_TASK.getLabel(), POSITIVE)),
      row(btn(CommandType.CURRENT_TASKS.getLabel(), PRIMARY)));

  private static final String EXPERIENCE_OPTIONS = keyboard(
      row(btn(ButtonLabels.NO_EXPERIENCE, DEFAULT_COLOR)),
      row(btn(ButtonLabels.ONE_YEAR, DEFAULT_COLOR)),
      row(btn(ButtonLabels.TWO_YEARS, DEFAULT_COLOR)),
      row(btn(ButtonLabels.THREE_YEARS_PLUS, DEFAULT_COLOR)),
      row(btn(ButtonLabels.ANY_EXPERIENCE, POSITIVE), btn(ButtonLabels.BACK_TO_TASK, NEGATIVE)));

  private static final String SALARY_OPTIONS = keyboard(
      row(btn(ButtonLabels.SALARY_50_70, DEFAULT_COLOR)),
      row(btn(ButtonLabels.SALARY_70_100, DEFAULT_COLOR)),
      row(btn(ButtonLabels.SALARY_100_PLUS, DEFAULT_COLOR)),
      row(btn(ButtonLabels.ANY_SALARY, POSITIVE), btn(ButtonLabels.BACK_TO_TASK, NEGATIVE)));

  public static String screen1() {
    return SCREEN_1;
  }

  public static String screen2() {
    return SCREEN_2;
  }

  public static String experienceOptions() {
    return EXPERIENCE_OPTIONS;
  }

  public static String salaryOptions() {
    return SALARY_OPTIONS;
  }

  public static String screen3(SearchTask task) {
    String region = "Регион: " + (task.hasRegion() ? String.valueOf(task.getRegionCode()) : "Любой");
    String exp = "Мин. опыт: " + formatExpShort(task.getExperienceFrom());
    String salary = "Зарплата: " + formatSalaryShort(task);
    String keyword = "Вакансия: " + (task.hasKeyword() ? "«" + task.getKeyword() + "»" : "Любая");
    return keyboard(
        row(btn(region, PRIMARY)),
        row(btn(exp, PRIMARY)),
        row(btn(salary, PRIMARY)),
        row(btn(keyword, PRIMARY)),
        row(btn(CommandType.DONE.getLabel(), POSITIVE), btn(ButtonLabels.ROOT_MENU, NEGATIVE)));
  }

  public static String regionPage(RegionPage page) {
    List<ArrayNode> rows = new ArrayList<>();
    page.regions().forEach(r -> rows.add(row(btn(r.code() + " " + r.name(), DEFAULT_COLOR))));
    rows.add(navigationRow(page));
    rows.add(row(btn(ButtonLabels.ANY_REGION, POSITIVE), btn(ButtonLabels.BACK_TO_TASK, NEGATIVE)));
    return keyboard(rows.toArray(ArrayNode[]::new));
  }

  private static ArrayNode navigationRow(RegionPage page) {
    int current = page.page();
    String next = "-> [" + (current + 1) + "]";
    String prev = "<- [" + (current - 1) + "]";
    if (current == 1) {
      return row(btn(next, PRIMARY));
    }
    if (page.hasNext()) {
      return row(btn(prev, PRIMARY), btn(next, PRIMARY));
    }
    return row(btn(prev, PRIMARY), btn(ButtonLabels.FIRST_PAGE, PRIMARY));
  }

  public static String taskInlineKeyboard(String taskId) {
    return inlineKeyboard(row(
        button("✏️ Изменить", PRIMARY, commandPayload("update_task", taskId)),
        button("❌ Удалить", NEGATIVE, commandPayload("delete_task", taskId))));
  }

  private static String commandPayload(String command, String taskId) {
    ObjectNode payload = MAPPER.createObjectNode();
    payload.put("command", command);
    payload.put("id", taskId);
    return payload.toString();
  }

  private static String keyboard(ArrayNode... rows) {
    return serialize(MAPPER.createObjectNode().put("one_time", false), rows);
  }

  private static String inlineKeyboard(ArrayNode... rows) {
    return serialize(MAPPER.createObjectNode().put("inline", true), rows);
  }

  private static String serialize(ObjectNode keyboard, ArrayNode... rows) {
    ArrayNode buttons = MAPPER.createArrayNode();
    for (ArrayNode row : rows) {
      buttons.add(row);
    }
    keyboard.set("buttons", buttons);
    try {
      return MAPPER.writeValueAsString(keyboard);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Keyboard serialization error", e);
    }
  }

  private static ArrayNode row(ObjectNode... buttons) {
    ArrayNode row = MAPPER.createArrayNode();
    for (ObjectNode button : buttons) {
      row.add(button);
    }
    return row;
  }

  private static ObjectNode btn(String label, String color) {
    String safeLabel = truncate(label);
    return button(safeLabel, color, "\"" + safeLabel.replace("\"", "") + "\"");
  }

  private static ObjectNode button(String label, String color, String jsonPayload) {
    ObjectNode action = MAPPER.createObjectNode();
    action.put("type", "text");
    action.put("label", truncate(label));
    action.put("payload", jsonPayload);
    ObjectNode button = MAPPER.createObjectNode();
    button.set("action", action);
    button.put("color", color);
    return button;
  }

  private static String truncate(String label) {
    return label.length() > MAX_LABEL_LENGTH ? label.substring(0, MAX_LABEL_LENGTH) : label;
  }

  private static String formatExpShort(int exp) {
    return switch (exp) {
      case 0 -> "Любой";
      case 1 -> "1 год";
      case 2 -> "2 года";
      default -> "3+ лет";
    };
  }

  private static String formatSalaryShort(SearchTask task) {
    if (task.getSalaryMin() == 0 && task.getSalaryMax() == SearchTask.NO_SALARY_LIMIT) {
      return "Любая";
    }
    if (task.getSalaryMax() == SearchTask.NO_SALARY_LIMIT) {
      return "от " + task.getSalaryMin();
    }
    return task.getSalaryMin() + "-" + task.getSalaryMax();
  }
}
