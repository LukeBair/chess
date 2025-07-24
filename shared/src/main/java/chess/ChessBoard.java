package chess;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {
    private final ChessPiece[][] spaces = new ChessPiece[8][8];
//    private final HashMap<>

    public ChessBoard() {
        
    }

    public ChessBoard clone() {
       var tmp = new ChessBoard();
       for (int i = 0; i < 8; i++) {
           for (int j = 0; j < 8; j++) {
               tmp.spaces[i][j] = this.spaces[i][j] != null ? new ChessPiece(this.spaces[i][j].getTeamColor(), this.spaces[i][j].getPieceType()) : null;
           }
       }

       return tmp;
    }

    public static ChessBoard copy(ChessBoard board) {
        ChessBoard newBoard = new ChessBoard();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                newBoard.spaces[i][j] = board.spaces[i][j] != null ? new ChessPiece(board.spaces[i][j].getTeamColor(), board.spaces[i][j].getPieceType()) : null;
            }
        }

        return newBoard;
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        spaces[position.getColumn() - 1][position.getRow() - 1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return spaces[position.getColumn() - 1][position.getRow() - 1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        for(int i = 0; i < 8; i++) {
            spaces[i][1] = ChessGame.WHITE_PAWN;
            spaces[i][6] = ChessGame.BLACK_PAWN;
        }

        spaces[0][0] = ChessGame.WHITE_ROOK;
        spaces[1][0] = ChessGame.WHITE_KNIGHT;
        spaces[2][0] = ChessGame.WHITE_BISHOP;
        spaces[3][0] = ChessGame.WHITE_QUEEN;
        spaces[4][0] = ChessGame.WHITE_KING;
        spaces[5][0] = ChessGame.WHITE_BISHOP;
        spaces[6][0] = ChessGame.WHITE_KNIGHT;
        spaces[7][0] = ChessGame.WHITE_ROOK;

        spaces[0][7] = ChessGame.BLACK_ROOK;
        spaces[1][7] = ChessGame.BLACK_KNIGHT;
        spaces[2][7] = ChessGame.BLACK_BISHOP;
        spaces[3][7] = ChessGame.BLACK_QUEEN;
        spaces[4][7] = ChessGame.BLACK_KING;
        spaces[5][7] = ChessGame.BLACK_BISHOP;
        spaces[6][7] = ChessGame.BLACK_KNIGHT;
        spaces[7][7] = ChessGame.BLACK_ROOK;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(spaces);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != ChessBoard.class) return false;
        return Arrays.deepEquals(((ChessBoard) obj).spaces, spaces);
    }
}
