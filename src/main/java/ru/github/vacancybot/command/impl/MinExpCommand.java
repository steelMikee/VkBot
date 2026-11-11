package ru.github.vacancybot.command.impl;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;
import ru.github.vacancybot.util.TaskFormatter;

@RequiredArgsConstructor
public class MinExpCommand implements BotCommand {

  private final VkMessageSender sender;
  private final UserService userService;
  private final DraftEditor draftEditor;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    String arg = message.parsedCommand().argument();
    userService.setDialogState(userId, DialogState.AWAIT_EXPERIENCE);
    if (arg == null) {
      sender.sendWithKeyboard(userId, "🎓 Выберите минимальный опыт работы:",
          KeyboardBuilder.experienceOptions());
      return;
    }
    applyExp(userId, parseExp(arg));
  }

  public void applyExp(long userId, int years) {
    draftEditor.update(userId,
        task -> task.setExperienceFrom(years),
        task -> "✅ Минимальный опыт: " + TaskFormatter.formatExp(years));
  }

  private static int parseExp(String arg) {
    return switch (arg.trim().toLowerCase(Locale.ROOT)) {
      case "без опыта", "0" -> 0;
      case "1 год", "1" -> 1;
      case "2 года", "2" -> 2;
      default -> 3;
    };
  }

  @Override
  public CommandType getType() {
    return CommandType.MIN_EXP;
  }
}
