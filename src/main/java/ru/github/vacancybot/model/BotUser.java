package ru.github.vacancybot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BotUser {

  private long vkUserId;
  @Builder.Default
  private DialogState dialogState = DialogState.SCREEN_1;
  private String editingTaskId;
}
