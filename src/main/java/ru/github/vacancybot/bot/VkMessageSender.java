package ru.github.vacancybot.bot;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Keyboard;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class VkMessageSender {

  public static final int MAX_MESSAGE_LENGTH = 4000;

  private final VkApiClient vk;
  private final GroupActor actor;

  public void sendText(long userId, String text) {
    send(userId, text, null);
  }

  public void sendWithKeyboard(long userId, String text, String keyboardJson) {
    send(userId, text, vk.getGson().fromJson(keyboardJson, Keyboard.class));
  }

  public void sendChunked(long userId, String header, List<String> entries) {
    StringBuilder sb = new StringBuilder(header);
    for (String entry : entries) {
      if (sb.length() + entry.length() > MAX_MESSAGE_LENGTH) {
        sendText(userId, sb.toString());
        sb.setLength(0);
      }
      sb.append(entry);
    }
    if (!sb.isEmpty()) {
      sendText(userId, sb.toString());
    }
  }

  private void send(long userId, String text, Keyboard keyboard) {
    try {
      var query = vk.messages()
          .sendDeprecated(actor)
          .userId(userId)
          .message(text)
          .randomId(ThreadLocalRandom.current().nextInt());
      if (keyboard != null) {
        query.keyboard(keyboard);
      }
      query.execute();
    } catch (ApiException | ClientException e) {
      log.error("Error sending message to user {}: {}", userId, e.getMessage());
    }
  }
}
