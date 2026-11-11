package ru.github.vacancybot.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import ru.github.vacancybot.exception.StorageException;

public abstract class AbstractJsonRepository {

  private static final String TMP_SUFFIX = ".tmp";

  protected final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  protected final Path dataDir;

  protected AbstractJsonRepository(Path dataDir) {
    this.dataDir = dataDir;
    try {
      Files.createDirectories(dataDir);
    } catch (IOException e) {
      throw new StorageException("Не удалось создать директорию: " + dataDir, e);
    }
  }

  protected <T> T read(Path file, Class<T> type) throws IOException {
    return mapper.readValue(file.toFile(), type);
  }

  protected <T> T read(Path file, TypeReference<T> type) throws IOException {
    return mapper.readValue(file.toFile(), type);
  }

  protected void write(Path file, Object value) {
    Path tmp = file.resolveSibling(file.getFileName() + TMP_SUFFIX);
    try {
      mapper.writeValue(tmp.toFile(), value);
      moveAtomicOrFallback(tmp, file);
    } catch (IOException e) {
      throw new StorageException("Не удалось записать файл: " + file, e);
    }
  }

  private static void moveAtomicOrFallback(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  protected void deleteIfExists(Path file) {
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      throw new StorageException("Не удалось удалить файл: " + file, e);
    }
  }
}
