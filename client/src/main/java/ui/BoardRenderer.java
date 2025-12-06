package ui;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Set;

public class BoardRenderer {
    // Better tan/brown color scheme
    private static final String LIGHT_SQUARE = EscapeSequences.SET_BG_COLOR_TAN;       // Light tan
    private static final String DARK_SQUARE = EscapeSequences.SET_BG_COLOR_LIGHT_GREEN;    // Dark brown
    private static final String HIGHLIGHT_LIGHT = EscapeSequences.SET_BG_COLOR_YELLOW;   // Highlight light square
    private static final String HIGHLIGHT_DARK = EscapeSequences.SET_BG_COLOR_DARK_GREEN; // Highlight dark square
    private static final String RESET_BG = EscapeSequences.RESET_BG_COLOR;
    private static final String WHITE_PIECE = EscapeSequences.SET_TEXT_COLOR_WHITE;      // White pieces
    private static final String BLACK_PIECE = EscapeSequences.SET_TEXT_COLOR_BLACK;      // Black pieces
    private static final String RESET_TEXT = EscapeSequences.RESET_TEXT_COLOR;

    /**
     * Draw the board with optional position highlights
     * @param board The chess board to draw
     * @param viewAs Which side to view from (WHITE = white on bottom, BLACK = black on bottom)
     * @param highlightPositions Positions to highlight (null or empty = no highlights)
     * @return Array of strings representing each line of the board
     */
    public String[] drawBoard(ChessBoard board, ChessGame.TeamColor viewAs, Set<ChessPosition> highlightPositions) {
        String[] lines = new String[10];  // Changed from 20 to 10 (only what we need)

        // Top border (files a-h)
        String topFiles = viewAs == ChessGame.TeamColor.BLACK ? "  h  g  f  e  d  c  b  a  " : "  a  b  c  d  e  f  g  h  ";
        lines[0] = EscapeSequences.SET_TEXT_COLOR_MAGENTA + topFiles + RESET_TEXT;

        // Board rows
        int startRow = viewAs == ChessGame.TeamColor.BLACK ? 1 : 8;
        int endRow = viewAs == ChessGame.TeamColor.BLACK ? 9 : 0;
        int rowStep = viewAs == ChessGame.TeamColor.BLACK ? 1 : -1;
        int lineIdx = 1;

        for (int row = startRow; row != endRow; row += rowStep) {
            StringBuilder rowLine = new StringBuilder(EscapeSequences.SET_TEXT_COLOR_MAGENTA + row + " " + RESET_TEXT);

            // Column iteration (also needs to reverse for black view)
            int startCol = viewAs == ChessGame.TeamColor.BLACK ? 8 : 1;
            int endCol = viewAs == ChessGame.TeamColor.BLACK ? 0 : 9;
            int colStep = viewAs == ChessGame.TeamColor.BLACK ? -1 : 1;

            for (int col = startCol; col != endCol; col += colStep) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);

                boolean isLight = ((row + col) % 2 == 0);
                boolean isHighlighted = highlightPositions != null && highlightPositions.contains(pos);

                // Choose background color
                String bg;
                if (isHighlighted) {
                    bg = isLight ? HIGHLIGHT_LIGHT : HIGHLIGHT_DARK;
                } else {
                    bg = isLight ? LIGHT_SQUARE : DARK_SQUARE;
                }

                if (piece != null) {
                    String symbol = getUnicode(piece);
                    String color = piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_PIECE : BLACK_PIECE;
                    rowLine.append(bg).append(color).append(" ").append(symbol).append(" ").append(RESET_TEXT).append(RESET_BG);
                } else {
                    rowLine.append(bg).append("   ").append(RESET_BG);
                }
            }
            rowLine.append(" ").append(EscapeSequences.SET_TEXT_COLOR_MAGENTA).append(row).append(RESET_TEXT);
            lines[lineIdx++] = rowLine.toString();
        }

        // Bottom border (files a-h)
        String bottomFiles = viewAs == ChessGame.TeamColor.BLACK ? "  h  g  f  e  d  c  b  a  " : "  a  b  c  d  e  f  g  h  ";
        lines[lineIdx] = EscapeSequences.SET_TEXT_COLOR_MAGENTA + bottomFiles + RESET_TEXT;

        return lines;
    }

    private String getUnicode(ChessPiece piece) {
        ChessPiece.PieceType type = piece.getPieceType();
        ChessGame.TeamColor color = piece.getTeamColor();

        if (color == ChessGame.TeamColor.WHITE) {
            return switch (type) {
                case KING -> EscapeSequences.WHITE_KING.trim();
                case QUEEN -> EscapeSequences.WHITE_QUEEN.trim();
                case BISHOP -> EscapeSequences.WHITE_BISHOP.trim();
                case KNIGHT -> EscapeSequences.WHITE_KNIGHT.trim();
                case ROOK -> EscapeSequences.WHITE_ROOK.trim();
                case PAWN -> EscapeSequences.WHITE_PAWN.trim();
            };
        } else {
            return switch (type) {
                case KING -> EscapeSequences.BLACK_KING.trim();
                case QUEEN -> EscapeSequences.BLACK_QUEEN.trim();
                case BISHOP -> EscapeSequences.BLACK_BISHOP.trim();
                case KNIGHT -> EscapeSequences.BLACK_KNIGHT.trim();
                case ROOK -> EscapeSequences.BLACK_ROOK.trim();
                case PAWN -> EscapeSequences.BLACK_PAWN.trim();
            };
        }
    }
}