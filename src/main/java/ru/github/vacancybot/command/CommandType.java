package ru.github.vacancybot.command;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommandType {
  DEFAULT("Начать", "/start"),
  MAIN_MENU("Показать меню", "/showMenu"),
  NEW_TASK("Создать заявку на поиск вакансий", "/newTask"),
  CURRENT_TASKS("Текущие заявки на поиск", "/current"),
  DELETE_TASK("Удалить заявку на поиск", "/delete"),
  UPDATE_TASK("Обновить задачу на поиск", "/update"),
  REGION("Регион", "/region"),
  MIN_EXP("Минимальный опыт", "/minExp"),
  MIN_ZP("Минимальная зарплата", "/minZp"),
  KEYWORD("Наименование вакансии", "/keyword"),
  DONE("Готово", "/done"),
  STOP("Остановить бота", "/stop"),
  NEXT("Далее", "/next"),
  NONE(null, null);

  private final String label;
  private final String alias;

  private static final Map<String, CommandType> BY_LABEL = index(CommandType::getLabel);
  private static final Map<String, CommandType> BY_ALIAS = index(CommandType::getAlias);

  public static Optional<CommandType> fromLabel(String text) {
    return lookup(BY_LABEL, text);
  }

  public static Optional<CommandType> fromAlias(String text) {
    return lookup(BY_ALIAS, text);
  }

  private static Map<String, CommandType> index(Function<CommandType, String> key) {
    return Arrays.stream(values())
        .filter(type -> key.apply(type) != null)
        .collect(Collectors.toUnmodifiableMap(
            type -> key.apply(type).toLowerCase(Locale.ROOT), Function.identity()));
  }

  private static Optional<CommandType> lookup(Map<String, CommandType> index, String text) {
    return text == null
        ? Optional.empty()
        : Optional.ofNullable(index.get(text.trim().toLowerCase(Locale.ROOT)));
  }
}
