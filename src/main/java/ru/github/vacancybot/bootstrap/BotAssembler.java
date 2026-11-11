package ru.github.vacancybot.bootstrap;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.github.vacancybot.bot.LongPollClient;
import ru.github.vacancybot.bot.VkBot;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.client.TrudvsemApiClient;
import ru.github.vacancybot.command.BotCommand;
import ru.github.vacancybot.command.CommandDispatcher;
import ru.github.vacancybot.command.DispatchPolicy;
import ru.github.vacancybot.command.FilterCommands;
import ru.github.vacancybot.command.MessageParser;
import ru.github.vacancybot.command.TaskIdCommands;
import ru.github.vacancybot.command.impl.CurrentTasksCommand;
import ru.github.vacancybot.command.impl.DefaultCommand;
import ru.github.vacancybot.command.impl.DeleteTaskCommand;
import ru.github.vacancybot.command.impl.DoneCommand;
import ru.github.vacancybot.command.impl.KeywordCommand;
import ru.github.vacancybot.command.impl.MinExpCommand;
import ru.github.vacancybot.command.impl.MinZpCommand;
import ru.github.vacancybot.command.impl.NewTaskCommand;
import ru.github.vacancybot.command.impl.NextCommand;
import ru.github.vacancybot.command.impl.RegionCommand;
import ru.github.vacancybot.command.impl.ShowMenuCommand;
import ru.github.vacancybot.command.impl.StartCommand;
import ru.github.vacancybot.command.impl.StopCommand;
import ru.github.vacancybot.command.impl.UpdateTaskCommand;
import ru.github.vacancybot.command.support.BotScreens;
import ru.github.vacancybot.command.support.DraftEditor;
import ru.github.vacancybot.command.support.DynamicHandlerRegistrar;
import ru.github.vacancybot.config.AppConfig;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.repository.JsonTaskRepository;
import ru.github.vacancybot.repository.JsonUserRepository;
import ru.github.vacancybot.repository.JsonVacancyRepository;
import ru.github.vacancybot.repository.TaskRepository;
import ru.github.vacancybot.repository.UserRepository;
import ru.github.vacancybot.repository.VacancyRepository;
import ru.github.vacancybot.service.NotificationService;
import ru.github.vacancybot.service.NotificationServiceImpl;
import ru.github.vacancybot.service.SchedulerService;
import ru.github.vacancybot.service.SchedulerServiceImpl;
import ru.github.vacancybot.service.SearchLauncher;
import ru.github.vacancybot.service.SearchLauncherImpl;
import ru.github.vacancybot.service.TaskService;
import ru.github.vacancybot.service.TaskServiceImpl;
import ru.github.vacancybot.service.UserService;
import ru.github.vacancybot.service.UserServiceImpl;
import ru.github.vacancybot.service.VacancyService;
import ru.github.vacancybot.service.VacancyServiceImpl;
import ru.github.vacancybot.service.vacancy.VacancyParser;

@Slf4j
@RequiredArgsConstructor
public class BotAssembler {

  private final AppConfig config;

  public VkBot assemble() {
    Repositories repositories = createRepositories();
    VkContext vk = createVkContext();
    Services services = createServices(repositories, vk.sender());
    CommandDispatcher dispatcher = createDispatcher(services, vk.sender());
    restoreScheduledUpdates(services);
    registerShutdownHook(services.scheduler());
    LongPollClient longPollClient =
        new LongPollClient(vk.client(), vk.actor(), config.getVkGroupId());
    return new VkBot(longPollClient, dispatcher, new MessageParser());
  }

  private Repositories createRepositories() {
    String dataDir = config.getDataDir();
    return new Repositories(
        new JsonUserRepository(dataDir),
        new JsonTaskRepository(dataDir),
        new JsonVacancyRepository(dataDir));
  }

  private VkContext createVkContext() {
    TransportClient transportClient = HttpTransportClient.getInstance();
    VkApiClient vk = new VkApiClient(transportClient);
    GroupActor actor = new GroupActor(config.getVkGroupId(), config.getVkGroupToken());
    return new VkContext(vk, actor, new VkMessageSender(vk, actor));
  }

