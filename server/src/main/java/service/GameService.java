package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import models.*;
import chess.ChessGame;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GameService {
    private final DataAccess dao;

    public GameService(DataAccess dao) {
        this.dao = dao;
    }

    public ListGamesResult listGames(String authToken) {
        try {
            if (authToken == null || authToken.trim().isEmpty()) {
                return new ListGamesResult(null, "Error: unauthorized");
            }
            AuthData authData = dao.getAuth(authToken);
            if (authData == null) {
                return new ListGamesResult(null, "Error: unauthorized");
            }
            List<GameData> games = dao.listGames();
            List<GameListEntry> gameList = games.stream()
                    .map(g -> new GameListEntry(g.gameID(), g.whiteUsername(), g.blackUsername(), g.gameName()))
                    .collect(Collectors.toList());
            return new ListGamesResult(gameList, null);
        } catch (DataAccessException e) {
            return new ListGamesResult(null, "Error: " + e.getMessage());
        }
    }

    public CreateGameResult createGame(CreateGameRequest request, String authToken) {
        try {
            if (authToken == null || authToken.trim().isEmpty()) {
                return new CreateGameResult(-1, "Error: unauthorized");
            }
            AuthData authData = dao.getAuth(authToken);
            if (authData == null) {
                return new CreateGameResult(-1, "Error: unauthorized");
            }
            if (request.gameName() == null || request.gameName().trim().isEmpty()) {
                return new CreateGameResult(-1, "Error: bad request");
            }
            int gameID = Math.abs(UUID.randomUUID().hashCode());
            GameData game = new GameData(gameID, null, null, request.gameName(), new ChessGame());
            dao.createGame(game);
            return new CreateGameResult(gameID, null);
        } catch (DataAccessException e) {
            return new CreateGameResult(-1, "Error: " + e.getMessage());
        }
    }

    public JoinGameResult joinGame(JoinGameRequest request, String authToken) {
        try {
            if (authToken == null || authToken.trim().isEmpty()) {
                return new JoinGameResult("Error: unauthorized");
            }
            AuthData authData = dao.getAuth(authToken);
            if (authData == null) {
                return new JoinGameResult("Error: unauthorized");
            }
            if (request.gameID() == null || request.playerColor() == null ||
                    (!request.playerColor().equalsIgnoreCase("WHITE") && !request.playerColor().equalsIgnoreCase("BLACK"))) {
                return new JoinGameResult("Error: bad request");
            }
            GameData game = dao.getGame(request.gameID());
            if (game == null) {
                return new JoinGameResult("Error: bad request");
            }
            String username = authData.username();
            GameData updatedGame = null;
            if (request.playerColor().equalsIgnoreCase("WHITE")) {
                if (game.whiteUsername() != null) {
                    return new JoinGameResult("Error: already taken");
                }
                updatedGame = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
            } else if (request.playerColor().equalsIgnoreCase("BLACK")) {
                if (game.blackUsername() != null) {
                    return new JoinGameResult("Error: already taken");
                }
                updatedGame = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
            }
            if (updatedGame != null) {
                dao.updateGame(updatedGame);
            }
            return new JoinGameResult(null);
        } catch (DataAccessException e) {
            return new JoinGameResult("Error: " + e.getMessage());
        }
    }
}