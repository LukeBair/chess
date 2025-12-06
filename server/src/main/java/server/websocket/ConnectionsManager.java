package server.websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsManager {
    private final ConcurrentHashMap<Integer, ArrayList<Session>> connections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Session, Integer> sessionToGame = new ConcurrentHashMap<>(); // for removal
    private final ConcurrentHashMap<Integer, ChessGame>  gameToConnection = new ConcurrentHashMap<>();

    private final Gson gson = new Gson();
    public void add(Session session, int gameID) {
        connections.putIfAbsent(gameID, new ArrayList<>());
        connections.get(gameID).add(session);
        sessionToGame.put(session, gameID);
        gameToConnection.putIfAbsent(gameID, new ChessGame());
    }

    public void remove(Session session) {
        Integer gameID = sessionToGame.remove(session);
        if (gameID != null) {
            ArrayList<Session> sessions = connections.get(gameID);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    connections.remove(gameID);
                }
            }
        }
    }

    public void broadcastToGame(int gameID, Session excludeSession, ServerMessage message) throws IOException {
        String msg = new Gson().toJson(message);

        ArrayList<Session> sessions = connections.get(gameID);
        if (sessions != null) {
            for (Session session : sessions) {
                if (session.isOpen() && !session.equals(excludeSession)) {
                    session.getRemote().sendString(msg);
                }
            }
        }
    }

    public void sendToSession(Session session, ServerMessage message) throws IOException {
        if (session.isOpen()) {
            String msg = new Gson().toJson(message);
            session.getRemote().sendString(msg);
        }
    }
}
