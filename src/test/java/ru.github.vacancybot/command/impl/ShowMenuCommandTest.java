package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.message;
import static ru.github.vacancybot.command.impl.CommandTestSupport.newTask;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.MessageParser;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.model.SearchTask;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShowMenuCommand")
class ShowMenuCommandTest {

  @Mock DraftEditor draftEditor;
  @Mock BotScreens screens;
  @InjectMocks ShowMenuCommand command;

  @Test
  @DisplayName("root_menu — черновик отбрасывается, показывается главное меню")
  void rootMenuDiscardsDraft() {
    command.handle(message(CommandType.MAIN_MENU, MessageParser.ARG_ROOT_MENU));

    verify(draftEditor).discardDraft(USER);
    verify(screens).mainMenu(eq(USER), anyString());
  }

  @Test
  @DisplayName("back_to_task — возвращает к экрану заявки")
  void backToTaskShowsDraft() {
    SearchTask draft = newTask("d");
    when(draftEditor.findDraft(USER)).thenReturn(Optional.of(draft));

    command.handle(message(CommandType.MAIN_MENU, MessageParser.ARG_BACK_TO_TASK));

    verify(screens).taskScreen(eq(USER), org.mockito.ArgumentMatchers.startsWith("✏️ Продолжение"), eq(draft));
    verify(draftEditor, never()).discardDraft(anyLong());
  }

  @Test
  @DisplayName("back_to_task без черновика — главное меню")
  void backToTaskWithoutDraft() {
    when(draftEditor.findDraft(USER)).thenReturn(Optional.empty());

    command.handle(message(CommandType.MAIN_MENU, MessageParser.ARG_BACK_TO_TASK));

    verify(screens).mainMenu(eq(USER), anyString());
    verify(screens, never()).taskScreen(anyLong(), anyString(), any());
  }

  @Test
  @DisplayName("без аргумента — главное меню, черновик не трогается")
  void noArgument() {
    command.handle(message(CommandType.MAIN_MENU, null));

    verify(screens).mainMenu(eq(USER), anyString());
    verify(draftEditor, never()).discardDraft(anyLong());
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.MAIN_MENU);
  }
}
