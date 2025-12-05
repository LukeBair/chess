package websocket.messages;

public class LoadGameMessage extends ServerMessage {
    private String visitorName;
    private String role;
    private int gameID;
    private String message;

    public LoadGameMessage(String visitorName, String role, int gameID) {
        super(ServerMessageType.LOAD_GAME);
        this.visitorName = visitorName;
        this.role = role;
        this.gameID = gameID;
        this.message = visitorName + " joined as " + role;
    }

    public String getMessage() {
        return message;
    }

    public int getGameID() {
        return gameID;
    }
}