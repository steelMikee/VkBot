package ru.github.vacancybot.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchedulerServiceImpl implements SchedulerService {

  private static final int POOL_SIZE = 4;
  private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(POOL_SIZE);
  private final Map<Long, List<Future<?>>> scheduledTasks = new ConcurrentHashMap<>();

  @Override
  public void schedule(long userId, Runnable task, long periodHours, long initialDelayMinutes) {
    Future<?> future = executor.scheduleAtFixedRate(
        task, initialDelayMinutes, TimeUnit.HOURS.toMinutes(periodHours), TimeUnit.MINUTES);
    scheduledTasks.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(future);
    log.info("The task is scheduled for the user {}", userId);
  }

  @Override
  public void cancel(long userId) {
    List<Future<?>> futures = scheduledTasks.remove(userId);
    if (futures != null) {
      futures.forEach(f -> f.cancel(false));
      log.info("Tasks for the user {} have been cancelled.", userId);
    }
  }

  @Override
  public void shutdown() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
    log.info("The scheduler has stopped.");
  }
}
