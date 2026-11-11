package ru.github.vacancybot.command;

import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FilterKind {
  REGION("регион", "/region"),
  EXPERIENCE("минимальный опыт", "/minexp"),
  SALARY("минимальная зарплата", "/minzp"),
  KEYWORD("наименование вакансии", "/keyword");

  private final String labelPrefix;
  private final String alias;

  public static Optional<FilterKind> detect(String normalizedText) {
    return Arrays.stream(values())
        .filter(k -> normalizedText.startsWith(k.labelPrefix) || normalizedText.startsWith(k.alias))
        .findFirst();
  }
}
