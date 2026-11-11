package ru.github.vacancybot.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import ru.github.vacancybot.model.BotUser;

@Slf4j
public class JsonUserRepository extends AbstractJsonRepository implements UserRepository {

  private static final String SUBDIR = "users";

  public JsonUserRepository(String dataDir) {
    super(Path.of(dataDir, SUBDIR));
  }

  @Override
  public Optional<BotUser> findById(long id) {
    Path file = fileFor(id);
    if (!Files.exists(file)) {
      return Optional.empty();
    }
    try {
      return Optional.of(read(file, BotUser.class));
    } catch (IOException e) {
      log.error("Failed to read user file {}: {}", file, e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void save(BotUser user) {
    write(fileFor(user.getVkUserId()), user);
  }

  private Path fileFor(long id) {
    return dataDir.resolve(id + ".json");
  }
}
