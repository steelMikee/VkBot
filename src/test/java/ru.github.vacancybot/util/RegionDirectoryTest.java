package ru.github.vacancybot.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.github.vacancybot.util.RegionDirectory.Region;
import ru.github.vacancybot.util.RegionDirectory.RegionPage;

@DisplayName("RegionDirectory")
class RegionDirectoryTest {

  @Test
  @DisplayName("справочник: 89 регионов, уникальные коды, отсортирован по коду")
  void directoryIntegrity() {
    assertThat(RegionDirectory.getAll()).hasSize(89);
    assertThat(RegionDirectory.getAll()).extracting(Region::code).doesNotHaveDuplicates().isSorted();
  }

  @Test
  void findByCode() {
    assertThat(RegionDirectory.findByCode(77)).contains(new Region(77, "Москва"));
    assertThat(RegionDirectory.findByCode(80)).isEmpty();
    assertThat(RegionDirectory.findByCode(-1)).isEmpty();
  }

  @Nested
  class Paging {

    @Test
    void firstPage() {
      RegionPage p = RegionDirectory.page(1);

      assertThat(p.page()).isEqualTo(1);
      assertThat(p.regions()).extracting(Region::code).containsExactly(1, 2, 3);
      assertThat(p.hasNext()).isTrue();
    }

    @Test
    void lastPage() {
      RegionPage p = RegionDirectory.page(30);

      assertThat(p.regions()).extracting(Region::code).containsExactly(94, 95);
      assertThat(p.hasNext()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, 31, 999})
    @DisplayName("вне диапазона → первая страница")
    void outOfRange(int page) {
      RegionPage p = RegionDirectory.page(page);

      assertThat(p.page()).isEqualTo(1);
      assertThat(p.regions()).extracting(Region::code).containsExactly(1, 2, 3);
    }
  }

  @Nested
  class Resolve {

    @ParameterizedTest(name = "«{0}» → {1}")
    @CsvSource({
        "77, 77",
        "  78  , 78",
        "Москва, 77",
        "москва, 77",
        "77 Москва, 77",
        "Тверская, 69",
        "тверская область, 69",
        "Татарстан, 16",
        "Санкт-Петербург, 78",
        "Ханты-Мансийский, 86",
        "Алтай, 4",
        "алтайский, 22",
        "Алтайский край, 22",
        "50 Московская, 50",
        "93, 93",
        "Донецкая, 93",
        "94, 94",
        "Луганская, 94",
        "95, 95",
        "Херсонская, 95"})
    void resolves(String input, int expectedCode) {
      assertThat(RegionDirectory.resolve(input)).map(Region::code).contains(expectedCode);
    }

    @Test
    @DisplayName("код + название другого региона: приоритет у названия")
    void nameWinsOverCode() {
      assertThat(RegionDirectory.resolve("77 Тверская")).map(Region::code).contains(69);
    }

    @Test
    @DisplayName("код + неизвестный текст: остаётся регион по коду")
    void unknownTextFallsBackToCode() {
      assertThat(RegionDirectory.resolve("77 абвгд")).map(Region::code).contains(77);
    }

    @ParameterizedTest
    @ValueSource(strings = {"999", "Атлантида", "ab", "99999999999", "99 Нарния"})
    @DisplayName("несуществующий код / название / слишком короткий / переполнение → пусто")
    void unresolved(String input) {
      assertThat(RegionDirectory.resolve(input)).isEmpty();
    }
  }

  @Test
  @DisplayName("findBySmartText — ввод короче 2 символов игнорируется")
  void tooShort() {
    assertThat(RegionDirectory.findBySmartText("М")).isEmpty();
  }
}
