package websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import websocket.commands.ConnectCommand;

import java.io.IOException;

public class WebSocketManager implements WebSocketListener {
    private final int gameID;
    private final String authToken;
    private final boolean isPlayer;
    private Session session;

    public WebSocketManager(int gameID, String authToken, boolean isPlayer) {
        this.gameID = gameID;
        this.authToken = authToken;
        this.isPlayer = isPlayer;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        ConnectCommand cmd = new ConnectCommand(authToken, gameID);

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
//        currentGame = null;

    }
}