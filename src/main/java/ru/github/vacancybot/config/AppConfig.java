package ru.github.vacancybot.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;

@Getter
public class AppConfig {

  private static final String DEFAULT_DATA_DIR = "data";
  private static final String DEFAULT_PERIOD_HOURS = "24";
  private static final String DEFAULT_INITIAL_DELAY_MINUTES = "1";

  private final String vkGroupToken;
  private final long vkGroupId;
  private final String dataDir;
  private final long notificationPeriodHours;
  private final long schedulerInitialDelayMinutes;

  public AppConfig() {
    this(Dotenv.configure().ignoreIfMissing().load());
  }

  AppConfig(Dotenv dotenv) {
    this.vkGroupToken = requireEnv(dotenv, "VK_ACCESS_TOKEN");
    this.vkGroupId = parseLong("VK_GROUP_ID", requireEnv(dotenv, "VK_GROUP_ID"));
    this.dataDir = dotenv.get("DATA_DIR", DEFAULT_DATA_DIR);
    this.notificationPeriodHours = parseLong("NOTIFICATION_PERIOD_HOURS",
        dotenv.get("NOTIFICATION_PERIOD_HOURS", DEFAULT_PERIOD_HOURS));
    this.schedulerInitialDelayMinutes = parseLong("SCHEDULER_INITIAL_DELAY_MINUTES",
        dotenv.get("SCHEDULER_INITIAL_DELAY_MINUTES", DEFAULT_INITIAL_DELAY_MINUTES));
  }

  private static String requireEnv(Dotenv dotenv, String key) {
    String value = dotenv.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Переменная окружения '" + key + "' не задана в .env");
    }
    return value;
  }

  private static long parseLong(String key, String value) {
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Переменная окружения '" + key + "' должна быть числом, получено: " + value, e);
    }
  }
}
