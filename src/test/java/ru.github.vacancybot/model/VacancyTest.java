package ru.github.vacancybot.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Vacancy")
class VacancyTest {

  @Test
  @DisplayName("record сериализуется и читается Jackson без доп. аннотаций")
  void jsonRoundTrip() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    Vacancy v = new Vacancy("Dev", 100, 200, "http://v", "Corp", "Москва");

    Vacancy back = mapper.readValue(mapper.writeValueAsString(v), Vacancy.class);

    assertThat(back).isEqualTo(v);
  }
}
