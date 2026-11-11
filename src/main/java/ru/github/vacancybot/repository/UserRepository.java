package ru.github.vacancybot.repository;

import java.util.Optional;
import ru.github.vacancybot.model.BotUser;

public interface UserRepository {

  Optional<BotUser> findById(long id);

  void save(BotUser user);
}
