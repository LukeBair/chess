package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.SQLDataAccess;
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
            String gameName = request.gameName();
            if (gameName == null || gameName.trim().isEmpty()) {  // Enforce min length
                return new CreateGameResult(-1, "Error: bad request");
            }

            // Safer ID: UUID hash (positive int, low collision risk)
            int id = Math.abs(UUID.randomUUID().hashCode());

            // Check for collision (rare, but retry if needed)
            while (dao.getGame(id) != null) {
                id = Math.abs(UUID.randomUUID().hashCode());
            }

            GameData game = new GameData(id, null, null, gameName.trim(), new ChessGame());
            dao.createGame(game);
            return new CreateGameResult(id, null);
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
            Integer gameID = request.gameID();
            String playerColor = request.playerColor();
            if (gameID == null || gameID <= 0 || playerColor == null || playerColor.trim().isEmpty()) {
                return new JoinGameResult("Error: bad request");
            }
            GameData game = dao.getGame(gameID);
            if (game == null) {
                return new JoinGameResult("Error: bad request");
            }

            String username = authData.username();
            String colorUpper = playerColor.trim().toUpperCase();

            switch (colorUpper) {
                case "WHITE" -> {
                    if (game.whiteUsername() != null) {
                        return new JoinGameResult("Error: already taken");
                    }
                    GameData updatedGame = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
                    dao.updateGame(updatedGame);
                    return new JoinGameResult(null);
                }
                case "BLACK" -> {
                    if (game.blackUsername() != null) {
                        return new JoinGameResult("Error: already taken");
                    }
                    GameData updatedGame = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
                    dao.updateGame(updatedGame);
                    return new JoinGameResult(null);
                }
                case "UNASSIGNED" -> {
                    // Spectator: Allow if game exists (no slot check for Phase 5; add observer list later)
                    // TODO: If GameData has observers List<String>, add username here
                    return new JoinGameResult(null);  // Success, no update needed
                    // Spectator: Allow if game exists (no slot check for Phase 5; add observer list later)
                    // TODO: If GameData has observers List<String>, add username here
                }
                default -> {
                    return new JoinGameResult("Error: bad request");
                }
            }
        } catch (DataAccessException e) {
            return new JoinGameResult("Error: " + e.getMessage());
        }
    }
}