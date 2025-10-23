package server.Req_Res;

public record ListGamesResult(java.util.List<GameListEntry> games, String message) {} // message null on success
