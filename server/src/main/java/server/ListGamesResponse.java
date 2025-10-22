package server;

import java.util.List;

public record ListGamesResponse(List<GameListEntry> games) {}