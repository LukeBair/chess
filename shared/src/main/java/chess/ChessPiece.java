package chess;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    private final ChessGame.TeamColor pieceColor;
    private final ChessPiece.PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        int row, col;
        row = myPosition.getRow();
        col = myPosition.getColumn();
        Collection<ChessMove> moves = new ArrayList<>();
        switch (type) {
            case KING:
                if (row - 1 >= 0 && col - 1 >= 0)
                    moves.add(new ChessMove(myPosition, new ChessPosition(row - 1, col - 1), PieceType.KING));
                if (row - 1 >= 0)
                    moves.add(new ChessMove(myPosition, new ChessPosition(row - 1, col), PieceType.KING));
                if (row - 1 >= 0 && col + 1 <= 7)
                    moves.add(new ChessMove(myPosition, new ChessPosition(row - 1, col + 1), PieceType.KING));
                if (col - 1 >= 0)
                    moves.add(new ChessMove(myPosition, new ChessPosition(row, col - 1), PieceType.KING));
                if (col + 1 <= 7)
                    moves.add(new ChessMove(myPosition, new ChessPosition(row, col + 1), PieceType.KING));
                if (row + 1 <= 7 && col - 1 >= 0)
                    moves.add(new ChessMove(myPosition, new ChessPosition(row + 1, col - 1), PieceType.KING));
                if (row + 1 <= 7)
                    moves.add(new ChessMove(myPosition, new ChessPosition(row + 1,  col), PieceType.KING));
                if (row + 1 <= 7 && col + 1 <= 7)
                    moves.add(new ChessMove(myPosition, new ChessPosition(row + 1, col + 1), PieceType.KING));
                break;
            case QUEEN:

                break;
            case PAWN:
                break;
            case ROOK:
                break;
            case KNIGHT:
                break;
            case BISHOP:
                break;
            default:
                return null;
        }
    }
}
