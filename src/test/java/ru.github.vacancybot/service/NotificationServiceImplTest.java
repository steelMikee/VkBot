package ru.github.vacancybot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.github.vacancybot.bot.VkMessageSender;
import ru.github.vacancybot.exception.VacancyApiException;
import ru.github.vacancybot.model.SearchTask;
import ru.github.vacancybot.model.Vacancy;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl")
class NotificationServiceImplTest {

  @Mock VacancyService vacancyService;
  @Mock VkMessageSender sender;
  @InjectMocks NotificationServiceImpl service;

  private final SearchTask task = SearchTask.builder().id("t").ownerId(5L).keyword("java")
      .regionName("Москва").salaryMax(-1).build();

  @Test
  @DisplayName("нет вакансий — одно короткое сообщение")
  void noVacancies() {
    when(vacancyService.fetchAndSave(task)).thenReturn(List.of());

    service.notifyUser(task);

    verify(sender).sendText(5L, "🔔 По задаче «java, Москва» новых вакансий не найдено.");
    verify(sender, never()).sendChunked(anyLong(), anyString(), any());
  }

  @Test
  @DisplayName("есть вакансии — заголовок + подробные сниппеты через sendChunked")
  @SuppressWarnings("unchecked")
  void withVacancies() {
    Vacancy v = new Vacancy("Dev", 100, 200, "http://v", "Corp", "Москва");
    when(vacancyService.fetchAndSave(task)).thenReturn(List.of(v));

    service.notifyUser(task);

    ArgumentCaptor<List<String>> entries = ArgumentCaptor.forClass(List.class);
    verify(sender).sendChunked(eq(5L), eq("🔔 Обновление по задаче «java, Москва»\nНайдено вакансий: 1\n\n"), entries.capture());
    assertThat(entries.getValue()).hasSize(1);
    assertThat(entries.getValue().get(0)).contains("📌 Dev", "от 100 до 200 руб.", "http://v");
  }

  @Test
  @DisplayName("ошибка API не пробрасывается в планировщик")
  void apiErrorSwallowed() {
    when(vacancyService.fetchAndSave(task)).thenThrow(new VacancyApiException("down"));

    assertThatCode(() -> service.notifyUser(task)).doesNotThrowAnyException();
    verify(sender, never()).sendText(anyLong(), anyString());
  }
}
