package ru.github.vacancybot.command.support;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.github.vacancybot.exception.TaskNotFoundException;
import ru.github.vacancybot.model.EditingRef;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.util.TaskFormatter;

@Slf4j
@RequiredArgsConstructor
public class DraftEditor {

  private final UserService userService;
  private final TaskService taskService;
  private final BotScreens screens;

  public Optional<EditingRef> editingRef(long userId) {
    return EditingRef.parse(userService.getOrCreate(userId).getEditingTaskId());
  }

  public Optional<SearchTask> findDraft(long userId) {
    return editingRef(userId).flatMap(ref -> taskService.findById(ref.draftId()));
  }

  public void update(long userId, Consumer<SearchTask> change,
      Function<SearchTask, String> confirmation) {
    findDraft(userId).ifPresent(task -> {
      change.accept(task);
      taskService.save(task);
      screens.taskScreen(userId,
          confirmation.apply(task) + "\n\n" + TaskFormatter.taskInfo(task), task);
    });
  }

  public void discardDraft(long userId) {
    editingRef(userId).ifPresent(ref -> {
      deleteQuietly(ref.draftId());
      userService.setEditingTaskId(userId, null);
    });
  }

  public void deleteQuietly(String taskId) {
    try {
      taskService.delete(taskId);
    } catch (TaskNotFoundException e) {
      log.debug("Draft {} already removed", taskId);
    }
  }
}
