package ru.github.vacancybot.bot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.objects.groups.responses.GetLongPollServerResponse;
import com.vk.api.sdk.queries.groups.GroupsGetLongPollServerQuery;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("integration")
@WireMockTest
@DisplayName("LongPollClient — сеть через WireMock")
class LongPollClientTest {

  private final VkApiClient vk = mock(VkApiClient.class, Mockito.RETURNS_DEEP_STUBS);
  private final GroupActor actor = new GroupActor(1L, "test_token");

  @Test
  @DisplayName("check() читает тело ответа целиком (многострочный JSON склеивается)")
  void checkReadsBody(WireMockRuntimeInfo wm) throws IOException {
    wm.getWireMock().register(get(urlPathEqualTo("/lp"))
        .willReturn(aResponse().withBody("{\"ts\":\"5\",\n\"updates\":[]}")));
    LongPollSession session = new LongPollSession(wm.getHttpBaseUrl() + "/lp", "k", "1");

    String body = new LongPollClient(vk, actor, 1L).check(session);

    assertThat(body).isEqualTo("{\"ts\":\"5\",\"updates\":[]}");
  }

  @Test
  @DisplayName("HTTP 500 — IOException (цикл бота переподключится)")
  void serverErrorThrowsIoException(WireMockRuntimeInfo wm) {
    wm.getWireMock().register(get(urlPathEqualTo("/lp"))
        .willReturn(aResponse().withStatus(500)));
    LongPollSession session = new LongPollSession(wm.getHttpBaseUrl() + "/lp", "k", "1");

    assertThatThrownBy(() -> new LongPollClient(vk, actor, 1L).check(session))
        .isInstanceOf(IOException.class);
  }

  @Test
  @DisplayName("openSession() маппит ответ SDK в LongPollSession")
  void openSessionMapsResponse() throws Exception {
    GetLongPollServerResponse response = new GetLongPollServerResponse();
    response.setServer(java.net.URI.create("https://lp.vk.com/x"));
    response.setKey("key");
    response.setTs("42");
    GroupsGetLongPollServerQuery query = mock(GroupsGetLongPollServerQuery.class);
    when(vk.groups().getLongPollServer(actor, 1L)).thenReturn(query);
    when(query.execute()).thenReturn(response);

    LongPollSession session = new LongPollClient(vk, actor, 1L).openSession();

    assertThat(session.server()).isEqualTo("https://lp.vk.com/x");
    assertThat(session.key()).isEqualTo("key");
    assertThat(session.ts()).isEqualTo("42");
  }
}
