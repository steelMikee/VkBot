package ru.github.vacancybot.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.exception.VacancyApiException;
import ru.github.vacancybot.model.SearchTask;

@Tag("integration")
@WireMockTest
@DisplayName("TrudvsemApiClient — HTTP через WireMock")
class TrudvsemApiClientTest {

  private static SearchTask task(int region, int exp, String keyword) {
    return SearchTask.builder().regionCode(region).experienceFrom(exp).keyword(keyword)
        .salaryMax(-1).build();
  }

  @Test
  @DisplayName("fetchPage — регион с ведущим нулём, опыт и закодированный текст")
  void buildsFullUrl(WireMockRuntimeInfo wm) {
    wm.getWireMock().register(get(urlPathEqualTo("/api/region/05"))
        .willReturn(aResponse().withBody("{}")));
    TrudvsemApiClient client = new TrudvsemApiClient(wm.getHttpBaseUrl() + "/api");

    String body = client.fetchPage(task(5, 2, "java разработчик"), 100);

    assertThat(body).isEqualTo("{}");
    wm.getWireMock().verifyThat(getRequestedFor(urlPathEqualTo("/api/region/05"))
        .withQueryParam("offset", equalTo("100"))
        .withQueryParam("limit", equalTo("100"))
        .withQueryParam("experienceFrom", equalTo("2"))
        .withQueryParam("text", equalTo("java разработчик"))
        .withHeader("Accept", equalTo("application/json")));
  }

  @Test
  @DisplayName("fetchPage — без региона, опыта и ключевого слова «Любая» параметры не добавляются")
  void buildsMinimalUrl(WireMockRuntimeInfo wm) {
    wm.getWireMock().register(get(urlPathEqualTo("/api")).willReturn(aResponse().withBody("{}")));
    TrudvsemApiClient client = new TrudvsemApiClient(wm.getHttpBaseUrl() + "/api");

    client.fetchPage(task(0, 0, "Любая"), 0);

    wm.getWireMock().verifyThat(getRequestedFor(urlPathEqualTo("/api"))
        .withQueryParam("offset", equalTo("0"))
        .withQueryParam("limit", equalTo("100"))
        .withoutQueryParam("experienceFrom")
        .withoutQueryParam("text"));
  }

  @Test
  @DisplayName("fetchRegion — код региона без ведущего нуля (поведение широкого поиска)")
  void fetchRegionUrl(WireMockRuntimeInfo wm) {
    wm.getWireMock().register(get(urlPathEqualTo("/api/region/7"))
        .willReturn(aResponse().withBody("ok")));
    TrudvsemApiClient client = new TrudvsemApiClient(wm.getHttpBaseUrl() + "/api");

    assertThat(client.fetchRegion(7)).isEqualTo("ok");
  }

  @Test
  @DisplayName("статус != 200 → VacancyApiException с кодом")
  void nonOkStatus(WireMockRuntimeInfo wm) {
    wm.getWireMock().register(get(urlPathEqualTo("/api/region/1"))
        .willReturn(aResponse().withStatus(503)));
    TrudvsemApiClient client = new TrudvsemApiClient(wm.getHttpBaseUrl() + "/api");

    assertThatThrownBy(() -> client.fetchRegion(1))
        .isInstanceOf(VacancyApiException.class)
        .hasMessageContaining("503");
  }

  @Test
  @DisplayName("сетевая ошибка → VacancyApiException, флаг прерывания НЕ выставляется")
  void ioErrorDoesNotInterrupt() {
    TrudvsemApiClient client = new TrudvsemApiClient("http://nonexistent.invalid");

    assertThatThrownBy(() -> client.fetchRegion(1)).isInstanceOf(VacancyApiException.class);
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }
}
