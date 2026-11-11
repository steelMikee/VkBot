package ru.github.vacancybot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.exception.VacancyApiException;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchLauncherImpl — фоновый поиск в виртуальном потоке")
class SearchLauncherImplTest {

  private static final long TIMEOUT = 3_000;

  @Mock VacancyService vacancyService;
  @Mock SchedulerService schedulerService;
  @Mock NotificationService notificationService;
  @Mock VkMessageSender sender;

  private SearchLauncherImpl launcher;
  private final SearchTask task = SearchTask.builder().id("t").ownerId(5L).keyword("qa")
      .regionName("Москва").salaryMax(-1).build();

  @BeforeEach
  void setUp() {
    launcher = new SearchLauncherImpl(vacancyService, schedulerService, notificationService,
        sender, 24L, 1L);
  }

  @Test
  @DisplayName("scheduleUpdates — планирует уведомление с параметрами конфигурации")
  void scheduleUpdates() {
    launcher.scheduleUpdates(task);

    ArgumentCaptor<Runnable> job = ArgumentCaptor.forClass(Runnable.class);
    verify(schedulerService).schedule(eq(5L), job.capture(), eq(24L), eq(1L));
    job.getValue().run();
    verify(notificationService).notifyUser(task);
  }

  @Test
  @DisplayName("launch — найдено: заголовок, сниппеты, затем cancel + schedule")
  void launchWithResults() {
    when(vacancyService.fetchAndSave(task))
        .thenReturn(List.of(new Vacancy("QA", 50, 0, "http://v", "", "")));

    launcher.launch(task);

    verify(schedulerService, timeout(TIMEOUT)).schedule(eq(5L), any(), eq(24L), eq(1L));
    InOrder order = Mockito.inOrder(sender, schedulerService);
    order.verify(sender).sendText(eq(5L), org.mockito.ArgumentMatchers.startsWith("🎉 Найдено вакансий: 1"));
    order.verify(sender).sendChunked(eq(5L), eq(""), any());
    order.verify(schedulerService).cancel(5L);
    order.verify(schedulerService).schedule(eq(5L), any(), eq(24L), eq(1L));
  }

  @Test
  @DisplayName("launch — пусто: сообщение «не найдено», планировщик всё равно запускается")
  void launchEmpty() {
    when(vacancyService.fetchAndSave(task)).thenReturn(List.of());

    launcher.launch(task);

    verify(sender, timeout(TIMEOUT)).sendText(eq(5L), org.mockito.ArgumentMatchers.contains("вакансий в регионе не найдено"));
    verify(schedulerService, timeout(TIMEOUT)).schedule(eq(5L), any(), eq(24L), eq(1L));
    verify(sender, never()).sendChunked(anyLong(), anyString(), any());
  }

  @Test
  @DisplayName("launch — ошибка поиска: пользователь уведомлён, периодика всё равно запланирована")
  void launchError() {
    when(vacancyService.fetchAndSave(task)).thenThrow(new VacancyApiException("down"));

    launcher.launch(task);

    verify(sender, timeout(TIMEOUT)).sendText(eq(5L), org.mockito.ArgumentMatchers.contains("Фоновая ошибка"));
    verify(schedulerService, timeout(TIMEOUT)).schedule(eq(5L), any(), eq(24L), eq(1L));
  }

  @Test
  @DisplayName("launch не блокирует вызывающий поток")
  void launchIsAsync() throws Exception {
    java.util.concurrent.CountDownLatch gate = new java.util.concurrent.CountDownLatch(1);
    when(vacancyService.fetchAndSave(task)).thenAnswer(inv -> {
      gate.await();
      return List.of();
    });

    long start = System.nanoTime();
    launcher.launch(task);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    gate.countDown();

    assertThat(elapsedMs).isLessThan(500);
    verify(schedulerService, timeout(TIMEOUT)).schedule(anyLong(), any(), anyLong(), anyLong());
  }
}
