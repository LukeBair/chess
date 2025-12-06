package websocket.messages;

import chess.ChessGame;

public class MakeMoveMessage extends ServerMessage {
    private final ChessGame game;
    public MakeMoveMessage(ChessGame game) {
        super(ServerMessageType.MOVE);

        this.game = game;
    }

    public ChessGame getGame() { return game; }
}
