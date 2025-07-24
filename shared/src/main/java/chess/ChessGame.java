package chess;

import java.util.Collection;
import java.util.ArrayList;

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
    public final static ChessPiece WHITE_QUEEN = new ChessPiece(TeamColor.WHITE, ChessPiece.PieceType.QUEEN); // Fixed
    public final static ChessPiece WHITE_KING = new ChessPiece(TeamColor.WHITE, ChessPiece.PieceType.KING); // Fixed

    public final static ChessPiece BLACK_PAWN = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.PAWN);
    public final static ChessPiece BLACK_ROOK = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.ROOK);
    public final static ChessPiece BLACK_KNIGHT = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.KNIGHT);
    public final static ChessPiece BLACK_BISHOP = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.BISHOP);
    public final static ChessPiece BLACK_QUEEN = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.QUEEN); // Fixed
    public final static ChessPiece BLACK_KING = new ChessPiece(TeamColor.BLACK, ChessPiece.PieceType.KING); // Fixed

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
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }
        Collection<ChessMove> moves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> valid = new ArrayList<>();
        for (ChessMove move : moves) {
            ChessGame tmp = new ChessGame();
            ChessBoard tempBoard = ChessBoard.copy(board);
            tmp.setBoard(tempBoard);
            tempBoard.addPiece(move.getEndPosition(), piece);
            tempBoard.addPiece(move.getStartPosition(), null);
            if (!tmp.isInCheck(piece.getTeamColor(), tempBoard)) {
                valid.add(move);
            }
        }
        return valid;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPiece piece = board.getPiece(move.getStartPosition());
        if (piece == null || piece.getTeamColor() != currentTeamTurn) {
            throw new InvalidMoveException("Invalid move: No piece or wrong team");
        }
        Collection<ChessMove> validMoves = validMoves(move.getStartPosition());
        if (validMoves == null || !validMoves.contains(move)) {
            throw new InvalidMoveException("Invalid move: Not a valid move");
        }

        if (move.getPromotionPiece() != null && piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            board.addPiece(move.getEndPosition(), new ChessPiece(currentTeamTurn, move.getPromotionPiece()));
        } else {
            board.addPiece(move.getEndPosition(), piece);
        }
        board.addPiece(move.getStartPosition(), null);

        currentTeamTurn = currentTeamTurn == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        return isInCheck(teamColor, board);
    }

    private boolean isInCheck(TeamColor teamColor, ChessBoard board) {
        ChessPosition kingPos = new ChessPosition(0, 0); // it will be set to a valid position later
        ChessPosition[] enemyPositions = new ChessPosition[16];
        int enemyIndex = 0;

        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                var piece = board.getPiece(new ChessPosition(x, y));

                if (piece == null) {
                    continue;
                }
                if (piece.getPieceType() == ChessPiece.PieceType.KING
                        && piece.getTeamColor() == teamColor) {
                    kingPos = new ChessPosition(x, y);
                }
                else if(piece.getTeamColor() != teamColor) {
                    enemyPositions[enemyIndex] = new ChessPosition(x, y);
                    enemyIndex++;
                }
            }
        }

        for(var x : enemyPositions) {
            if(x == null) {
                continue;
            }

            var enemyMoves = board.getPiece(x).pieceMoves(board, x);
            ChessPosition finalKingPos = kingPos;
            if (enemyMoves.stream().anyMatch((move) -> move.getEndPosition().equals(finalKingPos))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }

        return !hasAnyValidMoves(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }

        return !hasAnyValidMoves(teamColor);
    }

    private boolean hasAnyValidMoves(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(col, row);
                ChessPiece piece = board.getPiece(position);

                if (piece == null || piece.getTeamColor() != teamColor) {
                    continue;
                }

                Collection<ChessMove> possibleMoves = piece.pieceMoves(board, position);

                for (ChessMove move : possibleMoves) {
                    ChessBoard tempBoard = ChessBoard.copy(board);

                    tempBoard.addPiece(move.getEndPosition(), piece);
                    tempBoard.addPiece(move.getStartPosition(), null);

                    if (!isInCheck(teamColor, tempBoard)) {
                        return true;
                    }
                }
            }
        }

        return false;
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

    private ChessPiece[][] copyBoard(ChessPiece[][] original) {
        ChessPiece[][] copy = new ChessPiece[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                copy[i][j] = original[i][j];
            }
        }
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return ((ChessGame) obj).getBoard().equals(this.getBoard()) &&
               ((ChessGame) obj).getTeamTurn() == this.getTeamTurn();
    }

    public int hashCode() {
        return board.hashCode() + currentTeamTurn.hashCode();
    }
}