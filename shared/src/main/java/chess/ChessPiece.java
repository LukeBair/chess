package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    private final ChessGame.TeamColor teamColor;
    private final PieceType pieceType;
    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.teamColor = pieceColor;
        this.pieceType = type;
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
        return teamColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return pieceType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return teamColor == that.teamColor && pieceType == that.pieceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamColor, pieceType);
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        return switch (pieceType) {
            case KING -> kingMoves(board, myPosition);
            case QUEEN -> queenMoves(board, myPosition);
            case BISHOP -> bishopMoves(board, myPosition);
            case KNIGHT -> knightMoves(board, myPosition);
            case ROOK -> rookMoves(board, myPosition);
            case PAWN -> pawnMoves(board, myPosition);
            default -> throw new RuntimeException("Unknown piece type: " + pieceType);
        };
    }

    private Collection<ChessMove> kingMoves(ChessBoard board, ChessPosition myPosition) {
        int[] moveOffsets = {-1, 0, 1};
        ArrayList<ChessMove> moves = new ArrayList<>();

        for (int rowOffset : moveOffsets) {
            for (int colOffset : moveOffsets) {
                int newRow = myPosition.getRow() + rowOffset;
                int newCol = myPosition.getColumn() + colOffset;

                if (newRow < 1 || newRow > 8 || newCol < 1 || newCol > 8) {
                    continue;
                }
                else if (rowOffset == 0 && colOffset == 0) {
                    continue;
                }


                ChessPosition newPosition = new ChessPosition(newRow, newCol);
                ChessPiece pieceAtNewPosition = board.getPiece(newPosition);

                if (pieceAtNewPosition != null) {
                    if (pieceAtNewPosition.getTeamColor() != this.teamColor) {
                        moves.add(new ChessMove(myPosition, newPosition, null));
                    }
                } else {
                    moves.add(new ChessMove(myPosition, newPosition, null));
                }
            }
        }

        return moves;
    }

    private Collection<ChessMove> queenMoves(ChessBoard board, ChessPosition myPosition) {
        var moves = linearMoves(board, myPosition, 1, 0);
        moves.addAll(linearMoves(board, myPosition, -1, 0));
        moves.addAll(linearMoves(board, myPosition, 0, 1));
        moves.addAll(linearMoves(board, myPosition, 0, -1));
        moves.addAll(linearMoves(board, myPosition, 1, 1));
        moves.addAll(linearMoves(board, myPosition, 1, -1));
        moves.addAll(linearMoves(board, myPosition, -1, 1));
        moves.addAll(linearMoves(board, myPosition, -1, -1));

        return moves;
    }

    private Collection<ChessMove> bishopMoves(ChessBoard board, ChessPosition myPosition) {
        var moves = linearMoves(board, myPosition, 1, 1);
        moves.addAll(linearMoves(board, myPosition, -1, 1));
        moves.addAll(linearMoves(board, myPosition, 1, -1));
        moves.addAll(linearMoves(board, myPosition, -1, -1));

        return moves;
    }

    private Collection<ChessMove> knightMoves(ChessBoard board, ChessPosition myPosition) {
        int[][] moveOffsets = {
                {1, 2}, {1, -2}, {-1, 2}, {-1, -2},
                {2, 1}, {2, -1}, {-2, 1}, {-2, -1}};

        ArrayList<ChessMove> moves = new ArrayList<>();

        for (int[] offset : moveOffsets) {
            int newRow = myPosition.getRow() + offset[0];
            int newCol = myPosition.getColumn() + offset[1];

            if (newRow < 1 || newRow > 8 || newCol < 1 || newCol > 8) {
                continue;
            }

            ChessPosition newPosition = new ChessPosition(newRow, newCol);
            ChessPiece pieceAtNewPosition = board.getPiece(newPosition);

            if (pieceAtNewPosition != null) {
                if (pieceAtNewPosition.getTeamColor() != this.teamColor) {
                    moves.add(new ChessMove(myPosition, newPosition, null));
                }
            } else {
                moves.add(new ChessMove(myPosition, newPosition, null));
            }
        }

        return moves;
    }

    private Collection<ChessMove> rookMoves(ChessBoard board, ChessPosition myPosition) {
        var moves = linearMoves(board, myPosition, 1, 0);
        moves.addAll(linearMoves(board, myPosition, -1, 0));
        moves.addAll(linearMoves(board, myPosition, 0, 1));
        moves.addAll(linearMoves(board, myPosition, 0, -1));

        return moves;
    }

    private Collection<ChessMove> pawnMoves(ChessBoard board, ChessPosition myPosition) {
        return null;
    }

    private Collection<ChessMove> linearMoves(ChessBoard board, ChessPosition myPosition, int rowIncrement, int colIncrement) {
        ArrayList<ChessMove> moves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        int newRow = row + rowIncrement;
        int newCol = col + colIncrement;

        while (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <= 8) {
            ChessPosition position = new ChessPosition(newRow, newCol);
            ChessPiece pieceAtPosition = board.getPiece(position);
            if (pieceAtPosition == null) {
                moves.add(new ChessMove(myPosition, position, null));
            } else {
                if (pieceAtPosition.getTeamColor() != this.teamColor) {
                    moves.add(new ChessMove(myPosition, position, null));
                }
                break;
            }

            newRow += rowIncrement;
            newCol += colIncrement;
        }

        return moves;
    }

}
