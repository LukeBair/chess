package chess;

import jdk.jshell.spi.ExecutionControl;

import java.lang.reflect.Array;
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

    private final ChessGame.TeamColor color;
    private final ChessPiece.PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.color = pieceColor;
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
        return color;
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
        switch (type) {
            case PAWN:
                return pawnMoves(board, myPosition);

            case ROOK:
                return rookMoves(board, myPosition);

            case KNIGHT:
                return knightMoves(board, myPosition);

            case BISHOP:
                return bishopMoves(board, myPosition);

            case KING:
                return kingMoves(board, myPosition);

            case QUEEN:
                return queenMoves(board, myPosition);
            default:
                return null;
        }
    }

    private Collection<ChessMove> pawnMoves(ChessBoard board, ChessPosition pos) {
        ArrayList<ChessMove> moves = new ArrayList<>();

        int dir = board.getPiece(pos).color == ChessGame.TeamColor.BLACK ? -1 : 1;
        ChessPosition newPos = new ChessPosition(pos.getRow() + dir, pos.getColumn());

        if(!outOfBounds(newPos) && isEmpty(board.getPiece(newPos))) {
//            moves.add(new ChessMove(pos, newPos, null));

            moves.addAll(addPromos(pos, newPos));

            //dir doesnt matter cause the 2nd move on the other side of the board will be out of bounds
            if(pos.getRow() == 2 || pos.getRow() == 7) {
                ChessPosition newPos2 = new ChessPosition(pos.getRow() + (dir * 2), pos.getColumn());
                if(!outOfBounds(newPos2) && isEmpty(board.getPiece(newPos2)))
                    moves.add(new ChessMove(pos, newPos2, null));
            }
        }

        ChessPosition diagonal1 = new ChessPosition(pos.getRow() + dir, pos.getColumn() + 1);
        ChessPosition diagonal2 = new ChessPosition(pos.getRow() + dir,pos.getColumn() - 1);

        if(!outOfBounds(diagonal1) && hasEnemy(board.getPiece(pos), board.getPiece(diagonal1))) {
            moves.addAll(addPromos(pos, diagonal1));
        }

        if(!outOfBounds(diagonal2) && hasEnemy(board.getPiece(pos), board.getPiece(diagonal2))) {
            moves.addAll(addPromos(pos, diagonal2));
        }
        return moves;
    }

    private Collection<ChessMove> addPromos(ChessPosition current, ChessPosition newPos) {
        ArrayList<ChessMove> moves = new ArrayList<>();

        if(newPos.getRow() == 8 || newPos.getRow() == 1) {
            moves.add(new ChessMove(current, newPos, PieceType.QUEEN));
            moves.add(new ChessMove(current, newPos, PieceType.KNIGHT));
            moves.add(new ChessMove(current, newPos, PieceType.BISHOP));
            moves.add(new ChessMove(current, newPos, PieceType.ROOK));
        } else {
            moves.add(new ChessMove(current,  newPos, null));

        }
        return moves;
    }

    private Collection<ChessMove> rookMoves(ChessBoard board, ChessPosition pos) {
        ArrayList<ChessMove> moves = new ArrayList<>();

        //left of rook
        for(int i = 1; i < 8; i++) {
            ChessPosition newPos = new ChessPosition(pos.getRow() - i, pos.getColumn());
            if(outOfBounds(newPos)) break;
            if(emptyOrEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                moves.add(new ChessMove(pos, newPos, null));
                if(hasEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                    break;
                }
            }
            else {
                break;
            }
        }
        //right of rook
        for(int i = 1; i < 8; i++) {
            ChessPosition newPos = new ChessPosition(pos.getRow() + i, pos.getColumn());
            if(outOfBounds(newPos)) break;
            if(emptyOrEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                moves.add(new ChessMove(pos, newPos, null));
                if(hasEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                    break;
                }
            }
            else {
                break;
            }
        }

        //above rook
        for(int i = 1; i < 8; i++) {
            ChessPosition newPos = new ChessPosition(pos.getRow(), pos.getColumn() - i);
            if(outOfBounds(newPos)) break;
            if(emptyOrEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                moves.add(new ChessMove(pos, newPos, null));
                if(hasEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                    break;
                }
            }
            else {
                break;
            }
        }

        //left of rook
        for(int i = 1; i < 8; i++) {
            ChessPosition newPos = new ChessPosition(pos.getRow(), pos.getColumn() + i);
            if(outOfBounds(newPos)) break;
            if(emptyOrEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                moves.add(new ChessMove(pos, newPos, null));
                if(hasEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                    break;
                }
            }
            else {
                break;
            }
        }

        return moves;
    }

    private Collection<ChessMove> knightMoves(ChessBoard board, ChessPosition pos) {
        ArrayList<ChessMove> moves = new ArrayList<>();
        final int[][] knightMoves = {{-1, -2}, {1, -2}, {-2, -1}, {2, -1}, {-2, 1}, {2, 1}, {-1, 2}, {1, 2}};
        for(var move : knightMoves) {
            ChessPosition newPos = new ChessPosition(pos.getRow() + move[1], pos.getColumn() + move[0]);
            if(!outOfBounds(newPos) && emptyOrEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                moves.add(new ChessMove(pos, newPos, null));
            }
        }
        return moves;
    }

    private Collection<ChessMove> bishopMoves(ChessBoard board, ChessPosition pos) {
        ArrayList<ChessMove> moves = new ArrayList<>();

        //upleft of bishop
        for(int i = 1; i < 8; i++) {
            ChessPosition newPos = new ChessPosition(pos.getRow() - i, pos.getColumn() - i);
            if(outOfBounds(newPos)) break;
            if(emptyOrEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                moves.add(new ChessMove(pos, newPos, null));
                if(hasEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                    break;
                }
            }
            else {
                break;
            }
        }
        //upright of bishop
        for(int i = 1; i < 8; i++) {
            ChessPosition newPos = new ChessPosition(pos.getRow() - i, pos.getColumn() + i);
            if(outOfBounds(newPos)) break;
            if(emptyOrEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                moves.add(new ChessMove(pos, newPos, null));
                if(hasEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                    break;
                }
            }
            else {
                break;
            }
        }
        //downleft bishop
        for(int i = 1; i < 8; i++) {
            ChessPosition newPos = new ChessPosition(pos.getRow() + i, pos.getColumn() - i);
            if(outOfBounds(newPos)) break;
            if(emptyOrEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                moves.add(new ChessMove(pos, newPos, null));
                if(hasEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                    break;
                }
            }
            else {
                break;
            }
        }
        //downright of bishop
        for(int i = 1; i < 8; i++) {
            ChessPosition newPos = new ChessPosition(pos.getRow()+i, pos.getColumn() + i);
            if(outOfBounds(newPos)) break;
            if(emptyOrEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                moves.add(new ChessMove(pos, newPos, null));
                if(hasEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                    break;
                }
            }
            else {
                break;
            }
        }

        return moves;
    }

    private Collection<ChessMove> kingMoves(ChessBoard board, ChessPosition pos) {
        ArrayList<ChessMove> moves = new ArrayList<>();
        final int[][] kingMoves = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
        for(var move : kingMoves) {
            ChessPosition newPos = new ChessPosition(pos.getRow() + move[1], pos.getColumn() + move[0]);
            if(!outOfBounds(newPos) && emptyOrEnemy(board.getPiece(pos), board.getPiece(newPos))) {
                moves.add(new ChessMove(pos, newPos, null));
            }
        }
        return moves;
    }

    private Collection<ChessMove> queenMoves(ChessBoard board, ChessPosition pos) {
        ArrayList<ChessMove> moves = new ArrayList<>();

        moves.addAll(rookMoves(board, pos));
        moves.addAll(bishopMoves(board, pos));
        return moves;
    }

    private boolean outOfBounds(ChessPosition pos) {
        return pos.getRow() > 8 || pos.getRow() < 1 || pos.getColumn() > 8 || pos.getColumn() < 1;
    }

    private boolean emptyOrEnemy(ChessPiece currentSpace, ChessPiece enemySpace) {
        return isEmpty(enemySpace) || currentSpace.color != enemySpace.color;
    }

    private boolean isEmpty(ChessPiece spaceToCheck) {
        return spaceToCheck == null;
    }

    private boolean hasEnemy(ChessPiece currentSpace, ChessPiece enemySpace) {
        if (enemySpace == null) return false;

        return currentSpace.color != enemySpace.color;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj.getClass() != ChessPiece.class) return false;

        return ((ChessPiece)obj).type == type && ((ChessPiece)obj).color == color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, color);
    }
}
