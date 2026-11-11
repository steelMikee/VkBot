package ru.github.vacancybot.command.support;

import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;
import ru.github.vacancybot.util.RegionDirectory;
import ru.github.vacancybot.util.RegionDirectory.RegionPage;
import ru.github.vacancybot.util.TaskFormatter;

@RequiredArgsConstructor
public class BotScreens {

  private final VkMessageSender sender;
  private final UserService userService;

  public void mainMenu(long userId, String text) {
    userService.setDialogState(userId, DialogState.SCREEN_2);
    sender.sendWithKeyboard(userId, text, KeyboardBuilder.screen2());
  }

  public void taskScreen(long userId, String text, SearchTask task) {
    userService.setDialogState(userId, DialogState.CREATING_TASK);
    sender.sendWithKeyboard(userId, text, KeyboardBuilder.screen3(task));
  }

  public void regionPage(long userId, int page) {
    RegionPage regionPage = RegionDirectory.page(page);
    String text = regionPage.page() == 1
        ? TaskFormatter.regionPrompt()
        : TaskFormatter.regionPageHeader(regionPage.page());
    sender.sendWithKeyboard(userId, text, KeyboardBuilder.regionPage(regionPage));
  }

  public void regionPageWithText(long userId, int page, String text) {
    sender.sendWithKeyboard(userId, text, KeyboardBuilder.regionPage(RegionDirectory.page(page)));
  }
}
