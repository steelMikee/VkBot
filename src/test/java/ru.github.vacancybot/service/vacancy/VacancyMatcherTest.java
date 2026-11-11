package ru.github.vacancybot.service.vacancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;

@DisplayName("VacancyMatcher — строгая фильтрация")
class VacancyMatcherTest {

  private static SearchTask.SearchTaskBuilder task() {
    return SearchTask.builder().keyword("").regionName(SearchTask.ALL_REGIONS).salaryMax(-1);
  }

  private static RawVacancy raw(String job, String region, int min, int max, int exp) {
    return new RawVacancy(job, "u", "e", region, min, max, exp);
  }

  @Nested
  class ExperienceRule {

    @Test
    void requiredExperience() {
      VacancyMatcher m = new VacancyMatcher(task().experienceFrom(2).build());

      assertThat(m.matches(raw("j", "r", 0, 0, 1))).isFalse();
      assertThat(m.matches(raw("j", "r", 0, 0, 2))).isTrue();
      assertThat(m.matches(raw("j", "r", 0, 0, 5))).isTrue();
    }

    @Test
    void noRequirement() {
      assertThat(new VacancyMatcher(task().build()).matches(raw("j", "r", 0, 0, 0))).isTrue();
    }
  }

  @Nested
  class SalaryRule {

    @Test
    @DisplayName("минимум: вакансия с min ниже требуемого отсекается")
    void minBelowRequired() {
      VacancyMatcher m = new VacancyMatcher(task().salaryMin(100).build());

      assertThat(m.matches(raw("j", "r", 90, 0, 0))).isFalse();
      assertThat(m.matches(raw("j", "r", 100, 0, 0))).isTrue();
    }

    @Test
    @DisplayName("минимум: если min=0, сравнивается max")
    void maxUsedWhenMinZero() {
      VacancyMatcher m = new VacancyMatcher(task().salaryMin(100).build());

      assertThat(m.matches(raw("j", "r", 0, 90, 0))).isFalse();
      assertThat(m.matches(raw("j", "r", 0, 150, 0))).isTrue();
    }

    @Test
    @DisplayName("без зарплаты + требуемый минимум: через select подставляется минимум и проходит")
    void noSalaryGetsFloorViaSelect() {
      VacancyMatcher m = new VacancyMatcher(task().salaryMin(100).build());

      List<Vacancy> result = m.select(List.of(raw("j", "r", 0, 0, 0)));

      assertThat(result).hasSize(1);
      assertThat(result.get(0).salaryMin()).isEqualTo(100);
    }

    @Test
    @DisplayName("потолок: min вакансии выше salaryMax заявки — отсекается")
    void ceiling() {
      VacancyMatcher m = new VacancyMatcher(task().salaryMin(50).salaryMax(100).build());

      assertThat(m.matches(raw("j", "r", 101, 0, 0))).isFalse();
      assertThat(m.matches(raw("j", "r", 100, 0, 0))).isTrue();
    }

    @Test
    @DisplayName("salaryMax = -1 — потолка нет")
    void noCeiling() {
      VacancyMatcher m = new VacancyMatcher(task().salaryMin(50).build());

      assertThat(m.matches(raw("j", "r", 1_000_000, 0, 0))).isTrue();
    }
  }

  @Nested
  class RegionRule {

    @Test
    @DisplayName("название региона без «область/край/…» ищется в адресе без учёта регистра")
    void regionSubstring() {
      VacancyMatcher m = new VacancyMatcher(task().regionCode(69).regionName("Тверская область").build());

      assertThat(m.matches(raw("j", "г Тверь, ТВЕРСКАЯ обл", 0, 0, 0))).isTrue();
      assertThat(m.matches(raw("j", "г Москва", 0, 0, 0))).isFalse();
    }

    @Test
    @DisplayName("республика: «Республика Татарстан» → ищем «татарстан»")
    void republic() {
      VacancyMatcher m = new VacancyMatcher(task().regionCode(16).regionName("Республика Татарстан").build());

      assertThat(m.matches(raw("j", "Татарстан, Казань", 0, 0, 0))).isTrue();
    }

    @Test
    void noRegion() {
      assertThat(new VacancyMatcher(task().build()).matches(raw("j", "anywhere", 0, 0, 0))).isTrue();
    }
  }

  @Nested
  class KeywordRule {

    @Test
    void keywordSubstringCaseInsensitive() {
      VacancyMatcher m = new VacancyMatcher(task().keyword(" JAVA ").build());

      assertThat(m.matches(raw("Senior Java Developer", "r", 0, 0, 0))).isTrue();
      assertThat(m.matches(raw("Python dev", "r", 0, 0, 0))).isFalse();
    }

    @Test
    @DisplayName("«Любая» — фильтр не применяется")
    void anyKeyword() {
      assertThat(new VacancyMatcher(task().keyword("Любая").build()).matches(raw("x", "r", 0, 0, 0))).isTrue();
    }
  }

  @Test
  @DisplayName("select — сохраняет порядок и конвертирует в Vacancy")
  void selectKeepsOrder() {
    VacancyMatcher m = new VacancyMatcher(task().keyword("a").build());

    List<Vacancy> result = m.select(List.of(
        raw("a1", "r", 1, 0, 0), raw("b", "r", 2, 0, 0), raw("a2", "r", 3, 0, 0)));

    assertThat(result).extracting(Vacancy::jobName).containsExactly("a1", "a2");
  }
}
