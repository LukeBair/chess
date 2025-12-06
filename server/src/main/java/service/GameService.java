package service;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import models.*;

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
            if (gameName == null || gameName.trim().isEmpty()) {
                return new CreateGameResult(-1, "Error: bad request");
            }

            int id = Math.abs(UUID.randomUUID().hashCode());
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
                    return new JoinGameResult(null);
                }
                default -> {
                    return new JoinGameResult("Error: bad request");
                }
            }
        } catch (DataAccessException e) {
            return new JoinGameResult("Error: " + e.getMessage());
        }
    }

    // WebSocket Methods

    /**
     * Get a game by ID (for WebSocket handlers)
     */
    public GameData getGame(int gameID) throws DataAccessException {
        return dao.getGame(gameID);
    }

    /**
     * Validate auth token and return username
     */
    public String validateAuthToken(String authToken) throws DataAccessException {
        if (authToken == null || authToken.trim().isEmpty()) {
            return null;
        }
        AuthData authData = dao.getAuth(authToken);
        return authData != null ? authData.username() : null;
    }

    /**
     * Make a move in a game (for WebSocket MAKE_MOVE command)
     */
    public GameData makeMove(int gameID, ChessMove move, String username) throws DataAccessException, InvalidMoveException {
        GameData game = dao.getGame(gameID);
        if (game == null) {
            throw new DataAccessException("Game not found");
        }

        ChessGame chessGame = game.game();

        // Check if game is over
        if (chessGame.isGameOver()) {
            throw new InvalidMoveException("Game is over");
        }

        // Determine which color the user is playing
        ChessGame.TeamColor playerColor = null;
        if (game.whiteUsername() != null && username.equals(game.whiteUsername())) {
            playerColor = ChessGame.TeamColor.WHITE;
        } else if (game.blackUsername() != null && username.equals(game.blackUsername())) {
            playerColor = ChessGame.TeamColor.BLACK;
        } else {
            throw new InvalidMoveException("You are not a player in this game");
        }

        // Check if it's the player's turn
        if (chessGame.getTeamTurn() != playerColor) {
            throw new InvalidMoveException("It's not your turn");
        }

        // Make the move (this will throw InvalidMoveException if invalid)
        chessGame.makeMove(move);

        // Update game in database
        GameData updatedGame = new GameData(game.gameID(), game.whiteUsername(), game.blackUsername(), game.gameName(), chessGame);
        dao.updateGame(updatedGame);

        return updatedGame;
    }

    /**
     * Remove a player from a game (for WebSocket LEAVE command)
     */
    public void leaveGame(int gameID, String username) throws DataAccessException {
        GameData game = dao.getGame(gameID);
        if (game == null) {
            throw new DataAccessException("Game not found");
        }

        String newWhite = game.whiteUsername();
        String newBlack = game.blackUsername();

        if (game.whiteUsername() != null && username.equals(game.whiteUsername())) {
            newWhite = null;
        } else if (game.blackUsername() != null && username.equals(game.blackUsername())) {
            newBlack = null;
        }
        // If observer, no change to game data needed

        GameData updatedGame = new GameData(game.gameID(), newWhite, newBlack, game.gameName(), game.game());
        dao.updateGame(updatedGame);
    }

    /**
     * Mark a game as over due to resignation (for WebSocket RESIGN command)
     */
    public void resignGame(int gameID, String username) throws DataAccessException, InvalidMoveException {
        GameData game = dao.getGame(gameID);
        if (game == null) {
            throw new DataAccessException("Game not found");
        }

        // Check if user is a player (not observer)
        if (game.whiteUsername() != null && (!username.equals(game.whiteUsername())) &&
           (game.blackUsername() != null && !username.equals(game.blackUsername()))) {
            throw new InvalidMoveException("Observers cannot resign");
        }

        ChessGame chessGame = game.game();

        // Check if game is already over
        if (chessGame.isGameOver()) {
            throw new InvalidMoveException("Game is already over");
        }

        // Mark game as over
        chessGame.setGameOver(true);

        // Update game in database
        GameData updatedGame = new GameData(game.gameID(), game.whiteUsername(), game.blackUsername(), game.gameName(), chessGame);
        dao.updateGame(updatedGame);
    }

    public void endGame(int gameID) throws DataAccessException {
        var game = dao.getGame(gameID);
        game.game().setGameOver(true);
        dao.updateGame(game);
    }
}