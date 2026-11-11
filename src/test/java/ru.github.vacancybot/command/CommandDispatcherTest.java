package ru.github.vacancybot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.dto.ParsedCommand;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommandDispatcher — порядок маршрутизации")
class CommandDispatcherTest {

  @Mock UserService userService;
  @Mock BotCommand defaultCommand;
  @Mock BotCommand doneCommand;
  @Mock DispatchPolicy policy;

  private CommandDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    when(doneCommand.getType()).thenReturn(CommandType.DONE);
    dispatcher = new CommandDispatcher(defaultCommand, userService, policy);
    dispatcher.register(doneCommand);
    when(userService.getDialogState(1L)).thenReturn(DialogState.CREATING_TASK);
  }

  private static IncomingMessage msg(CommandType type) {
    return new IncomingMessage(1L, "txt", new ParsedCommand(type, null), null);
  }

  @Test
  @DisplayName("политика требует default → остальные не вызываются")
  void policyWins() {
    when(policy.requiresDefault(any(), any(), anyString())).thenReturn(true);
    List<IncomingMessage> dynamic = new ArrayList<>();
    dispatcher.registerDynamic(m -> true, dynamic::add);

    dispatcher.dispatch(msg(CommandType.DONE));

    verify(defaultCommand).handle(any());
    verify(doneCommand, never()).handle(any());
    assertThat(dynamic).isEmpty();
  }

  @Test
  @DisplayName("динамический хендлер имеет приоритет над реестром")
  void dynamicBeforeRegistry() {
    when(policy.requiresDefault(any(), any(), anyString())).thenReturn(false);
    List<IncomingMessage> dynamic = new ArrayList<>();
    dispatcher.registerDynamic(m -> false, m -> { throw new AssertionError("not matched"); });
    dispatcher.registerDynamic(m -> true, dynamic::add);

    dispatcher.dispatch(msg(CommandType.DONE));

    assertThat(dynamic).hasSize(1);
    verify(doneCommand, never()).handle(any());
  }

  @Test
  @DisplayName("команда из реестра")
  void registryCommand() {
    when(policy.requiresDefault(any(), any(), anyString())).thenReturn(false);

    dispatcher.dispatch(msg(CommandType.DONE));

    verify(doneCommand).handle(any());
    verify(defaultCommand, never()).handle(any());
  }

  @Test
  @DisplayName("незарегистрированный тип → default")
  void fallbackToDefault() {
    when(policy.requiresDefault(any(), any(), anyString())).thenReturn(false);

    dispatcher.dispatch(msg(CommandType.STOP));

    verify(defaultCommand).handle(any());
  }
}
