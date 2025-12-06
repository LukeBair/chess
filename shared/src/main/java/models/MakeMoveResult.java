package models;

import chess.ChessBoard;

public record MakeMoveResult(ChessBoard result, Status status) {
    public enum Status {
        SUCCESS,
        FAILED_INVALID_AUTH,
        FAILED_INVALID_MOVE,
        FAILED_INVALID_GAME,
        FAILED
    }
}
