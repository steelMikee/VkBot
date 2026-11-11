package ru.github.vacancybot.bot;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.groups.responses.GetLongPollServerResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LongPollClient {

  private static final int CONNECT_TIMEOUT_MS = 30_000;
  private static final int READ_TIMEOUT_MS = 35_000;

  private final VkApiClient vk;
  private final GroupActor actor;
  private final long groupId;

  public LongPollSession openSession() throws ClientException, ApiException {
    GetLongPollServerResponse response = vk.groups().getLongPollServer(actor, groupId).execute();
    return new LongPollSession(String.valueOf(response.getServer()), response.getKey(),
        response.getTs());
  }

  public String check(LongPollSession session) throws IOException {
    HttpURLConnection conn =
        (HttpURLConnection) URI.create(session.checkUrl()).toURL().openConnection();
    conn.setRequestMethod("GET");
    conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
    conn.setReadTimeout(READ_TIMEOUT_MS);
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining());
    } finally {
      conn.disconnect();
    }
  }
}
