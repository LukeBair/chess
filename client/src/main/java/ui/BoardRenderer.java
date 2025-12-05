package ui;

import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

public class BoardRenderer {
    private static final String LIGHT_SQUARE = EscapeSequences.SET_BG_COLOR_LIGHT_GREY;  // Tan
    private static final String DARK_SQUARE = EscapeSequences.SET_BG_COLOR_GREEN;        // Green
    private static final String RESET_BG = EscapeSequences.RESET_BG_COLOR;
    private static final String WHITE_PIECE = EscapeSequences.SET_TEXT_COLOR_WHITE;      // White pieces
    private static final String BLACK_PIECE = EscapeSequences.SET_TEXT_COLOR_BLACK;      // Black pieces
    private static final String RESET_TEXT = EscapeSequences.RESET_TEXT_COLOR;

    public String[] drawInitialBoard(ChessGame.TeamColor viewAs) {
        ChessGame game = new ChessGame();
        game.getBoard().resetBoard();  // Make sure board is initialized
        String[] lines = new String[20];

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
                ChessPiece piece = game.getBoard().getPiece(pos);

                boolean isLight = ((row + col) % 2 == 0);
                String bg = isLight ? LIGHT_SQUARE : DARK_SQUARE;

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

        // Pad empty lines
        for (int i = lineIdx + 1; i < lines.length; i++) {
            lines[i] = "";
        }

        return lines;
    }

    private String getUnicode(ChessPiece piece) {
        ChessPiece.PieceType type = piece.getPieceType();
        ChessGame.TeamColor color = piece.getTeamColor();

        if (color == ChessGame.TeamColor.WHITE) {
            switch (type) {
                case KING: return EscapeSequences.WHITE_KING.trim();
                case QUEEN: return EscapeSequences.WHITE_QUEEN.trim();
                case BISHOP: return EscapeSequences.WHITE_BISHOP.trim();
                case KNIGHT: return EscapeSequences.WHITE_KNIGHT.trim();
                case ROOK: return EscapeSequences.WHITE_ROOK.trim();
                case PAWN: return EscapeSequences.WHITE_PAWN.trim();
            }
        } else {
            switch (type) {
                case KING: return EscapeSequences.BLACK_KING.trim();
                case QUEEN: return EscapeSequences.BLACK_QUEEN.trim();
                case BISHOP: return EscapeSequences.BLACK_BISHOP.trim();
                case KNIGHT: return EscapeSequences.BLACK_KNIGHT.trim();
                case ROOK: return EscapeSequences.BLACK_ROOK.trim();
                case PAWN: return EscapeSequences.BLACK_PAWN.trim();
            }
        }
        return " ";
    }
}