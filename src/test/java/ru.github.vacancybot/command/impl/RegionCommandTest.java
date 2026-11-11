package ru.github.vacancybot.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.github.vacancybot.command.impl.CommandTestSupport.USER;
import static ru.github.vacancybot.command.impl.CommandTestSupport.message;
import static ru.github.vacancybot.command.impl.CommandTestSupport.newTask;

import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegionCommand")
class RegionCommandTest {

  @Mock UserService userService;
  @Mock DraftEditor draftEditor;
  @Mock BotScreens screens;
  @InjectMocks RegionCommand command;

  @Test
  @DisplayName("без аргумента — состояние AWAIT_REGION и первая страница регионов")
  void showsFirstPage() {
    command.handle(message(CommandType.REGION, null));

    verify(userService).setDialogState(USER, DialogState.AWAIT_REGION);
    verify(screens).regionPage(USER, 1);
  }

  @Test
  @DisplayName("аргумент — код региона: применяется к черновику")
  @SuppressWarnings("unchecked")
  void appliesRegionByCode() {
    command.handle(message(CommandType.REGION, "77"));

    ArgumentCaptor<Consumer<SearchTask>> change = ArgumentCaptor.forClass(Consumer.class);
    ArgumentCaptor<Function<SearchTask, String>> text = ArgumentCaptor.forClass(Function.class);
    verify(draftEditor).update(eq(USER), change.capture(), text.capture());
    SearchTask task = newTask("x");
    change.getValue().accept(task);
    assertThat(task.getRegionCode()).isEqualTo(77);
    assertThat(task.getRegionName()).isEqualTo("Москва");
    assertThat(text.getValue().apply(task)).isEqualTo("✅ Регион выбран: [77] Москва");
    verify(screens, never()).regionPage(anyLong(), anyInt());
  }

  @Test
  @DisplayName("несуществующий код — трактуется как номер страницы")
  void unknownCodeIsPage() {
    command.handle(message(CommandType.REGION, "2"));

    verify(draftEditor).update(eq(USER), any(), any());
  }

  @Test
  @DisplayName("код вне справочника — показывается страница (с нормализацией к первой)")
  void codeOutsideDirectoryShowsPage() {
    command.handle(message(CommandType.REGION, "999"));

    verify(draftEditor, never()).update(anyLong(), any(), any());
    verify(screens).regionPage(USER, 999);
  }

  @Test
  @DisplayName("нечисловой аргумент — первая страница")
  void nonNumericArgument() {
    command.handle(message(CommandType.REGION, "abc"));

    verify(screens).regionPage(USER, 1);
  }

  @Test
  void type() {
    assertThat(command.getType()).isEqualTo(CommandType.REGION);
  }
}
