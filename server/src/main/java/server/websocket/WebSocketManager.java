package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.websocket.*;
import models.AuthData;
import models.GameData;
import org.eclipse.jetty.websocket.api.Session;
import websocket.commands.*;
import websocket.messages.*;
import service.GameService;
import service.UserService;

import java.io.IOException;

public class WebSocketManager implements WsConnectHandler, WsMessageHandler, WsCloseHandler {

    private final ConnectionsManager connections = new ConnectionsManager();
    private final GameService gameService;
    private final UserService userService;
    private final Gson gson = new Gson();

    public WebSocketManager(GameService gameService, UserService userService) {
        this.gameService = gameService;
        this.userService = userService;
    }

    @Override
    public void handleConnect(WsConnectContext ctx) {
        System.out.println("Websocket connected");
        ctx.enableAutomaticPings();
    }

    @Override
    public void handleMessage(WsMessageContext ctx) {
        try {
            // First, deserialize to get the command type
            UserGameCommand baseCommand = gson.fromJson(ctx.message(), UserGameCommand.class);

            // Authenticate the user
            String username = validateAuth(baseCommand.getAuthToken());
            if (username == null) {
                sendError(ctx.session, "Error: Invalid authentication");
                return;
            }

            // Route to appropriate handler based on command type
            switch (baseCommand.getCommandType()) {
                case CONNECT -> handleConnect(baseCommand, username, ctx.session);
                case MAKE_MOVE -> handleMakeMove(ctx.message(), username, ctx.session);
                case LEAVE -> handleLeave(baseCommand, username, ctx.session);
                case RESIGN -> handleResign(baseCommand, username, ctx.session);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            sendError(ctx.session, "Error: " + ex.getMessage());
        }
    }

    @Override
    public void handleClose(WsCloseContext ctx) {
        System.out.println("Websocket closed");
        connections.remove(ctx.session);
    }

    private String validateAuth(String authToken) {
        try {
            var authData = userService.getAuthByToken(authToken);
            return authData != null ? authData.username() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void handleConnect(UserGameCommand command, String username, Session session) throws IOException {
        int gameID = command.getGameID();
        connections.add(session, gameID);

        try {
            var game = gameService.getGame(gameID);
            String role = determineRole(game, username);

            LoadGameMessage loadMsg = new LoadGameMessage(username, role, gameID, game.game());
            connections.sendToSession(session, loadMsg);

            NotificationMessage notification = new NotificationMessage(
                    username +
                            (role.equalsIgnoreCase("black") || role.equalsIgnoreCase("white") ?
                                    " is playing as " : "joined as ") + role
            );
            connections.broadcastToGame(gameID, session, notification);
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMakeMove(String message, String username, Session session) throws IOException {
        MakeMoveCommand moveCommand = gson.fromJson(message, MakeMoveCommand.class);
        int gameID = moveCommand.getGameID();

        try {
            var updatedGame = gameService.makeMove(
                    gameID,
                    moveCommand.getChessMove(),
                    username
            );

            MakeMoveMessage loadMsg = new MakeMoveMessage(updatedGame.game());
            connections.broadcastToGame(gameID, null, loadMsg); // null = send to everyone

            NotificationMessage moveNotif = new NotificationMessage(
                    username + " moved " + formatMove(moveCommand.getChessMove())
            );
            connections.broadcastToGame(gameID, session, moveNotif);

            ChessGame.TeamColor opponentColor = updatedGame.game().getTeamTurn(); // next player to move

            boolean isCheckmate = updatedGame.game().isInCheckmate(opponentColor);
            boolean isStalemate = updatedGame.game().isInStalemate(opponentColor);

            if (isCheckmate) {
                String winner = opponentColor == ChessGame.TeamColor.WHITE ? "Black" : "White";
                NotificationMessage checkmateNotif = new NotificationMessage(
                        "Checkmate! " + winner + " wins!"
                );
                connections.broadcastToGame(gameID, null, checkmateNotif);
            } else if (isStalemate) {
                NotificationMessage stalemateNotif = new NotificationMessage(
                        "Stalemate! Game is a draw."
                );
                connections.broadcastToGame(gameID, null, stalemateNotif);
            } else if (updatedGame.game().isInCheck(opponentColor)) {
                NotificationMessage checkNotif = new NotificationMessage("Check!");
                connections.broadcastToGame(gameID, null, checkNotif);
            }

        } catch (Exception e) {
            // Send error only to the client who made the invalid move
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleLeave(UserGameCommand command, String username, Session session) throws IOException {
        int gameID = command.getGameID();
        NotificationMessage notification;

        // Update game in database (remove player if they're playing)
        try {
            gameService.leaveGame(gameID, username);
            notification = new NotificationMessage(
                    username + " left the game"
            );
        } catch (DataAccessException e) {
            // howd u get here
            notification = new NotificationMessage("failed to leave game: " + e.getMessage());
        }
        connections.remove(session);

        connections.broadcastToGame(gameID, session, notification);
    }

    private void handleResign(UserGameCommand command, String username, Session session) throws IOException {
        int gameID = command.getGameID();

        NotificationMessage notification;
        try {
            gameService.resignGame(gameID, username);
            notification = new NotificationMessage(
                    username + " resigned. Game over."
            );
        } catch (DataAccessException | InvalidMoveException e) {
            // TODO: prolly make nicer
            notification = new NotificationMessage("Resign failed: " + e.getMessage());
        }

        connections.broadcastToGame(gameID, null, notification);
    }

    private void sendError(Session session, String errorMessage) {
        try {
            ErrorMessage error = new ErrorMessage(500, errorMessage);
            connections.sendToSession(session, error);
        } catch (IOException e) {
            System.out.println("Failed to send error: " + e.getMessage());
        }
    }

    private String determineRole(GameData game, String username) {
        if (game.blackUsername() != null && game.blackUsername().equalsIgnoreCase(username)) {
            return "black";
        } else if (game.whiteUsername() != null && game.whiteUsername().equalsIgnoreCase(username)) {
            return "white";
        } else {
            return "observer";
        }
    }
    private String formatMove(ChessMove move) {
        return move.toString();
    }
}