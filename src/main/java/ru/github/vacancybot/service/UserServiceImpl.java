package ru.github.vacancybot.service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.model.BotUser;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.repository.UserRepository;

@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  static final int MAX_CACHE_SIZE = 1000;

  private final UserRepository userRepository;
  private final LruCache<Long, BotUser> cache = new LruCache<>(MAX_CACHE_SIZE);

  @Override
  public BotUser getOrCreate(long userId) {
    BotUser user = cache.get(userId);
    if (user == null) {
      user = userRepository.findById(userId).orElseGet(() -> createUser(userId));
      cache.put(userId, user);
    }
    return user;
  }

  @Override
  public DialogState getDialogState(long userId) {
    return Objects.requireNonNullElse(getOrCreate(userId).getDialogState(), DialogState.SCREEN_1);
  }

  @Override
  public void setDialogState(long userId, DialogState state) {
    BotUser user = getOrCreate(userId);
    user.setDialogState(state);
    userRepository.save(user);
  }

  @Override
  public void setEditingTaskId(long userId, String taskId) {
    BotUser user = getOrCreate(userId);
    user.setEditingTaskId(taskId);
    userRepository.save(user);
  }

  private BotUser createUser(long userId) {
    BotUser user = BotUser.builder().vkUserId(userId).build();
    userRepository.save(user);
    return user;
  }

  static class LruCache<K, V> {

    private final int maxSize;
    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<Object> accessOrder = new ConcurrentLinkedDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    LruCache(int maxSize) {
      this.maxSize = maxSize;
    }

    public V get(Object key) {
      lock.lock();
      try {
        V value = map.get(key);
        if (value != null) {
          accessOrder.remove(key);
          accessOrder.addLast(key);
        }
        return value;
      } finally {
        lock.unlock();
      }
    }

    public V put(K key, V value) {
      lock.lock();
      try {
        if (!map.containsKey(key)) {
          if (map.size() >= maxSize) {
            Object eldest = accessOrder.pollFirst();
            if (eldest != null) {
              map.remove(eldest);
            }
          }
        } else {
          accessOrder.remove(key);
        }
        accessOrder.addLast(key);
        return map.put(key, value);
      } finally {
        lock.unlock();
      }
    }

    public V remove(Object key) {
      lock.lock();
      try {
        accessOrder.remove(key);
        return map.remove(key);
      } finally {
        lock.unlock();
      }
    }

    public int size() {
      lock.lock();
      try {
        return map.size();
      } finally {
        lock.unlock();
      }
    }

    public boolean containsKey(Object key) {
      lock.lock();
      try {
        return map.containsKey(key);
      } finally {
        lock.unlock();
      }
    }
  }
}
