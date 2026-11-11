package ru.github.vacancybot.service.vacancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("VacancyParser — разбор JSON trudvsem")
class VacancyParserTest {

  private final VacancyParser parser = new VacancyParser();

  private static String wrap(String vacancyJson) {
    return "{\"results\":{\"vacancies\":[{\"vacancy\":" + vacancyJson + "}]}}";
  }

  @Nested
  class Total {

    @Test
    void readsTotal() {
      assertThat(parser.parseTotal("{\"meta\":{\"total\":42}}")).isEqualTo(42);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"<html>", "  <!doctype", "{broken", "{}", "   ", "[1,2,3]", "Service Unavailable"})
    @DisplayName("не-JSON, HTML, мусор, отсутствие meta, пустые строки, массивы, plain-text → 0")
    void invalidIsZero(String json) {
      assertThat(parser.parseTotal(json)).isZero();
    }
  }

  @Nested
  class ParsePage {

    @Test
    @DisplayName("возвращает total и вакансии за один парсинг")
    void returnsTotalAndVacancies() {
      String json = """
          {"meta":{"total":10},"results":{"vacancies":[{"vacancy":{
            "job-name":"Dev","vac_url":"http://v","company":{"name":"Corp"},
            "requirement":{"experience":"От 1 года"},"region_name":"Москва"}}]}}""";

      VacancyPage page = parser.parsePage(json);

      assertThat(page.total()).isEqualTo(10);
      assertThat(page.vacancies()).hasSize(1);
      assertThat(page.vacancies().get(0).jobName()).isEqualTo("Dev");
    }

    @Test
    @DisplayName("без meta.total → total = 0, вакансии парсятся")
    void noMetaReturnsZeroTotal() {
      String json = wrap("{\"job-name\":\"Test\"}");

      VacancyPage page = parser.parsePage(json);

      assertThat(page.total()).isZero();
      assertThat(page.vacancies()).hasSize(1);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"<html>", "  ", "[1,2]", "Error 502", "{}"})
    @DisplayName("не-JSON → пустая страница")
    void invalidReturnsEmpty(String json) {
      VacancyPage page = parser.parsePage(json);

      assertThat(page.total()).isZero();
      assertThat(page.vacancies()).isEmpty();
    }

    @Test
    @DisplayName("parseTotal и parse делегируют в parsePage")
    void delegatesToParsePage() {
      String json = """
          {"meta":{"total":5},"results":{"vacancies":[{"vacancy":{
            "job-name":"A","vac_url":"","company":{"name":""},
            "requirement":{"experience":""},"region_name":""}}]}}""";

      assertThat(parser.parseTotal(json)).isEqualTo(5);
      assertThat(parser.parse(json)).hasSize(1);
    }
  }

  @Nested
  class Items {

    @Test
    @DisplayName("базовые поля")
    void basicFields() {
      List<RawVacancy> list = parser.parse(wrap("""
          {"job-name":"Dev","vac_url":"http://v","company":{"name":"Corp"},
           "requirement":{"experience":"От 2 лет"},"region_name":"Тверская область"}"""));

      assertThat(list).hasSize(1);
      RawVacancy v = list.get(0);
      assertThat(v.jobName()).isEqualTo("Dev");
      assertThat(v.vacUrl()).isEqualTo("http://v");
      assertThat(v.employer()).isEqualTo("Corp");
      assertThat(v.region()).isEqualTo("Тверская область");
      assertThat(v.experienceYears()).isEqualTo(2);
    }

    @Test
    @DisplayName("регион: location из addresses приоритетнее region_name; fallback на lng_text")
    void regionPriority() {
      RawVacancy withLocation = parser.parse(wrap("""
          {"addresses":{"address":[{"location":"г Москва"}]},"region_name":"X"}""")).get(0);
      RawVacancy withLng = parser.parse(wrap("""
          {"addresses":{"address":[{"location":"","lng_text":"Казань"}]},"region_name":"X"}""")).get(0);
      RawVacancy emptyAddresses = parser.parse(wrap("""
          {"addresses":{"address":[]},"region_name":"X"}""")).get(0);

      assertThat(withLocation.region()).isEqualTo("г Москва");
      assertThat(withLng.region()).isEqualTo("Казань");
      assertThat(emptyAddresses.region()).isEqualTo("X");
    }

    @Test
    @DisplayName("зарплата: объект salary")
    void salaryObject() {
      RawVacancy v = parser.parse(wrap("{\"salary\":{\"salary_min\":100,\"salary_max\":\"200 руб\"}}")).get(0);

      assertThat(v.salaryMin()).isEqualTo(100);
      assertThat(v.salaryMax()).isEqualTo(200);
    }

    @Test
    @DisplayName("зарплата: поля на уровне вакансии")
    void salaryTopLevel() {
      RawVacancy v = parser.parse(wrap("{\"salary_min\":\"50000\",\"salary_max\":null}")).get(0);

      assertThat(v.salaryMin()).isEqualTo(50_000);
      assertThat(v.salaryMax()).isZero();
    }

    @Test
    @DisplayName("зарплата: текст «от 30 000 - 45 000»")
    void salaryTextRange() {
      RawVacancy v = parser.parse(wrap("{\"salary\":\"от 30 000 - 45 000\"}")).get(0);

      assertThat(v.salaryMin()).isEqualTo(30_000);
      assertThat(v.salaryMax()).isEqualTo(45_000);
    }

    @Test
    @DisplayName("зарплата: текст без диапазона")
    void salaryTextSingle() {
      RawVacancy v = parser.parse(wrap("{\"salary\":\"от 80 000 руб\"}")).get(0);

      assertThat(v.salaryMin()).isEqualTo(80_000);
      assertThat(v.salaryMax()).isZero();
    }

    @Test
    @DisplayName("отсутствующие поля → пустые строки и нули, без исключений")
    void missingFields() {
      RawVacancy v = parser.parse(wrap("{}")).get(0);

      assertThat(v.jobName()).isEmpty();
      assertThat(v.employer()).isEmpty();
      assertThat(v.region()).isEmpty();
      assertThat(v.salaryMin()).isZero();
      assertThat(v.experienceYears()).isZero();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"<html>", "{broken", "{\"results\":{\"vacancies\":{}}}", "{}"})
    @DisplayName("не-JSON / HTML / не-массив → пустой список")
    void invalidIsEmpty(String json) {
      assertThat(parser.parse(json)).isEmpty();
    }
  }

  @Nested
  class Experience {

    @ParameterizedTest(name = "«{0}» → {1}")
    @CsvSource({
        "'', 0",
        "без опыта, 0",
        "не имеет значения, 0",
        "от 1 года, 1",
        "1 год, 1",
        "от 2 лет, 2",
        "2 года, 2",
        "от 3 лет, 3",
        "3 года, 3",
        "более 5 лет, 3",
        "5, 5",
        "опыт не требуется, 0"})
    void parsesExperience(String text, int expected) {
      RawVacancy v = parser.parse(wrap("{\"requirement\":{\"experience\":\"" + text + "\"}}")).get(0);

      assertThat(v.experienceYears()).isEqualTo(expected);
    }
  }
}
