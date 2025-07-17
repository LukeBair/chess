package chess;

import java.util.Objects;

/**
 * Represents moving a chess piece on a chessboard
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessMove {
    private final ChessPosition startPos, endPos;
    private final ChessPiece.PieceType promotionPiece;

    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece) {
        this.startPos = startPosition;
        this.endPos = endPosition;
        this.promotionPiece = promotionPiece;
    }

    /**
     * @return ChessPosition of starting location
     */
    public ChessPosition getStartPosition() {
        return startPos;
    }

    /**
     * @return ChessPosition of ending location
     */
    public ChessPosition getEndPosition() {
        return endPos;
    }

    /**
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this
     * chess move
     *
     * @return Type of piece to promote a pawn to, or null if no promotion
     */
    public ChessPiece.PieceType getPromotionPiece() {
        return promotionPiece;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != ChessMove.class) return false;

        boolean samePos = ((ChessMove)obj).startPos.equals(startPos) && ((ChessMove)obj).endPos.equals(endPos);
        boolean samePromo = (((ChessMove)obj).promotionPiece != null && promotionPiece != null && ((ChessMove)obj).promotionPiece == promotionPiece) || (((ChessMove)obj).promotionPiece == null && promotionPiece == null);
        return samePos && samePromo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPos, endPos, promotionPiece);
    }
}
