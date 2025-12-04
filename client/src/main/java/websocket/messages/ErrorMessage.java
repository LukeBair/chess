package websocket.messages;

import server.Server;

public class ErrorMessage extends ServerMessage {

    public ErrorMessage(ServerMessageType type) {
        super(type);
    }
}
