package ru.github.vacancybot.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import ru.github.vacancybot.exception.VacancyApiException;
import ru.github.vacancybot.model.SearchTask;

public class TrudvsemApiClient implements VacancyApiClient {

  public static final String DEFAULT_BASE_URL = "https://opendata.trudvsem.ru/api/v1/vacancies";
  private static final int HTTP_OK = 200;

  private final String baseUrl;
  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .build();

  public TrudvsemApiClient() {
    this(DEFAULT_BASE_URL);
  }

  public TrudvsemApiClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public String fetchPage(SearchTask task, int offset) {
    return get(buildUrl(task, offset));
  }

  @Override
  public String fetchRegion(int regionCode) {
    return get(baseUrl + "/region/" + regionCode + "?offset=0&limit=" + PAGE_LIMIT);
  }

  private String buildUrl(SearchTask task, int offset) {
    StringBuilder sb = new StringBuilder(baseUrl);
    if (task.hasRegion()) {
      sb.append("/region/").append(String.format("%02d", task.getRegionCode()));
    }
    sb.append("?offset=").append(offset).append("&limit=").append(PAGE_LIMIT);
    if (task.getExperienceFrom() > 0) {
      sb.append("&experienceFrom=").append(task.getExperienceFrom());
    }
    if (task.hasKeyword()) {
      sb.append("&text=").append(URLEncoder.encode(task.getKeyword(), StandardCharsets.UTF_8));
    }
    return sb.toString();
  }

  private String get(String url) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .timeout(Duration.ofSeconds(30))
        .header("Accept", "application/json")
        .build();
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != HTTP_OK) {
        throw new VacancyApiException("API status " + response.statusCode() + " for URL: " + url);
      }
      return response.body();
    } catch (IOException e) {
      throw new VacancyApiException("HTTP error: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new VacancyApiException("HTTP request interrupted", e);
    }
  }
}
