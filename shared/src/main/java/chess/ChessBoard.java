package chess;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {
    private final ChessPiece[][] board = new ChessPiece[8][8];

    public ChessBoard() {    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        board[position.getColumn()][position.getRow()] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return board[position.getColumn()][position.getRow()];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        for (int y = 0; y < 8; y++) {
            board[y][1] = ChessGame.BLACK_PAWN;
            board[y][6] = ChessGame.WHITE_PAWN;
        }

        board[0][0] = ChessGame.BLACK_ROOK;
        board[7][0] = ChessGame.BLACK_ROOK;
        board[1][0] = ChessGame.BLACK_KNIGHT;
        board[6][0] = ChessGame.BLACK_KNIGHT;
        board[2][0] = ChessGame.BLACK_BISHOP;
        board[5][0] = ChessGame.BLACK_BISHOP;
        board[3][0] = ChessGame.BLACK_QUEEN;
        board[4][0] = ChessGame.BLACK_KING;

        board[0][7] = ChessGame.WHITE_ROOK;
        board[7][7] = ChessGame.WHITE_ROOK;
        board[1][7] = ChessGame.WHITE_KNIGHT;
        board[6][7] = ChessGame.WHITE_KNIGHT;
        board[2][7] = ChessGame.WHITE_BISHOP;
        board[5][7] = ChessGame.WHITE_BISHOP;
        board[3][7] = ChessGame.WHITE_QUEEN;
        board[4][7] = ChessGame.WHITE_KING;
    }
}
