package websocket.messages;

import chess.ChessGame;

public class LoadGameMessage extends ServerMessage {
    private final String visitorName;
    private final String role;
    private final int gameID;
    private final String message;
    private final ChessGame game;

    public LoadGameMessage(String visitorName, String role, int gameID, ChessGame game) {
        super(ServerMessageType.LOAD_GAME);
        this.visitorName = visitorName;
        this.role = role;
        this.gameID = gameID;
        this.message = null;
        this.game = game;
    }

    public String getMessage() {
        return message;
    }

    public int getGameID() {
        return gameID;
    }

    public String getVisitorName() { return visitorName; }
    public String getRole() { return role; }
    public ChessGame getGame() { return game; }

}