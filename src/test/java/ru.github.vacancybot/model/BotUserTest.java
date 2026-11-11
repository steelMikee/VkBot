package ru.github.vacancybot.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BotUser")
class BotUserTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  @DisplayName("builder: состояние по умолчанию SCREEN_1")
  void defaultState() {
    assertThat(BotUser.builder().vkUserId(1L).build().getDialogState())
        .isEqualTo(DialogState.SCREEN_1);
  }

  @Test
  @DisplayName("совместимость: старый JSON со строковым состоянием и лишним полем firstName")
  void readsLegacyJson() throws Exception {
    BotUser user = mapper.readValue(
        "{\"vkUserId\":5,\"firstName\":\"Ivan\",\"dialogState\":\"AWAIT_REGION\",\"editingTaskId\":\"a:b\"}",
        BotUser.class);

    assertThat(user.getVkUserId()).isEqualTo(5L);
    assertThat(user.getDialogState()).isEqualTo(DialogState.AWAIT_REGION);
    assertThat(user.getEditingTaskId()).isEqualTo("a:b");
  }

  @Test
  @DisplayName("сериализация пишет имя enum строкой")
  void writesEnumAsString() throws Exception {
    String json = mapper.writeValueAsString(
        BotUser.builder().vkUserId(1L).dialogState(DialogState.CREATING_TASK).build());

    assertThat(json).contains("\"dialogState\":\"CREATING_TASK\"");
  }
}
