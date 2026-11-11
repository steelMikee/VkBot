package ru.github.vacancybot.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.command.CommandDispatcher;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.MessageParser;
import ru.github.vacancybot.dto.IncomingMessage;

@ExtendWith(MockitoExtension.class)
@DisplayName("VkBot — главный цикл Long Poll")
class VkBotTest {

  private static final String MESSAGE_UPDATE = """
      {"ts":"11","updates":[
        {"type":"message_new","object":{"message":{"from_id":42,"text":"/start","payload":"\\"x\\""}}},
        {"type":"message_reply","object":{}}
      ]}""";

  @Mock LongPollClient client;
  @Mock CommandDispatcher dispatcher;

  private final LongPollSession session = new LongPollSession("https://lp.vk.com/x", "k", "1");

  private void runBotUntil(VkBot bot, AtomicInteger polls, int target) throws Exception {
    Thread thread = new Thread(() -> {
      try {
        bot.start();
      } catch (Exception ignored) {
      }
    });
    thread.start();
    long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
    while (polls.get() < target && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
    thread.interrupt();
    thread.join(2_000);
    assertThat(thread.isAlive()).as("цикл должен завершиться по прерыванию").isFalse();
  }

  @Test
  @DisplayName("message_new разбирается и уходит в диспетчер, другие типы игнорируются")
  void dispatchesNewMessagesOnly() throws Exception {
    AtomicInteger polls = new AtomicInteger();
    when(client.openSession()).thenReturn(session);
    when(client.check(any())).thenAnswer(inv -> {
      if (polls.incrementAndGet() == 1) {
        return MESSAGE_UPDATE;
      }
      Thread.sleep(50);
      return "{\"ts\":\"12\",\"updates\":[]}";
    });

    runBotUntil(new VkBot(client, dispatcher, new MessageParser()), polls, 2);

    ArgumentCaptor<IncomingMessage> captor = ArgumentCaptor.forClass(IncomingMessage.class);
    verify(dispatcher, times(1)).dispatch(captor.capture());
    IncomingMessage msg = captor.getValue();
    assertThat(msg.userId()).isEqualTo(42L);
    assertThat(msg.rawText()).isEqualTo("/start");
    assertThat(msg.payload()).isEqualTo("\"x\"");
    assertThat(msg.parsedCommand().type()).isEqualTo(CommandType.DEFAULT);
  }

  @Test
  @DisplayName("ts обновляется после каждого ответа")
  void advancesTs() throws Exception {
    AtomicInteger polls = new AtomicInteger();
    when(client.openSession()).thenReturn(session);
    when(client.check(any())).thenAnswer(inv -> {
      polls.incrementAndGet();
      Thread.sleep(20);
      return "{\"ts\":\"99\",\"updates\":[]}";
    });

    runBotUntil(new VkBot(client, dispatcher, new MessageParser()), polls, 3);

    ArgumentCaptor<LongPollSession> captor = ArgumentCaptor.forClass(LongPollSession.class);
    verify(client, atLeastOnce()).check(captor.capture());
    assertThat(captor.getAllValues().get(1).ts()).isEqualTo("99");
  }

  @Test
  @DisplayName("failed:1 — берётся новый ts без переподключения")
  void failedOneUpdatesTs() throws Exception {
    AtomicInteger polls = new AtomicInteger();
    when(client.openSession()).thenReturn(session);
    when(client.check(any())).thenAnswer(inv -> {
      polls.incrementAndGet();
      Thread.sleep(20);
      return "{\"failed\":1,\"ts\":\"77\"}";
    });

    runBotUntil(new VkBot(client, dispatcher, new MessageParser()), polls, 2);

    verify(client, times(1)).openSession();
    ArgumentCaptor<LongPollSession> captor = ArgumentCaptor.forClass(LongPollSession.class);
    verify(client, atLeastOnce()).check(captor.capture());
    assertThat(captor.getAllValues().get(1).ts()).isEqualTo("77");
  }

  @Test
  @DisplayName("failed:2 и failed:3 — переподключение к Long Poll серверу")
  void failedTwoReconnects() throws Exception {
    AtomicInteger polls = new AtomicInteger();
    when(client.openSession()).thenReturn(session);
    when(client.check(any())).thenAnswer(inv -> {
      int n = polls.incrementAndGet();
      Thread.sleep(20);
      return n == 1 ? "{\"failed\":2}" : n == 2 ? "{\"failed\":3}" : "{\"ts\":\"1\",\"updates\":[]}";
    });

    runBotUntil(new VkBot(client, dispatcher, new MessageParser()), polls, 3);

    verify(client, times(3)).openSession();
  }

  @Test
  @DisplayName("исключение из клиента не убивает цикл — выполняется повторная попытка")
  void survivesClientErrors() throws Exception {
    AtomicInteger polls = new AtomicInteger();
    when(client.openSession()).thenReturn(session);
    when(client.check(any())).thenAnswer(inv -> {
      if (polls.incrementAndGet() == 1) {
        throw new java.io.IOException("boom");
      }
      Thread.sleep(20);
      return "{\"ts\":\"1\",\"updates\":[]}";
    });

    runBotUntil(new VkBot(client, dispatcher, new MessageParser()), polls, 2);

    assertThat(polls.get()).isGreaterThanOrEqualTo(2);
  }

  @Test
  @DisplayName("ошибка в диспетчере на одном сообщении не мешает остальным")
  void dispatcherErrorIsIsolated() throws Exception {
    AtomicInteger polls = new AtomicInteger();
    when(client.openSession()).thenReturn(session);
    when(client.check(any())).thenAnswer(inv -> {
      if (polls.incrementAndGet() == 1) {
        return """
            {"ts":"2","updates":[
              {"type":"message_new","object":{"message":{"from_id":1,"text":"a"}}},
              {"type":"message_new","object":{"message":{"from_id":2,"text":"b"}}}]}""";
      }
      Thread.sleep(20);
      return "{\"ts\":\"3\",\"updates\":[]}";
    });
    org.mockito.Mockito.doThrow(new IllegalStateException("x")).doNothing()
        .when(dispatcher).dispatch(any());

    runBotUntil(new VkBot(client, dispatcher, new MessageParser()), polls, 2);

    verify(dispatcher, times(2)).dispatch(any());
  }

  @Test
  @DisplayName("ответ без updates не вызывает диспетчер")
  void noUpdatesNoDispatch() throws Exception {
    AtomicInteger polls = new AtomicInteger();
    when(client.openSession()).thenReturn(session);
    when(client.check(any())).thenAnswer(inv -> {
      polls.incrementAndGet();
      Thread.sleep(20);
      return "{\"ts\":\"5\"}";
    });

    runBotUntil(new VkBot(client, dispatcher, new MessageParser()), polls, 2);

    verify(dispatcher, never()).dispatch(any());
  }
}
