package ru.github.vacancybot.command.impl;

import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.RegionDirectory;
import ru.github.vacancybot.util.RegionDirectory.Region;

@RequiredArgsConstructor
public class RegionCommand implements BotCommand {

  private final UserService userService;
  private final DraftEditor draftEditor;
  private final BotScreens screens;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    String arg = message.parsedCommand().argument();
    userService.setDialogState(userId, DialogState.AWAIT_REGION);
    if (arg != null && arg.matches("\\d{1,3}")) {
      Region region = RegionDirectory.findByCode(Integer.parseInt(arg)).orElse(null);
      if (region != null) {
        applyRegion(userId, region);
        return;
      }
    }
    screens.regionPage(userId, parsePage(arg));
  }

  private void applyRegion(long userId, Region region) {
    draftEditor.update(userId,
        task -> task.setRegion(region.code(), region.name()),
        task -> "✅ Регион выбран: [" + region.code() + "] " + region.name());
  }

  private static int parsePage(String arg) {
    if (arg == null || arg.isBlank()) {
      return 1;
    }
    try {
      return Integer.parseInt(arg.trim());
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  @Override
  public CommandType getType() {
    return CommandType.REGION;
  }
}
