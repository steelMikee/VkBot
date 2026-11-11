package ru.github.vacancybot.command.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandType;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.dto.IncomingMessage;
import ru.github.vacancybot.model.DialogState;
import ru.github.vacancybot.model.EditingRef;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.SearchLauncher;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.KeyboardBuilder;

@RequiredArgsConstructor
public class DoneCommand implements BotCommand {

  private final VkMessageSender sender;
  private final UserService userService;
  private final TaskService taskService;
  private final DraftEditor draftEditor;
  private final SearchLauncher searchLauncher;
  private final long notificationPeriodHours;

  @Override
  public void handle(IncomingMessage message) {
    long userId = message.userId();
    Optional<EditingRef> ref = draftEditor.editingRef(userId);
    if (ref.isEmpty()) {
      sender.sendWithKeyboard(userId, "❌ Нет активной задачи. Создайте заявку сначала.",
          KeyboardBuilder.screen2());
      return;
    }
    Optional<SearchTask> draft = taskService.findById(ref.get().draftId());
    if (draft.isEmpty()) {
      sender.sendWithKeyboard(userId, "❌ Задача не найдена.", KeyboardBuilder.screen2());
      return;
    }
    SearchTask task = resolveTargetTask(ref.get(), draft.get());
    userService.setDialogState(userId, DialogState.SCREEN_2);
    userService.setEditingTaskId(userId, null);
    sender.sendWithKeyboard(userId, acceptedText(), KeyboardBuilder.screen2());
    searchLauncher.launch(task);
  }

  private SearchTask resolveTargetTask(EditingRef ref, SearchTask draft) {
    if (!ref.isUpdate()) {
      return draft;
    }
    Optional<SearchTask> original = taskService.findById(ref.originalId());
    if (original.isEmpty()) {
      return draft;
    }
    SearchTask target = original.get();
    target.copyFiltersFrom(draft);
    taskService.save(target);
    draftEditor.deleteQuietly(ref.draftId());
    return target;
  }

  private String acceptedText() {
    return "⏳ Заявка принята!\n"
        + "Ищу вакансии в фоновом режиме, это займет несколько секунд... "
        + "\nОбновления будут приходить каждые " + notificationPeriodHours + " ч.";
  }

  @Override
  public CommandType getType() {
    return CommandType.DONE;
  }
}
