package server.Req_Res;

import java.util.List;

public record ListGamesResponse(List<GameListEntry> games) {}