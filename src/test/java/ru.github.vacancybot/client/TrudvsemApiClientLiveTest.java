package ru.github.vacancybot.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.model.SearchTask;

@Tag("live-network")
@DisplayName("TrudvsemApiClient — реальное API")
class TrudvsemApiClientLiveTest {

  private final TrudvsemApiClient client = new TrudvsemApiClient();
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  @DisplayName("регион 77 (Москва) возвращает JSON с массивом вакансий и meta.total > 0")
  void moscowRegionReturnsVacancies() throws Exception {
    JsonNode root = mapper.readTree(client.fetchRegion(77));

    assertThat(root.path("status").asText()).isEqualTo("200");
    assertThat(root.path("meta").path("total").asInt()).isPositive();
    assertThat(root.path("results").path("vacancies").isArray()).isTrue();
  }

  @Test
  @DisplayName("страница с текстовым фильтром отвечает валидным JSON")
  void pageWithKeywordIsValidJson() throws Exception {
    SearchTask task = SearchTask.builder().regionCode(78).keyword("водитель").salaryMax(-1).build();

    JsonNode root = mapper.readTree(client.fetchPage(task, 0));

    assertThat(root.path("status").asText()).isEqualTo("200");
    assertThat(root.path("results").isObject() || root.path("results").isMissingNode()).isTrue();
  }

  @Test
  @DisplayName("контракт ответа: у вакансии есть job-name и vac_url")
  void vacancyContract() throws Exception {
    JsonNode vacancies = mapper.readTree(client.fetchRegion(77))
        .path("results").path("vacancies");

    assertThat(vacancies.size()).isPositive();
    JsonNode first = vacancies.get(0).path("vacancy");
    assertThat(first.path("job-name").asText()).isNotBlank();
    assertThat(first.path("vac_url").asText()).startsWith("http");
  }
}
