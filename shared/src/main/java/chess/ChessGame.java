package chess;

import java.util.Collection;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    public final static ChessPiece WHITE_PAWN = new ChessPiece(TeamColor.WHITE, ChessPiece.PieceType.PAWN);
    public final static ChessPiece WHITE_ROOK = new ChessPiece(TeamColor.WHITE, ChessPiece.PieceType.ROOK);
    public final static ChessPiece WHITE_KNIGHT = new ChessPiece(TeamColor.WHITE, ChessPiece.PieceType.KNIGHT);
    public final static ChessPiece WHITE_BISHOP = new ChessPiece(TeamColor.WHITE, ChessPiece.PieceType.BISHOP);
    public final static ChessPiece WHITE_QUEEN = new ChessPiece(TeamColor.WHITE, ChessPiece.PieceType.KING);
    public final static ChessPiece WHITE_KING = new ChessPiece(TeamColor.WHITE, ChessPiece.PieceType.QUEEN);

    public final static ChessPiece BLACK_PAWN = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.PAWN);
    public final static ChessPiece BLACK_ROOK = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.ROOK);
    public final static ChessPiece BLACK_KNIGHT = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.KNIGHT);
    public final static ChessPiece BLACK_BISHOP = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.BISHOP);
    public final static ChessPiece BLACK_QUEEN = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.KING);
    public final static ChessPiece BLACK_KING = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.QUEEN);


    private TeamColor currentTeamTurn = TeamColor.WHITE;
    private ChessBoard board = new ChessBoard();

    public ChessGame() {
        board.resetBoard();
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return currentTeamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        currentTeamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        return board.getPiece(startPosition).pieceMoves(board, startPosition);
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        var validMoves = board.getPiece(move.getStartPosition()).pieceMoves(board, move.getStartPosition());
        boolean isValid = validMoves.stream().anyMatch((i) -> i.getEndPosition().equals(move.getEndPosition()));
        if (!isValid) throw new InvalidMoveException();

        board.addPiece(move.getEndPosition(), board.getPiece(move.getStartPosition()));
        board.addPiece(move.getStartPosition(), null);
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }
}
