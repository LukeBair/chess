package websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import jakarta.websocket.*;
import chess.ChessMove;
import managers.GamePlayManager;
import ui.Renderer;
import websocket.commands.*;
import websocket.messages.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static websocket.messages.ServerMessage.ServerMessageType.LOAD_GAME;

public class WebSocketFacade extends Endpoint {
    Session session;
    private final Renderer renderer;
    private final GamePlayManager gameManager;
    private final Gson gson = new Gson();

    public WebSocketFacade(Renderer renderer, GamePlayManager gameManager) {
        this.renderer = renderer;
        this.gameManager = gameManager;

        try {
            URI socketURI = new URI("ws://localhost:8080/ws");
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, socketURI);

            // Set message handler
            this.session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    handleServerMessage(message);
                }
            });
        } catch (DeploymentException | IOException | URISyntaxException ex) {
            System.out.println("WebSocket connection error: " + ex);
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
    }

    private void handleServerMessage(String message) {
        try {
            ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

            switch (serverMessage.getServerMessageType()) {
                case NOTIFICATION -> {
                    NotificationMessage notif = gson.fromJson(message, NotificationMessage.class);
                    String msg = notif.getMessage();

                    renderer.enqueueRenderTask("\n[NOTIFICATION] " + msg);

                    // Detect game-ending messages
                    if (msg.toLowerCase().contains("resigned") ||
                            msg.toLowerCase().contains("checkmate") ||
                            msg.toLowerCase().contains("stalemate")) {

                        gameManager.setGameOver(msg.contains("resigned")
                                ? msg
                                : "Game Over - " + (msg.contains("Checkmate") ?
                                (msg.contains("Black wins") ? "Black wins!" : "White wins!")
                                : "Draw by stalemate!"));

                        renderer.enqueueRenderTask("\nType 'leave' to return to game list.");
                    }
                }
                case ERROR -> {
                    ErrorMessage error = gson.fromJson(message, ErrorMessage.class);
                    renderer.enqueueRenderTask("\n[ERROR] " + error.getErrorMessage());
                }
                case LOAD_GAME -> {
                    LoadGameMessage loadGame = gson.fromJson(message, LoadGameMessage.class);
                    ChessGame game = loadGame.getGame();

                    if (game != null) {
                        // Update the game in Managers.GameManager and trigger automatic redraw
                        gameManager.setIDIfNotNull(loadGame.getGameID());
                        if (loadGame.getRole() != null) {
                            gameManager.setTeamColor(ChessGame.TeamColor.valueOf(loadGame.getRole().toUpperCase()));
                        }
                        gameManager.updateGame(game);
                    } else {
                        renderer.enqueueRenderTask("\n[ERROR] Received LOAD_GAME with null game");
                    }
                }
            }
        } catch (Exception e) {

            if (e.getMessage().equalsIgnoreCase("No enum constant chess.ChessGame.TeamColor.OBSERVER")) {
                LoadGameMessage loadGame = gson.fromJson(message, LoadGameMessage.class);
                ChessGame game = loadGame.getGame();
                gameManager.setTeamColor(ChessGame.TeamColor.WHITE);
                gameManager.setIsObserver(true);
                gameManager.updateGame(game);
            } else {
                renderer.enqueueRenderTask("\n[ERROR] Failed to process server message: " + e.getMessage());
            }
        }
    }

    // CONNECT command
    public void connect(String authToken, int gameID) {
        try {
            var command = new ConnectCommand(authToken, gameID);
            this.session.getBasicRemote().sendText(gson.toJson(command));
        } catch (IOException ex) {
            System.out.println("Error sending CONNECT: " + ex);
        }
    }

    // MAKE_MOVE command
    public void makeMove(String authToken, int gameID, ChessMove move) {
        try {
            var command = new MakeMoveCommand(authToken, gameID, move);
            this.session.getBasicRemote().sendText(gson.toJson(command));
        } catch (IOException ex) {
            System.out.println("Error sending MAKE_MOVE: " + ex);
        }
    }

    // LEAVE command
    public void leaveGame(String authToken, int gameID) {
        try {
            var command = new LeaveCommand(authToken, gameID);
            this.session.getBasicRemote().sendText(gson.toJson(command));
        } catch (IOException ex) {
            System.out.println("Error sending LEAVE: " + ex);
        }
    }

    // RESIGN command
    public void resign(String authToken, int gameID) {
        try {
            var command = new ResignCommand(authToken, gameID);
            this.session.getBasicRemote().sendText(gson.toJson(command));
        } catch (IOException ex) {
            System.out.println("Error sending RESIGN: " + ex);
        }
    }
}