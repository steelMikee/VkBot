package ru.github.vacancybot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.model.BotUser;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl — кэш write-through")
class UserServiceImplTest {

  @Mock UserRepository repository;
  @InjectMocks UserServiceImpl service;

  @Test
  @DisplayName("getOrCreate — нового пользователя создаёт в SCREEN_1 и сохраняет")
  void createsNewUser() {
    when(repository.findById(1L)).thenReturn(Optional.empty());

    BotUser user = service.getOrCreate(1L);

    assertThat(user.getVkUserId()).isEqualTo(1L);
    assertThat(user.getDialogState()).isEqualTo(DialogState.SCREEN_1);
    verify(repository).save(user);
  }

  @Test
  @DisplayName("getOrCreate — существующий читается с диска один раз, далее из кэша")
  void cachesUser() {
    when(repository.findById(1L))
        .thenReturn(Optional.of(BotUser.builder().vkUserId(1L).build()));

    service.getOrCreate(1L);
    service.getOrCreate(1L);
    service.getDialogState(1L);

    verify(repository, times(1)).findById(1L);
  }

  @Test
  @DisplayName("LRU-кэш — вытесняет наименее используемые записи")
  void lruEvictsLeastRecentlyUsed() {
    UserServiceImpl.LruCache<Long, BotUser> cache = new UserServiceImpl.LruCache<>(3);

    cache.put(1L, BotUser.builder().vkUserId(1L).build());
    cache.put(2L, BotUser.builder().vkUserId(2L).build());
    cache.put(3L, BotUser.builder().vkUserId(3L).build());
    assertThat(cache.size()).isEqualTo(3);

    cache.put(4L, BotUser.builder().vkUserId(4L).build());
    assertThat(cache.size()).isEqualTo(3);
    assertThat(cache.containsKey(1L)).isFalse();
    assertThat(cache.containsKey(2L)).isTrue();
    assertThat(cache.containsKey(3L)).isTrue();
    assertThat(cache.containsKey(4L)).isTrue();
  }

  @Test
  @DisplayName("кэш — доступ обновляет порядок использования")
  void accessUpdatesOrder() {
    UserServiceImpl.LruCache<Long, BotUser> cache = new UserServiceImpl.LruCache<>(3);

    cache.put(1L, BotUser.builder().vkUserId(1L).build());
    cache.put(2L, BotUser.builder().vkUserId(2L).build());
    cache.put(3L, BotUser.builder().vkUserId(3L).build());

    cache.get(1L);
    cache.put(4L, BotUser.builder().vkUserId(4L).build());

    assertThat(cache.containsKey(1L)).isTrue();
    assertThat(cache.containsKey(2L)).isFalse();
    assertThat(cache.containsKey(3L)).isTrue();
    assertThat(cache.containsKey(4L)).isTrue();
  }

  @Test
  @DisplayName("кэш — remove удаляет запись")
  void cacheRemove() {
    UserServiceImpl.LruCache<Long, BotUser> cache = new UserServiceImpl.LruCache<>(3);

    cache.put(1L, BotUser.builder().vkUserId(1L).build());
    cache.remove(1L);

    assertThat(cache.containsKey(1L)).isFalse();
    assertThat(cache.size()).isZero();
  }

  @Test
  @DisplayName("setDialogState — меняет и сразу сохраняет")
  void setDialogState() {
    when(repository.findById(1L)).thenReturn(Optional.empty());

    service.setDialogState(1L, DialogState.AWAIT_REGION);

    ArgumentCaptor<BotUser> captor = ArgumentCaptor.forClass(BotUser.class);
    verify(repository, times(2)).save(captor.capture());
    assertThat(captor.getValue().getDialogState()).isEqualTo(DialogState.AWAIT_REGION);
    assertThat(service.getDialogState(1L)).isEqualTo(DialogState.AWAIT_REGION);
  }

  @Test
  @DisplayName("setEditingTaskId — null сбрасывает ссылку")
  void setEditingTaskId() {
    when(repository.findById(1L)).thenReturn(Optional.empty());

    service.setEditingTaskId(1L, "d:o");
    assertThat(service.getOrCreate(1L).getEditingTaskId()).isEqualTo("d:o");
    service.setEditingTaskId(1L, null);

    assertThat(service.getOrCreate(1L).getEditingTaskId()).isNull();
    verify(repository, times(3)).save(any());
  }

  @Test
  @DisplayName("getDialogState — null в старом файле трактуется как SCREEN_1")
  void nullStateDefaultsToScreen1() {
    when(repository.findById(1L))
        .thenReturn(Optional.of(new BotUser(1L, null, null)));

    assertThat(service.getDialogState(1L)).isEqualTo(DialogState.SCREEN_1);
  }
}
