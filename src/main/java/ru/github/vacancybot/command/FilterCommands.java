package ru.github.vacancybot.command;

import java.util.List;
import ru.github.vacancybot.command.impl.KeywordCommand;
import ru.github.vacancybot.command.impl.MinExpCommand;
import ru.github.vacancybot.command.impl.MinZpCommand;
import ru.github.vacancybot.command.impl.RegionCommand;

public record FilterCommands(RegionCommand region, MinExpCommand minExp, MinZpCommand minZp,
                             KeywordCommand keyword) {

  public List<BotCommand> all() {
    return List.of(region, minExp, minZp, keyword);
  }
}
