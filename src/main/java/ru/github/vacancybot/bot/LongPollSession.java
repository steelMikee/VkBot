package ru.github.vacancybot.bot;

public record LongPollSession(String server, String key, String ts) {

  private static final int WAIT_SECONDS = 25;

  public String checkUrl() {
    return server + "?act=a_check&key=" + key + "&ts=" + ts + "&wait=" + WAIT_SECONDS;
  }

  public LongPollSession withTs(String newTs) {
    return new LongPollSession(server, key, newTs);
  }
}
