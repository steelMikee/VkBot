package ru.github.vacancybot.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SearchTask")
class SearchTaskTest {

  private static SearchTask base() {
    return SearchTask.builder().id("1").salaryMax(SearchTask.NO_SALARY_LIMIT).keyword("").build();
  }

  @Test
  void hasRegion() {
    assertThat(base().hasRegion()).isFalse();
    SearchTask t = base();
    t.setRegion(77, "Москва");
    assertThat(t.hasRegion()).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  ", "Любая", "любая"})
  @DisplayName("hasKeyword — false для пустых и служебного «Любая»")
  void hasKeywordFalse(String kw) {
    SearchTask t = base();
    t.setKeyword(kw);
    assertThat(t.hasKeyword()).isFalse();
  }

  @Test
  void hasKeywordTrue() {
    SearchTask t = base();
    t.setKeyword("java");
    assertThat(t.hasKeyword()).isTrue();
  }

  @Test
  void hasSalaryCeiling() {
    assertThat(base().hasSalaryCeiling()).isFalse();
    SearchTask t = base();
    t.setSalaryRange(1, 2);
    assertThat(t.hasSalaryCeiling()).isTrue();
  }

  @Test
  @DisplayName("isBroadSearch — нет региона и ключевого слова")
  void broadSearch() {
    assertThat(base().isBroadSearch()).isTrue();
    SearchTask withRegion = base();
    withRegion.setRegion(1, "x");
    assertThat(withRegion.isBroadSearch()).isFalse();
    SearchTask withKeyword = base();
    withKeyword.setKeyword("x");
    assertThat(withKeyword.isBroadSearch()).isFalse();
  }

  @Test
  @DisplayName("copyFiltersFrom — переносит фильтры, но не id/владельца/дату")
  void copyFilters() {
    SearchTask src = SearchTask.builder().id("src").ownerId(9).regionCode(5).regionName("R")
        .experienceFrom(3).salaryMin(10).salaryMax(20).keyword("k").createdAt(100).build();
    SearchTask dst = SearchTask.builder().id("dst").ownerId(1).createdAt(1).build();

    dst.copyFiltersFrom(src);

    assertThat(dst.getId()).isEqualTo("dst");
    assertThat(dst.getOwnerId()).isEqualTo(1);
    assertThat(dst.getCreatedAt()).isEqualTo(1);
    assertThat(dst.getRegionCode()).isEqualTo(5);
    assertThat(dst.getRegionName()).isEqualTo("R");
    assertThat(dst.getExperienceFrom()).isEqualTo(3);
    assertThat(dst.getSalaryMin()).isEqualTo(10);
    assertThat(dst.getSalaryMax()).isEqualTo(20);
    assertThat(dst.getKeyword()).isEqualTo("k");
  }

  @Test
  @DisplayName("вычисляемые свойства не попадают в JSON")
  void computedPropertiesIgnoredInJson() throws Exception {
    String json = new ObjectMapper().writeValueAsString(base());

    assertThat(json).doesNotContain("hasRegion", "hasKeyword", "broadSearch", "salaryCeiling");
  }

  @Test
  @DisplayName("toBuilder создаёт независимую копию")
  void toBuilderCopy() {
    SearchTask original = base();
    SearchTask copy = original.toBuilder().build();
    copy.setKeyword("changed");

    assertThat(original.getKeyword()).isEmpty();
  }
}
