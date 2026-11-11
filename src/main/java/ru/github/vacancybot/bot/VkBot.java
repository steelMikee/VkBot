package ru.github.vacancybot.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.github.vacancybot.command.CommandDispatcher;
import ru.github.vacancybot.command.MessageParser;
import ru.github.vacancybot.dto.IncomingMessage;

@Slf4j
@RequiredArgsConstructor
public class VkBot {

  private static final String FAILED = "failed";
  private static final String TS = "ts";
  private static final String MESSAGE_NEW = "message_new";
  private static final long RETRY_DELAY_MS = 3_000;
  private static final long UNKNOWN_ERROR_DELAY_MS = 5_000;

  private final LongPollClient longPollClient;
  private final CommandDispatcher dispatcher;
  private final MessageParser messageParser;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public void start() throws ClientException, ApiException {
    LongPollSession session = longPollClient.openSession();
    log.info("The bot is running and listening for events...");
    runLoop(session);
  }

  private void runLoop(LongPollSession initial) {
    LongPollSession session = initial;
    while (!Thread.currentThread().isInterrupted()) {
      try {
        session = poll(session);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        log.error("Long Poll error, reconnecting after pause...", e);
        sleepQuietly(RETRY_DELAY_MS);
      }
    }
    log.info("Long Poll loop stopped.");
  }

  private LongPollSession poll(LongPollSession session)
      throws IOException, InterruptedException, ClientException, ApiException {
    JsonNode root = objectMapper.readTree(longPollClient.check(session));
    if (root.has(FAILED)) {
      return handleFailure(root, session);
    }
    processUpdates(root.path("updates"));
    return root.has(TS) ? session.withTs(root.get(TS).asText()) : session;
  }

  private LongPollSession handleFailure(JsonNode root, LongPollSession session)
      throws InterruptedException, ClientException, ApiException {
    int code = root.get(FAILED).asInt();
    return switch (code) {
      case 1 -> session.withTs(root.get(TS).asText());
      case 2, 3 -> {
        log.info("Reconnecting to Long Poll...");
        yield longPollClient.openSession();
      }
      default -> {
        log.error("Unknown Long Poll error: {}", code);
        Thread.sleep(UNKNOWN_ERROR_DELAY_MS);
        yield session;
      }
    };
  }

  private void processUpdates(JsonNode updates) {
    if (!updates.isArray()) {
      return;
    }
    for (JsonNode update : updates) {
      if (MESSAGE_NEW.equals(update.path("type").asText(""))) {
        handleMessage(update.path("object").path("message"));
      }
    }
  }

  private void handleMessage(JsonNode msg) {
    try {
      String text = msg.path("text").asText("");
      String payload = msg.hasNonNull("payload") ? msg.get("payload").asText() : null;
      IncomingMessage message = new IncomingMessage(
          msg.path("from_id").asLong(), text, messageParser.parse(text), payload);
      dispatcher.dispatch(message);
    } catch (RuntimeException e) {
      log.error("Error processing update: {}", e.getMessage(), e);
    }
  }

  private static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
