package models;

public record ListGamesResult(java.util.List<GameListEntry> games, String message) {} // message null on success
