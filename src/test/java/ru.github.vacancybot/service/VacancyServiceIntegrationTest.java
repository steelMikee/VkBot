package ru.github.vacancybot.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.github.vacancybot.client.TrudvsemApiClient;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;
import ru.github.vacancybot.repository.JsonVacancyRepository;
import ru.github.vacancybot.service.vacancy.VacancyParser;

@Tag("integration")
@WireMockTest
@DisplayName("VacancyService — интеграция клиент+парсер+фильтр+хранилище")
class VacancyServiceIntegrationTest {

  private static final String PAGE = """
      {"status":"200","meta":{"total":3},"results":{"vacancies":[
        {"vacancy":{"job-name":"Java разработчик","vac_url":"http://v/1",
          "salary_min":150000,"salary_max":200000,"requirement":{"experience":"от 3 лет"},
          "addresses":{"address":[{"location":"г Москва, ул Ленина"}]}}},
        {"vacancy":{"job-name":"Java junior","vac_url":"http://v/2",
          "salary_min":60000,"salary_max":0,"requirement":{"experience":"без опыта"},
          "region_name":"Москва"}},
        {"vacancy":{"job-name":"Повар","vac_url":"http://v/3",
          "salary_min":300000,"salary_max":0,"requirement":{"experience":"без опыта"},
          "region_name":"Москва"}}
      ]}}""";

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("ключевое слово + регион + мин. зарплата: находит, фильтрует, сортирует, сохраняет")
  void endToEnd(WireMockRuntimeInfo wm) throws Exception {
    wm.getWireMock().register(get(urlPathEqualTo("/api/region/77"))
        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(PAGE)));
    JsonVacancyRepository repository = new JsonVacancyRepository(tempDir.toString());
    VacancyServiceImpl service = new VacancyServiceImpl(
        new TrudvsemApiClient(wm.getHttpBaseUrl() + "/api"), new VacancyParser(), repository);
    SearchTask task = SearchTask.builder().id("task-1").regionCode(77).regionName("Москва")
        .keyword("java").salaryMin(50_000).salaryMax(-1).build();

    List<Vacancy> result = service.fetchAndSave(task);

    assertThat(result).extracting(Vacancy::jobName)
        .containsExactly("Java разработчик", "Java junior");
    assertThat(result.get(0).region()).isEqualTo("г Москва, ул Ленина");
    List<Vacancy> persisted = new ObjectMapper().readValue(
        tempDir.resolve("vacancies/task-1.json").toFile(), new TypeReference<>() {});
    assertThat(persisted).isEqualTo(result);
  }
}
