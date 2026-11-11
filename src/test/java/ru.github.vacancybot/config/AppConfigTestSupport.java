package ru.github.vacancybot.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.cdimascio.dotenv.Dotenv;

public final class AppConfigTestSupport {

  private AppConfigTestSupport() {}

  public static AppConfig config(String token, String groupId, String dataDir,
      String periodHours, String delayMinutes) {
    Dotenv dotenv = mock(Dotenv.class);
    when(dotenv.get("VK_ACCESS_TOKEN")).thenReturn(token);
    when(dotenv.get("VK_GROUP_ID")).thenReturn(groupId);
    when(dotenv.get("DATA_DIR", "data")).thenReturn(dataDir);
    when(dotenv.get("NOTIFICATION_PERIOD_HOURS", "24")).thenReturn(periodHours);
    when(dotenv.get("SCHEDULER_INITIAL_DELAY_MINUTES", "1")).thenReturn(delayMinutes);
    return new AppConfig(dotenv);
  }
}
