package ru.github.vacancybot.service;

public interface SchedulerService {

  void schedule(long userId, Runnable task, long periodHours, long initialDelayMinutes);

  void cancel(long userId);

  void shutdown();
}
