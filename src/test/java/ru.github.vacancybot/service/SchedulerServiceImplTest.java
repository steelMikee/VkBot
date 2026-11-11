package ru.github.vacancybot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SchedulerServiceImpl")
class SchedulerServiceImplTest {

  private final SchedulerServiceImpl scheduler = new SchedulerServiceImpl();

  @AfterEach
  void tearDown() {
    scheduler.shutdown();
  }

  @Test
  @DisplayName("задача с нулевой задержкой выполняется")
  void runsScheduledTask() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);

    scheduler.schedule(1L, latch::countDown, 1, 0);

    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @DisplayName("cancel отменяет все задачи пользователя, чужие — остаются")
  void cancelIsPerUser() throws Exception {
    AtomicInteger cancelledRuns = new AtomicInteger();
    CountDownLatch other = new CountDownLatch(1);
    scheduler.schedule(1L, cancelledRuns::incrementAndGet, 1, 1);
    scheduler.schedule(1L, cancelledRuns::incrementAndGet, 1, 1);
    scheduler.schedule(2L, other::countDown, 1, 0);

    scheduler.cancel(1L);

    assertThat(other.await(2, TimeUnit.SECONDS)).isTrue();
    assertThat(cancelledRuns.get()).isZero();
  }

  @Test
  @DisplayName("cancel для неизвестного пользователя — без ошибок")
  void cancelUnknown() {
    scheduler.cancel(999L);
  }

  @Test
  @DisplayName("shutdown идемпотентен")
  void shutdownTwice() {
    scheduler.shutdown();
    scheduler.shutdown();
  }
}
