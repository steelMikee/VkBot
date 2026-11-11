package ru.github.vacancybot.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.util.RegionDirectory.Region;
import ru.github.vacancybot.util.RegionDirectory.RegionPage;

@DisplayName("KeyboardBuilder — JSON клавиатур VK")
class KeyboardBuilderTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private JsonNode read(String json) throws Exception {
    return mapper.readTree(json);
  }

  private static String label(JsonNode button) {
    return button.path("action").path("label").asText();
  }

  @Test
  @DisplayName("screen1 — одна кнопка «Начать», one_time=false")
  void screen1() throws Exception {
    JsonNode kb = read(KeyboardBuilder.screen1());

    assertThat(kb.path("one_time").asBoolean()).isFalse();
    assertThat(kb.path("buttons")).hasSize(1);
    assertThat(label(kb.path("buttons").get(0).get(0))).isEqualTo("Начать");
  }

  @Test
  @DisplayName("screen2 — две строки: создать / текущие")
  void screen2() throws Exception {
    JsonNode kb = read(KeyboardBuilder.screen2());

    assertThat(kb.path("buttons")).hasSize(2);
    assertThat(label(kb.path("buttons").get(0).get(0))).isEqualTo("Создать заявку на поиск вакансий");
    assertThat(label(kb.path("buttons").get(1).get(0))).isEqualTo("Текущие заявки на поиск");
  }

  @Test
  @DisplayName("статические клавиатуры возвращают один и тот же экземпляр (кэш)")
  void staticKeyboardsCached() {
    assertThat(KeyboardBuilder.screen2()).isSameAs(KeyboardBuilder.screen2());
    assertThat(KeyboardBuilder.experienceOptions()).isSameAs(KeyboardBuilder.experienceOptions());
  }

  @Test
  @DisplayName("screen3 — подписи отражают значения заявки")
  void screen3() throws Exception {
    SearchTask t = SearchTask.builder().regionCode(77).experienceFrom(2).salaryMin(50).salaryMax(70)
        .keyword("java").build();

    JsonNode rows = read(KeyboardBuilder.screen3(t)).path("buttons");

    assertThat(rows).hasSize(5);
    assertThat(label(rows.get(0).get(0))).isEqualTo("Регион: 77");
    assertThat(label(rows.get(1).get(0))).isEqualTo("Мин. опыт: 2 года");
    assertThat(label(rows.get(2).get(0))).isEqualTo("Зарплата: 50-70");
    assertThat(label(rows.get(3).get(0))).isEqualTo("Вакансия: «java»");
    assertThat(label(rows.get(4).get(0))).isEqualTo("Готово");
    assertThat(label(rows.get(4).get(1))).isEqualTo("В главное меню");
  }

  @Test
  @DisplayName("screen3 — пустая заявка: Любой / Любой / Любая / Любая")
  void screen3Empty() throws Exception {
    SearchTask t = SearchTask.builder().keyword("").salaryMax(-1).build();

    JsonNode rows = read(KeyboardBuilder.screen3(t)).path("buttons");

    assertThat(label(rows.get(0).get(0))).isEqualTo("Регион: Любой");
    assertThat(label(rows.get(1).get(0))).isEqualTo("Мин. опыт: Любой");
    assertThat(label(rows.get(2).get(0))).isEqualTo("Зарплата: Любая");
    assertThat(label(rows.get(3).get(0))).isEqualTo("Вакансия: Любая");
    assertThat(label(read(KeyboardBuilder.screen3(
        SearchTask.builder().keyword("").salaryMin(100).salaryMax(-1).build())).path("buttons").get(2).get(0)))
        .isEqualTo("Зарплата: от 100");
  }

  @Test
  @DisplayName("regionPage — первая страница: только «->», затем «Любой регион»/«Назад»")
  void regionPageFirst() throws Exception {
    RegionPage page = new RegionPage(List.of(new Region(1, "A"), new Region(2, "B")), 1, true);

    JsonNode rows = read(KeyboardBuilder.regionPage(page)).path("buttons");

    assertThat(rows).hasSize(4);
    assertThat(label(rows.get(0).get(0))).isEqualTo("1 A");
    assertThat(rows.get(2)).hasSize(1);
    assertThat(label(rows.get(2).get(0))).isEqualTo("-> [2]");
    assertThat(label(rows.get(3).get(0))).isEqualTo("Любой регион");
    assertThat(label(rows.get(3).get(1))).isEqualTo("Назад к заявке");
  }

  @Test
  @DisplayName("regionPage — середина: «<-» и «->»; конец: «<-» и «Сначала»")
  void regionPageNavigation() throws Exception {
    JsonNode middle = read(KeyboardBuilder.regionPage(new RegionPage(List.of(), 3, true))).path("buttons");
    JsonNode last = read(KeyboardBuilder.regionPage(new RegionPage(List.of(), 3, false))).path("buttons");

    assertThat(label(middle.get(0).get(0))).isEqualTo("<- [2]");
    assertThat(label(middle.get(0).get(1))).isEqualTo("-> [4]");
    assertThat(label(last.get(0).get(0))).isEqualTo("<- [2]");
    assertThat(label(last.get(0).get(1))).isEqualTo("Сначала");
  }

  @Test
  @DisplayName("taskInlineKeyboard — inline=true, payload с command и id")
  void inlineKeyboard() throws Exception {
    JsonNode kb = read(KeyboardBuilder.taskInlineKeyboard("abc"));

    assertThat(kb.path("inline").asBoolean()).isTrue();
    JsonNode row = kb.path("buttons").get(0);
    assertThat(row).hasSize(2);
    JsonNode updatePayload = read(row.get(0).path("action").path("payload").asText());
    assertThat(updatePayload.path("command").asText()).isEqualTo("update_task");
    assertThat(updatePayload.path("id").asText()).isEqualTo("abc");
    assertThat(read(row.get(1).path("action").path("payload").asText()).path("command").asText())
        .isEqualTo("delete_task");
    assertThat(row.get(1).path("color").asText()).isEqualTo("negative");
  }

  @Test
  @DisplayName("подписи обрезаются до 40 символов, кавычки вычищаются из payload")
  void labelTruncation() throws Exception {
    SearchTask t = SearchTask.builder().keyword("x\"".repeat(30)).salaryMax(-1).build();

    JsonNode btn = read(KeyboardBuilder.screen3(t)).path("buttons").get(3).get(0);

    assertThat(label(btn)).hasSize(40);
    assertThat(btn.path("action").path("payload").asText()).startsWith("\"").doesNotContain("x\"x");
  }
}
