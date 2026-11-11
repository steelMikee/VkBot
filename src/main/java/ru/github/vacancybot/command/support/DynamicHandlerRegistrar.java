package ru.github.vacancybot.command.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandDispatcher;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.FilterCommands;
import ru.github.vacancybot.command.TaskIdCommands;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.ButtonLabels;
import ru.github.vacancybot.util.RegionDirectory;

@Slf4j
@RequiredArgsConstructor
public class DynamicHandlerRegistrar {

  private static final String PAYLOAD_DELETE = "delete_task";
  private static final String PAYLOAD_UPDATE = "update_task";
  private static final String UNKNOWN_REGION =
      "Вы ввели несуществующий регион, выберите его из списка по кнопкам меню.";

  private static final Map<String, Integer> EXPERIENCE_OPTIONS = Map.of(
      lower(ButtonLabels.NO_EXPERIENCE), 0,
      lower(ButtonLabels.ONE_YEAR), 1,
      lower(ButtonLabels.TWO_YEARS), 2,
      lower(ButtonLabels.THREE_YEARS_PLUS), 3,
      lower(ButtonLabels.ANY_EXPERIENCE), 0);

  private static final Set<String> REGION_NAVIGATION = Set.of(
      lower(ButtonLabels.FIRST_PAGE), lower(ButtonLabels.ANY_REGION),
      lower(ButtonLabels.BACK_TO_TASK));

  private final UserService userService;
  private final DraftEditor draftEditor;
  private final BotScreens screens;
  private final FilterCommands filters;
  private final TaskIdCommands taskIdCommands;
  private final ObjectMapper mapper = new ObjectMapper();

  public void registerAll(CommandDispatcher dispatcher) {
    registerInlinePayload(dispatcher);
    registerFilterMenuButtons(dispatcher);
    registerExperienceOptions(dispatcher);
    registerSalaryOptions(dispatcher);
    registerAnyRegion(dispatcher);
    registerRegionFreeText(dispatcher);
  }

  private void registerInlinePayload(CommandDispatcher dispatcher) {
    dispatcher.registerDynamic(
        m -> m.payload() != null
            && (m.payload().contains(PAYLOAD_DELETE) || m.payload().contains(PAYLOAD_UPDATE)),
        this::handleInlinePayload);
  }

  private void handleInlinePayload(IncomingMessage message) {
    try {
      JsonNode node = mapper.readTree(message.payload());
      if (!node.hasNonNull("command") || !node.hasNonNull("id")) {
        return;
      }
      String command = node.get("command").asText();
      String taskId = node.get("id").asText();
      if (PAYLOAD_DELETE.equals(command)) {
        taskIdCommands.delete().handleTaskId(message, taskId);
      } else if (PAYLOAD_UPDATE.equals(command)) {
        taskIdCommands.update().handleTaskId(message, taskId);
      }
    } catch (JsonProcessingException e) {
      log.error("Inline button payload parse error: {}", e.getMessage());
    }
  }

  private void registerFilterMenuButtons(CommandDispatcher dispatcher) {
    registerMenuButton(dispatcher, "регион", filters.region(), CommandType.REGION);
    registerMenuButton(dispatcher, "мин. опыт", filters.minExp(), CommandType.MIN_EXP);
    registerMenuButton(dispatcher, "зарплата", filters.minZp(), CommandType.MIN_ZP);
    registerMenuButton(dispatcher, "вакансия", filters.keyword(), CommandType.KEYWORD);
  }

  private void registerMenuButton(CommandDispatcher dispatcher, String prefix,
      BotCommand command, CommandType type) {
    dispatcher.registerDynamic(
        inState(DialogState.CREATING_TASK).and(m -> m.normalizedText().startsWith(prefix)),
        m -> command.handle(m.withCommand(type, null)));
  }

  private void registerExperienceOptions(CommandDispatcher dispatcher) {
    dispatcher.registerDynamic(
        inState(DialogState.AWAIT_EXPERIENCE)
            .and(m -> EXPERIENCE_OPTIONS.containsKey(m.normalizedText())),
        m -> filters.minExp().applyExp(m.userId(), EXPERIENCE_OPTIONS.get(m.normalizedText())));
  }

  private void registerSalaryOptions(CommandDispatcher dispatcher) {
    dispatcher.registerDynamic(
        inState(DialogState.AWAIT_SALARY).and(m -> isSalaryOption(m.normalizedText())),
        m -> {
          SalaryRange range = resolveSalaryRange(m.normalizedText());
          filters.minZp().applySalaryRange(m.userId(), range.min(), range.max());
        });
  }

  private static boolean isSalaryOption(String text) {
    return text.equals(lower(ButtonLabels.ANY_SALARY))
        || text.contains("50000") || text.contains("70000") || text.contains("100000");
  }

  private static SalaryRange resolveSalaryRange(String text) {
    if (text.equals(lower(ButtonLabels.ANY_SALARY))) {
      return new SalaryRange(0, SearchTask.NO_SALARY_LIMIT);
    }
    if (text.contains("50000 - 70000")) {
      return new SalaryRange(50_000, 70_000);
    }
    if (text.contains("70000 - 100000")) {
      return new SalaryRange(70_000, 100_000);
    }
    return new SalaryRange(100_000, SearchTask.NO_SALARY_LIMIT);
  }

  private void registerAnyRegion(CommandDispatcher dispatcher) {
    dispatcher.registerDynamic(
        inState(DialogState.AWAIT_REGION)
            .and(m -> m.trimmedText().equalsIgnoreCase(ButtonLabels.ANY_REGION)),
        m -> draftEditor.update(m.userId(),
            task -> task.setRegion(0, SearchTask.ALL_REGIONS),
            task -> "✅ Выбраны все регионы страны."));
  }

  private void registerRegionFreeText(CommandDispatcher dispatcher) {
    dispatcher.registerDynamic(this::isRegionFreeText, this::handleRegionFreeText);
  }

  private boolean isRegionFreeText(IncomingMessage m) {
    if (!inState(DialogState.AWAIT_REGION).test(m)) {
      return false;
    }
    String text = m.trimmedText();
    String lower = m.normalizedText();
    return !text.startsWith("/")
        && CommandType.fromLabel(text).isEmpty()
        && !lower.startsWith("->")
        && !lower.startsWith("<-")
        && !REGION_NAVIGATION.contains(lower);
  }

  private void handleRegionFreeText(IncomingMessage m) {
    long userId = m.userId();
    RegionDirectory.resolve(m.trimmedText()).ifPresentOrElse(
        region -> draftEditor.update(userId,
            task -> task.setRegion(region.code(), region.name()),
            task -> "✅ Регион выбран: " + region.code() + " " + region.name()),
        () -> screens.regionPageWithText(userId, 1, UNKNOWN_REGION));
  }

  private Predicate<IncomingMessage> inState(DialogState state) {
    return m -> userService.getDialogState(m.userId()) == state;
  }

  private static String lower(String text) {
    return text.toLowerCase(Locale.ROOT);
  }

  private record SalaryRange(int min, int max) {}
}
