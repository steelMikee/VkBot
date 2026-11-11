package ru.github.vacancybot.service.vacancy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VacancyParser {

  private static final Pattern NON_DIGITS = Pattern.compile("[^0-9]");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public VacancyPage parsePage(String json) {
    if (isNotJsonObject(json)) {
      return VacancyPage.empty();
    }
    try {
      JsonNode root = MAPPER.readTree(json);
      int total = root.path("meta").path("total").asInt(0);
      List<RawVacancy> vacancies = new ArrayList<>();
      JsonNode vacanciesNode = root.path("results").path("vacancies");
      if (vacanciesNode.isArray()) {
        for (JsonNode item : vacanciesNode) {
          vacancies.add(parseItem(item.path("vacancy")));
        }
      }
      return new VacancyPage(total, vacancies);
    } catch (JsonProcessingException e) {
      log.error("Parsing error: {}", e.getMessage());
      return VacancyPage.empty();
    }
  }

  public int parseTotal(String json) {
    return parsePage(json).total();
  }

  public List<RawVacancy> parse(String json) {
    return parsePage(json).vacancies();
  }

  private static boolean isNotJsonObject(String json) {
    if (json == null) {
      return true;
    }
    int len = json.length();
    for (int i = 0; i < len; i++) {
      char c = json.charAt(i);
      if (!Character.isWhitespace(c)) {
        return c != '{';
      }
    }
    return true;
  }

  private static RawVacancy parseItem(JsonNode vac) {
    Salary salary = parseSalary(vac);
    String experienceText = vac.path("requirement").path("experience").asText("")
        .toLowerCase(Locale.ROOT);
    return new RawVacancy(
        vac.path("job-name").asText(""),
        vac.path("vac_url").asText(""),
        vac.path("company").path("name").asText(""),
        parseRegion(vac),
        salary.min(),
        salary.max(),
        parseExperience(experienceText));
  }

  private static String parseRegion(JsonNode vac) {
    JsonNode addresses = vac.path("addresses").path("address");
    if (!addresses.isArray() || addresses.isEmpty()) {
      return vac.path("region_name").asText("");
    }
    String location = addresses.path(0).path("location").asText("");
    return location.isBlank() ? addresses.path(0).path("lng_text").asText("") : location;
  }

  private static Salary parseSalary(JsonNode vac) {
    final String SALARY = "salary";
    final String SALARY_MIN = "salary_min";
    final String SALARY_MAX = "salary_max";
    int min = 0;
    int max = 0;
    JsonNode salaryNode = vac.path(SALARY);
    if (!salaryNode.isMissingNode() && !salaryNode.isNull()) {
      min = intField(salaryNode, SALARY_MIN);
      max = intField(salaryNode, SALARY_MAX);
    }
    if (min == 0 && max == 0) {
      min = intField(vac, SALARY_MIN);
      max = intField(vac, SALARY_MAX);
    }
    if (min == 0 && max == 0 && vac.has(SALARY)) {
      return parseTextSalary(vac.get(SALARY).asText(""));
    }
    return new Salary(min, max);
  }

  private static Salary parseTextSalary(String raw) {
    String text = WHITESPACE.matcher(raw).replaceAll("");
    if (!text.contains("-")) {
      return new Salary(digitsToInt(text), 0);
    }
    String[] parts = text.split("-");
    int min = parts.length > 0 ? digitsToInt(parts[0]) : 0;
    int max = parts.length > 1 ? digitsToInt(parts[1]) : 0;
    return new Salary(min, max);
  }

  private static int intField(JsonNode parent, String field) {
    JsonNode node = parent.path(field);
    if (node.isMissingNode() || node.isNull()) {
      return 0;
    }
    return node.isNumber() ? node.asInt(0) : digitsToInt(node.asText("0"));
  }

  private static int digitsToInt(String text) {
    if (text == null) {
      return 0;
    }
    String digits = NON_DIGITS.matcher(text).replaceAll("");
    if (digits.isEmpty()) {
      return 0;
    }
    try {
      return Integer.parseInt(digits);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static int parseExperience(String expText) {
    if (expText.isEmpty() || expText.contains("без") || expText.contains("не имеет значения")) {
      return 0;
    }
    if (expText.contains("1 год") || expText.contains("от 1")) {
      return 1;
    }
    if (expText.contains("2 года") || expText.contains("от 2")) {
      return 2;
    }
    if (expText.contains("3 года") || expText.contains("от 3") || expText.contains("более")) {
      return 3;
    }
    return digitsToInt(expText);
  }

  private record Salary(int min, int max) {}
}
