package ru.github.vacancybot;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.github.vacancybot.bootstrap.BotAssembler;
import ru.github.vacancybot.config.AppConfig;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Main {

  public static void main(String[] args) throws ClientException, ApiException {
    log.info("=== VK Vacancy Bot starting ===");
    AppConfig config = new AppConfig();
    log.info("Configuration loaded. VK group ID: {}", config.getVkGroupId());
    new BotAssembler(config).assemble().start();
  }
}