  private Services createServices(Repositories repos, VkMessageSender sender) {
    UserService users = new UserServiceImpl(repos.users());
    TaskService tasks = new TaskServiceImpl(repos.tasks());
    VacancyService vacancies = new VacancyServiceImpl(
        new TrudvsemApiClient(), new VacancyParser(), repos.vacancies());
    SchedulerService scheduler = new SchedulerServiceImpl();
    NotificationService notifications = new NotificationServiceImpl(vacancies, sender);
    SearchLauncher launcher = new SearchLauncherImpl(vacancies, scheduler, notifications, sender,
        config.getNotificationPeriodHours(), config.getSchedulerInitialDelayMinutes());
    return new Services(users, tasks, vacancies, scheduler, launcher);
  }

  private CommandDispatcher createDispatcher(Services s, VkMessageSender sender) {
    BotScreens screens = new BotScreens(sender, s.users());
    DraftEditor editor = new DraftEditor(s.users(), s.tasks(), screens);
    FilterCommands filters = createFilterCommands(sender, s.users(), editor, screens);
    TaskIdCommands taskIdCommands = createTaskIdCommands(s, sender, screens);
    DefaultCommand defaultCommand = new DefaultCommand(sender, s.users(), screens,
        filters.keyword(), taskIdCommands.delete(), taskIdCommands.update());
    CommandDispatcher dispatcher =
        new CommandDispatcher(defaultCommand, s.users(), new DispatchPolicy());
    filters.all().forEach(dispatcher::register);
    taskIdCommands.all().forEach(dispatcher::register);
    createMenuCommands(s, sender, screens, editor).forEach(dispatcher::register);
    new DynamicHandlerRegistrar(s.users(), editor, screens, filters, taskIdCommands)
        .registerAll(dispatcher);
    return dispatcher;
  }

  private FilterCommands createFilterCommands(VkMessageSender sender, UserService users,
      DraftEditor editor, BotScreens screens) {
    return new FilterCommands(
        new RegionCommand(users, editor, screens),
        new MinExpCommand(sender, users, editor),
        new MinZpCommand(sender, users, editor),
        new KeywordCommand(sender, users, editor, screens));
  }

  private TaskIdCommands createTaskIdCommands(Services s, VkMessageSender sender,
      BotScreens screens) {
    return new TaskIdCommands(
        new DeleteTaskCommand(sender, s.users(), s.tasks(), s.vacancies(), s.scheduler(), screens),
        new UpdateTaskCommand(sender, s.users(), s.tasks(), screens));
  }

  private List<BotCommand> createMenuCommands(Services s, VkMessageSender sender,
      BotScreens screens, DraftEditor editor) {
    return List.of(
        new StartCommand(screens),
        new ShowMenuCommand(editor, screens),
        new NewTaskCommand(s.users(), s.tasks(), screens),
        new CurrentTasksCommand(sender, s.tasks()),
        new DoneCommand(sender, s.users(), s.tasks(), editor, s.launcher(),
            config.getNotificationPeriodHours()),
        new NextCommand(screens),
        new StopCommand(sender, s.users(), s.scheduler()));
  }

  private void restoreScheduledUpdates(Services s) {
    List<SearchTask> allTasks = s.tasks().findAll();
    log.info("Restoring scheduler tasks. Tasks in the storage: {}", allTasks.size());
    allTasks.forEach(s.launcher()::scheduleUpdates);
  }

  private void registerShutdownHook(SchedulerService scheduler) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Application shutdown...");
      scheduler.shutdown();
    }, "shutdown-hook"));
  }

  private record Repositories(UserRepository users, TaskRepository tasks,
                              VacancyRepository vacancies) {}

  private record VkContext(VkApiClient client, GroupActor actor, VkMessageSender sender) {}

  private record Services(UserService users, TaskService tasks, VacancyService vacancies,
                          SchedulerService scheduler, SearchLauncher launcher) {}
}
