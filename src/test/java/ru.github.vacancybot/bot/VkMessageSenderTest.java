package ru.github.vacancybot.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.objects.messages.Keyboard;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.github.vacancybot.util.KeyboardBuilder;

@DisplayName("VkMessageSender")
class VkMessageSenderTest {

  private final VkApiClient vk = mock(VkApiClient.class, Mockito.RETURNS_DEEP_STUBS);
  private final com.vk.api.sdk.queries.messages.MessagesSendQueryWithDeprecated query =
      mock(com.vk.api.sdk.queries.messages.MessagesSendQueryWithDeprecated.class, Mockito.RETURNS_SELF);
  private final GroupActor actor = new GroupActor(1L, "test_token");
  private VkMessageSender sender;

  @BeforeEach
  void setUp() {
    when(vk.messages().sendDeprecated(actor)).thenReturn(query);
    sender = new VkMessageSender(vk, actor);
  }

  @Test
  @DisplayName("sendWithKeyboard — отправляет сообщение с клавиатурой")
  void sendWithKeyboard() {
    sender.sendWithKeyboard(1L, "hello", KeyboardBuilder.screen1());

    ArgumentCaptor<Long> userId = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
    verify(query).userId(userId.capture());
    verify(query).message(message.capture());
    assertThat(userId.getValue()).isEqualTo(1L);
    assertThat(message.getValue()).isEqualTo("hello");
  }

  @Test
  @DisplayName("sendChunked — разбивает длинный текст на куски")
  void sendChunked() {
    String longText = "x".repeat(5000);
    sender.sendChunked(1L, "", List.of(longText));

    verify(query, times(2)).message(anyString());
  }

  @Test
  @DisplayName("sendChunked — пустой список не отправляет ничего")
  void sendChunkedEmpty() {
    sender.sendChunked(1L, "", List.of());

    verify(query, never()).message(anyString());
  }

}
