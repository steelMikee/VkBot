package ru.github.vacancybot.command.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;
import ru.github.vacancybot.util.TaskFormatter;

@ExtendWith(MockitoExtension.class)
@DisplayName("BotScreens")
class BotScreensTest {

  @Mock VkMessageSender sender;
  @Mock UserService userService;
  @InjectMocks BotScreens screens;

  @Test
  @DisplayName("mainMenu — состояние SCREEN_2 и клавиатура экрана 2")
  void mainMenu() {
    screens.mainMenu(1L, "hi");

    verify(userService).setDialogState(1L, DialogState.SCREEN_2);
    verify(sender).sendWithKeyboard(1L, "hi", KeyboardBuilder.screen2());
  }

  @Test
  @DisplayName("taskScreen — состояние CREATING_TASK и клавиатура экрана 3")
  void taskScreen() {
    SearchTask task = SearchTask.builder().keyword("").salaryMax(-1).build();

    screens.taskScreen(1L, "t", task);

    verify(userService).setDialogState(1L, DialogState.CREATING_TASK);
    verify(sender).sendWithKeyboard(1L, "t", KeyboardBuilder.screen3(task));
  }

  @Test
  @DisplayName("regionPage(1) — полная подсказка")
  void firstPageHasPrompt() {
    screens.regionPage(1L, 1);

    verify(sender).sendWithKeyboard(eq(1L), eq(TaskFormatter.regionPrompt()), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("regionPage(3) — только короткий заголовок с номером страницы")
  void nextPagesHaveShortHeader() {
    screens.regionPage(1L, 3);

    ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
    verify(sender).sendWithKeyboard(eq(1L), text.capture(), org.mockito.ArgumentMatchers.anyString());
    assertThat(text.getValue()).isEqualTo(TaskFormatter.regionPageHeader(3))
        .doesNotContain("Примеры ввода");
  }

  @Test
  @DisplayName("regionPage(999) — выход за границы приводит к первой странице с подсказкой")
  void outOfRangeFallsBackToFirst() {
    screens.regionPage(1L, 999);

    verify(sender).sendWithKeyboard(eq(1L), eq(TaskFormatter.regionPrompt()), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("regionPageWithText — произвольный текст, без смены состояния")
  void regionPageWithText() {
    screens.regionPageWithText(1L, 1, "custom");

    verify(sender).sendWithKeyboard(eq(1L), eq("custom"), org.mockito.ArgumentMatchers.anyString());
    org.mockito.Mockito.verifyNoInteractions(userService);
  }
}
