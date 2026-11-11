package ru.github.vacancybot.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("EditingRef")
class EditingRefTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  @DisplayName("пустая ссылка → Optional.empty")
  void blankIsEmpty(String s) {
    assertThat(EditingRef.parse(s)).isEmpty();
  }

  @Test
  @DisplayName("одиночный id — создание новой заявки")
  void singleId() {
    assertThat(EditingRef.parse("abc")).contains(new EditingRef("abc", null));
    assertThat(EditingRef.parse("abc").orElseThrow().isUpdate()).isFalse();
  }

  @Test
  @DisplayName("композитный id — обновление существующей")
  void compositeId() {
    EditingRef ref = EditingRef.parse("draft:orig").orElseThrow();

    assertThat(ref.draftId()).isEqualTo("draft");
    assertThat(ref.originalId()).isEqualTo("orig");
    assertThat(ref.isUpdate()).isTrue();
  }

  @Test
  @DisplayName("compose ↔ parse симметричны")
  void roundTrip() {
    String composed = EditingRef.compose("a", "b");

    assertThat(composed).isEqualTo("a:b");
    assertThat(EditingRef.parse(composed)).contains(new EditingRef("a", "b"));
  }
}
