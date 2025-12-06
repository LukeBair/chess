package websocket.commands;

import chess.ChessMove;

public class MakeMoveCommand extends UserGameCommand {
    private final ChessMove chessMove;

    public MakeMoveCommand(String authToken, Integer gameID, ChessMove chessMove) {
        super(CommandType.MAKE_MOVE, authToken, gameID);

        this.chessMove = chessMove;
    }

    public ChessMove getChessMove() { return chessMove; }
}
