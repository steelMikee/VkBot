package ru.github.vacancybot.command;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.UserService;

@RequiredArgsConstructor
public class CommandDispatcher {

  private final Map<CommandType, BotCommand> registry = new EnumMap<>(CommandType.class);
  private final List<DynamicHandler> dynamicHandlers = new ArrayList<>();
  private final BotCommand defaultCommand;
  private final UserService userService;
  private final DispatchPolicy policy;

  public void register(BotCommand command) {
    registry.put(command.getType(), command);
  }

  public void registerDynamic(Predicate<IncomingMessage> predicate,
      Consumer<IncomingMessage> handler) {
    dynamicHandlers.add(new DynamicHandler(predicate, handler));
  }

  public void dispatch(IncomingMessage message) {
    DialogState state = userService.getDialogState(message.userId());
    if (policy.requiresDefault(state, message.parsedCommand(), message.normalizedText())) {
      defaultCommand.handle(message);
      return;
    }
    for (DynamicHandler dynamic : dynamicHandlers) {
      if (dynamic.predicate().test(message)) {
        dynamic.handler().accept(message);
        return;
      }
    }
    registry.getOrDefault(message.parsedCommand().type(), defaultCommand).handle(message);
  }

  private record DynamicHandler(Predicate<IncomingMessage> predicate,
                                Consumer<IncomingMessage> handler) {}
}
