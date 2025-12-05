package websocket;

import com.google.gson.Gson;
import models.GameData;
import models.GameListEntry;
import models.JoinGameResult;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import ui.Renderer;
import websocket.commands.ConnectCommand;
import websocket.messages.ServerMessage;

import java.io.IOException;

public class WebSocketManager implements WebSocketListener {
    private final GameListEntry gameData;
    private final String authToken;
    private final boolean isPlayer;
    private final Renderer renderer;
    private Session session;

    public WebSocketManager(GameListEntry gameData, String authToken, boolean isPlayer, Renderer rend) {
        this.gameData = gameData;
        this.authToken = authToken;
        this.isPlayer = isPlayer;
        this.renderer = rend;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        ConnectCommand cmd = new ConnectCommand(authToken, gameData.gameID());

        try {
            session.getRemote().sendString(cmd.toJson());
        } catch (IOException e) {
            // TODO: handle
            throw new RuntimeException(e);
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        session = null;
        renderer.enqueueRenderTask("Disconnected");
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        ServerMessage serverMessage = new Gson().fromJson(message, ServerMessage.class);
        switch (serverMessage.getServerMessageType()) {
            case LOAD_GAME -> {

            }
            case ERROR -> {
                System.out.println("wut: ");
            }
            case NOTIFICATION -> {
                System.out.println("wip");
            }
            default -> {
                System.out.println("wut");
            }
        }
    }

    public void ConnectToGame() {
        ConnectCommand cmd = new ConnectCommand(authToken, gameData.gameID());
        session.getRemote().sendString(cmd.toJson());
    }
}