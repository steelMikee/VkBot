# ============================================================
# Stage 1: Сборка проекта
# ============================================================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradle.properties ./
COPY gradle gradle
COPY gradlew gradlew.bat ./
COPY src src

RUN chmod +x gradlew
RUN ./gradlew jar --no-daemon

# ============================================================
# Stage 2: Минимальный образ для запуска
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Создаем пользователя и выдаем ему права на рабочую директорию и папку данных
RUN groupadd -r bot && useradd -r -g bot -d /app -s /sbin/nologin bot \
    && mkdir -p /app/data \
    && chown -R bot:bot /app

# Копируем собранный JAR из stage 1
COPY --from=builder --chown=bot:bot /app/build/libs/*.jar app.jar

# Папка для данных (JSON-файлы пользователей, задач, вакансий)
VOLUME /app/data

# Переменные окружения с значениями по умолчанию
ENV DATA_DIR=data
ENV NOTIFICATION_PERIOD_HOURS=24
ENV SCHEDULER_INITIAL_DELAY_MINUTES=1

USER bot

ENTRYPOINT ["java", "-jar", "app.jar"]
