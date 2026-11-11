package ru.github.vacancybot.dto;

import ru.github.vacancybot.command.CommandType;

public record ParsedCommand(CommandType type, String argument) {}
