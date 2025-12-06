package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private TeamColor currentTeam;
    private ChessBoard chessBoard;

    private ArrayList<ChessPosition> blackPiecesPositions = new ArrayList<>();
    private ArrayList<ChessPosition> whitePiecesPositions = new ArrayList<>();
    private boolean gameOver = false;

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public ChessGame() {
        for (int i = 1; i <= 8; i++) {
            whitePiecesPositions.add(new ChessPosition(2, i));
            whitePiecesPositions.add(new ChessPosition(1, i));
        }
        for (int i = 1; i <= 8; i++) {
            blackPiecesPositions.add(new ChessPosition(7, i));
            blackPiecesPositions.add(new ChessPosition(8, i));
        }

        chessBoard = new ChessBoard();
        chessBoard.resetBoard();

        currentTeam = TeamColor.WHITE;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return currentTeam;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        currentTeam = team;
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
        return validMoves(this.chessBoard, startPosition);
    }

    private Collection<ChessMove> validMoves(ChessBoard board, ChessPosition position) {
        ChessPiece piece = board.getPiece(position);
        if (piece == null) {
            return new ArrayList<>();
        }

        Collection<ChessMove> possibleMoves = piece.pieceMoves(board, position);
        Collection<ChessMove> legalMoves = new ArrayList<>();

        for (ChessMove move : possibleMoves) {
            ChessBoard boardCopy = board.deepCopy();

            ChessPiece movingPiece = boardCopy.getPiece(move.getStartPosition());
            boardCopy.addPiece(move.getEndPosition(), movingPiece);
            boardCopy.addPiece(move.getStartPosition(), null);

            if (!isInCheckOnBoard(boardCopy, piece.getTeamColor())) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPiece pieceToMove = chessBoard.getPiece(move.getStartPosition());

        if (pieceToMove == null) {
            throw new InvalidMoveException("No piece at start position");
        }
        if (pieceToMove.getTeamColor() != currentTeam) {
            throw new InvalidMoveException("Not your piece to move");
        }

        Collection<ChessMove> validMoves = validMoves(move.getStartPosition());
        if (!validMoves.contains(move)) {
            throw new InvalidMoveException("Move is not valid");
        }

        ChessPiece capturedPiece = chessBoard.getPiece(move.getEndPosition());

        ChessPiece finalPiece = move.getPromotionPiece() != null
                ? new ChessPiece(pieceToMove.getTeamColor(), move.getPromotionPiece())
                : pieceToMove;

        chessBoard.addPiece(move.getEndPosition(), finalPiece);
        chessBoard.addPiece(move.getStartPosition(), null);  // FIX: Remove piece from start

        if (pieceToMove.getTeamColor() == TeamColor.WHITE) {
            whitePiecesPositions.remove(move.getStartPosition());
            whitePiecesPositions.add(move.getEndPosition());
            if (capturedPiece != null && capturedPiece.getTeamColor() == TeamColor.BLACK) {
                blackPiecesPositions.remove(move.getEndPosition());
            }
        } else {
            blackPiecesPositions.remove(move.getStartPosition());
            blackPiecesPositions.add(move.getEndPosition());
            if (capturedPiece != null && capturedPiece.getTeamColor() == TeamColor.WHITE) {
                whitePiecesPositions.remove(move.getEndPosition());
            }
        }

        currentTeam = (currentTeam == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }


    public boolean isInCheck(TeamColor teamColor) {
        return isInCheckOnBoard(this.chessBoard, teamColor);
    }

    private boolean isInCheckOnBoard(ChessBoard board, TeamColor teamColor) {
        ChessPosition kingPosition = null;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null &&
                        piece.getTeamColor() == teamColor &&
                        piece.getPieceType() == ChessPiece.PieceType.KING) {
                    kingPosition = pos;
                    break;
                }
            }
            if (kingPosition != null) {
                break;
            }
        }

        if (kingPosition == null) {
            return false;
        }

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                if (piece == null || piece.getTeamColor() == teamColor) {
                    continue;  // Early skip for non-enemies
                }

                // Extracted: Check if this piece can attack king
                if (canPieceAttackKing(piece, pos, board, kingPosition)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean canPieceAttackKing(ChessPiece piece, ChessPosition pos, ChessBoard board, ChessPosition kingPos) {
        return piece.pieceMoves(board, pos)
                .stream()  // Or loop if avoiding streams
                .anyMatch(move -> move.getEndPosition().equals(kingPos));
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

        return hasNoValidMoves(teamColor);
    }

    private boolean hasNoValidMoves(TeamColor teamColor) {
        var friendlyPieces = teamColor == TeamColor.WHITE ? whitePiecesPositions : blackPiecesPositions;

        for (ChessPosition position : friendlyPieces) {
            Collection<ChessMove> moves = validMoves(position);

            if (!moves.isEmpty()) {
                return false;
            }
        }
        return true;
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

        return hasNoValidMoves(teamColor);
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        chessBoard = board;
        whitePiecesPositions = new ArrayList<>();
        blackPiecesPositions = new ArrayList<>();

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = chessBoard.getPiece(pos);

                if (piece != null) {
                    if (piece.getTeamColor() == TeamColor.WHITE) {
                        whitePiecesPositions.add(pos);
                    } else {
                        blackPiecesPositions.add(pos);
                    }
                }
            }
        }
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return chessBoard;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessGame chessGame)) {
            return false;
        }
        return currentTeam == chessGame.currentTeam && Objects.equals(chessBoard, chessGame.chessBoard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentTeam, chessBoard);
    }
}