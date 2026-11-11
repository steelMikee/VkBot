package ru.github.vacancybot.model;

import java.util.Optional;

public record EditingRef(String draftId, String originalId) {

  private static final char SEPARATOR = ':';

  public static Optional<EditingRef> parse(String composite) {
    if (composite == null || composite.isBlank()) {
      return Optional.empty();
    }
    int idx = composite.indexOf(SEPARATOR);
    if (idx < 0) {
      return Optional.of(new EditingRef(composite, null));
    }
    return Optional.of(new EditingRef(composite.substring(0, idx), composite.substring(idx + 1)));
  }

  public static String compose(String draftId, String originalId) {
    return draftId + SEPARATOR + originalId;
  }

  public boolean isUpdate() {
    return originalId != null;
  }
}
