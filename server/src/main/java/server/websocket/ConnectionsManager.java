package server.websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsManager {
    public final ConcurrentHashMap<Integer, ArrayList<Session>> connections = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Session, Integer> sessionToGame = new ConcurrentHashMap<>(); // for removal

    public void add(Session session, int gameID) {
        System.out.println("Adding connection to session: " + session.getRemoteAddress());
        connections.putIfAbsent(gameID, new ArrayList<>());
        connections.get(gameID).add(session);
        sessionToGame.put(session, gameID);
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
        System.out.println("Broadcasting to game " + gameID);
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
