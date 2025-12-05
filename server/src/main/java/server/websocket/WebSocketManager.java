package server.websocket;

import com.google.gson.Gson;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsCloseHandler;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsConnectHandler;
import io.javalin.websocket.WsMessageContext;
import io.javalin.websocket.WsMessageHandler;
import org.eclipse.jetty.websocket.api.Session;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;

public class WebSocketManager implements WsConnectHandler, WsMessageHandler, WsCloseHandler {

    private final ConnectionsManager connections = new ConnectionsManager();

    @Override
    public void handleConnect(WsConnectContext ctx) {
        System.out.println("Websocket connected");
        ctx.enableAutomaticPings();
    }

    @Override
    public void handleMessage(WsMessageContext ctx) {
        try {
            ServerMessage serverMessage = new Gson().fromJson(ctx.message(), ServerMessage.class);
            switch (serverMessage.getServerMessageType()) {
                case LOAD_GAME -> loadGame(new Gson().fromJson(ctx.message(), LoadGameMessage.class), ctx.session);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void handleClose(WsCloseContext ctx) {
        System.out.println("Websocket closed");
        connections.remove(ctx.session);
    }

    private void loadGame(LoadGameMessage loadGameMessage, Session session) throws IOException {
        int gameID = loadGameMessage.getGameID(); // You need to add this field
        connections.add(session, gameID);

        // Broadcast to everyone else in this game
        connections.broadcastToGame(gameID, session, loadGameMessage);

        // Also send confirmation to the joining player
        connections.sendToSession(session, loadGameMessage);
    }
}