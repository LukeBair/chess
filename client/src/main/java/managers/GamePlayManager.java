package managers;

import chess.*;
import ui.BoardRenderer;
import ui.EscapeSequences;
import ui.Renderer;

import java.util.HashSet;
import java.util.Set;

public class GamePlayManager {
    private final GameManager parent;
    private final Renderer renderer;
    private final BoardRenderer boardRenderer = new BoardRenderer();

    private ChessGame currentGame;                    // null until server sends it
    private ChessGame.TeamColor myColor;
    private int currentGameID;
    private ChessPosition lastHighlightedPosition = null;
    private boolean isGameOver = false;
    private String gameOverMessage = null;

    private boolean isObserver = false;

    public GamePlayManager(GameManager parent, Renderer renderer) {
        this.parent = parent;
        this.renderer = renderer;
        this.myColor = parent.myColor != null ? parent.myColor : ChessGame.TeamColor.WHITE;
        this.currentGameID = parent.currentGameID;
    }

    public void updateGame(ChessGame game) {
        this.currentGame = game;
        checkGameOverConditions();
        parent.update();
    }

    private void checkGameOverConditions() {
        if (currentGame == null) {
            return;
        }
        if (currentGame.isInCheckmate(ChessGame.TeamColor.WHITE) ||
                currentGame.isInCheckmate(ChessGame.TeamColor.BLACK) ||
                currentGame.isInStalemate(ChessGame.TeamColor.WHITE) ||
                currentGame.isInStalemate(ChessGame.TeamColor.BLACK) ||
                currentGame.isGameOver()) {
            isGameOver = true;
        }
    }

    public void playChess() {
        renderer.enqueueRenderTask(EscapeSequences.ERASE_SCREEN);
        if (currentGame != null) {
            redrawBoardWithHighlight();
            displayGameplayHelp();
        }

        String input = parent.getInput().trim().toLowerCase();
        if (!input.isEmpty()) {
            parseCommand(input);
        }
    }

    private void redrawBoardWithHighlight() {
        Set<ChessPosition> highlights = null;
        if (lastHighlightedPosition != null) {
            var validMoves = currentGame.validMoves(lastHighlightedPosition);
            if (validMoves != null && !validMoves.isEmpty()) {
                highlights = new HashSet<>();
                highlights.add(lastHighlightedPosition);
                for (ChessMove move : validMoves) {
                    highlights.add(move.getEndPosition());
                }
            } else {
                lastHighlightedPosition = null; // piece gone or invalid
            }
        }

        String[] boardLines = boardRenderer.drawBoard(
                currentGame.getBoard(),
                myColor,
                highlights
        );
        renderer.enqueueRenderTasks(boardLines);
    }

    private void parseCommand(String input) {
        String[] parts = input.split("\\s+");
        if (parts.length == 0) {
            return;
        }
        String cmd = parts[0];

        switch (cmd) {
            case "help", "redraw" -> { redrawBoardWithHighlight(); displayGameplayHelp(); }
            case "clear", "unhighlight" -> {
                lastHighlightedPosition = null;
                redrawBoardWithHighlight();
                displayGameplayHelp();
            }
            case "leave" -> handleLeave();
            case "move", "m" -> {
                if (parts.length < 3) {
                    renderer.enqueueRenderTask(EscapeSequences.SET_TEXT_COLOR_RED +
                            "Usage: move <from> <to> (e.g. move e2 e4)" + EscapeSequences.RESET_TEXT_COLOR);
                    return;
                }
                handleMove(parts[1], parts[2]);
            }
            case "highlight", "h" -> {
                if (parts.length < 2) {
                    renderer.enqueueRenderTask("Usage: highlight <square> (e.g. highlight e4)");
                    return;
                }
                handleHighlight(parts[1]);
            }
            case "resign" -> handleResign();
            default -> renderer.enqueueRenderTask(EscapeSequences.SET_TEXT_COLOR_RED +
                    "Unknown command. Type 'help'." + EscapeSequences.RESET_TEXT_COLOR);
        }
    }

    private void handleMove(String fromStr, String toStr) {
        if (isGameOver) {
            renderer.enqueueRenderTask("Game is over.");
            return;
        }
        if (parent.currentState == GameManager.GameState.OBSERVING) {
            renderer.enqueueRenderTask("Observers cannot make moves.");
            return;
        }

        ChessPosition from = parsePosition(fromStr);
        ChessPosition to = parsePosition(toStr);
        if (from == null || to == null) {
            renderer.enqueueRenderTask("Invalid position. Use format like 'e2'");
            return;
        }

        ChessPiece piece = currentGame.getBoard().getPiece(from);
        ChessPiece.PieceType promotion = null;

        if (piece != null && piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            boolean isPromoting = (piece.getTeamColor() == ChessGame.TeamColor.WHITE && to.getRow() == 8) ||
                    (piece.getTeamColor() == ChessGame.TeamColor.BLACK && to.getRow() == 1);
            if (isPromoting) {
                renderer.enqueueRenderTask("Promote pawn to: queen, rook, bishop, or knight? (default: queen)");
                String choice = parent.getInput().trim().toLowerCase();
                promotion = switch (choice) {
                    case "rook", "r" -> ChessPiece.PieceType.ROOK;
                    case "bishop", "b" -> ChessPiece.PieceType.BISHOP;
                    case "knight", "n", "k" -> ChessPiece.PieceType.KNIGHT;
                    default -> ChessPiece.PieceType.QUEEN;
                };
            }
        }

        ChessMove move = new ChessMove(from, to, promotion);
        parent.getWebSocketFacade().makeMove(parent.getAuthData().authToken(), currentGameID, move);
        lastHighlightedPosition = null;
    }

    private void handleHighlight(String posStr) {
        ChessPosition pos = parsePosition(posStr);
        if (pos == null) {
            renderer.enqueueRenderTask("Invalid square format.");
            return;
        }

        var moves = currentGame.validMoves(pos);
        if (moves == null || moves.isEmpty()) {
            renderer.enqueueRenderTask("No legal moves from " + posStr.toUpperCase());
            lastHighlightedPosition = null;
            redrawBoardWithHighlight();
            return;
        }

        lastHighlightedPosition = pos;
        Set<ChessPosition> highlights = new HashSet<>();
        highlights.add(pos);
        for (ChessMove m : moves) {
            highlights.add(m.getEndPosition());
        }

        String[] board = boardRenderer.drawBoard(currentGame.getBoard(),
                myColor == null ? ChessGame.TeamColor.WHITE : myColor, highlights);
        renderer.enqueueRenderTasks(board);
        renderer.enqueueRenderTask("Legal moves from " + posStr.toUpperCase() + " highlighted.");
        displayGameplayHelp();
    }

    private void handleLeave() {
        parent.getWebSocketFacade().leaveGame(parent.getAuthData().authToken(), currentGameID);
        currentGame = null;
        lastHighlightedPosition = null;
        parent.setState(GameManager.GameState.VIEW_GAMES);
    }

    private void handleResign() {
        if (isObserver) {
            return;
        }

        if (isGameOver) {
            renderer.enqueueRenderTask("Game is already over.");
            return;
        }
        renderer.enqueueRenderTask(EscapeSequences.SET_TEXT_COLOR_RED + "Type 'yes' to resign:" + EscapeSequences.RESET_TEXT_COLOR);
        if (parent.getInput().trim().equalsIgnoreCase("yes")) {
            parent.getWebSocketFacade().resign(parent.getAuthData().authToken(), currentGameID);
            isGameOver = true;
            gameOverMessage = myColor == ChessGame.TeamColor.WHITE ? "Black wins by resignation" : "White wins by resignation";
        } else {
            renderer.enqueueRenderTask("Resign cancelled.");
            displayGameplayHelp();
        }
    }

    private ChessPosition parsePosition(String s) {
        if (s.length() != 2) {
            return null;
        }
        char file = Character.toLowerCase(s.charAt(0));
        char rank = s.charAt(1);
        if (file < 'a' || file > 'h' || rank < '1' || rank > '8') {
            return null;
        }
        return new ChessPosition(rank - '0', file - 'a' + 1);
    }

    private void displayGameplayHelp() {
        String turn = currentGame.getTeamTurn().toString();
        String status = "";

        if (currentGame.isInCheckmate(ChessGame.TeamColor.WHITE)) {
            status = " [CHECKMATE Black wins]";
        }
        else if (currentGame.isInCheckmate(ChessGame.TeamColor.BLACK)) {
            status = " [CHECKMATE White wins]";
        }
        else if (currentGame.isInStalemate(currentGame.getTeamTurn())) {
            status = " [STALEMATE]";
        }
        else if (currentGame.isInCheck(currentGame.getTeamTurn())) {
            status = " [CHECK]";
        }
        else if (isGameOver && gameOverMessage != null) {
            status = " [" + gameOverMessage + "]";
        }

        renderer.enqueueRenderTasks(new String[]{
                EscapeSequences.SET_TEXT_COLOR_BLUE + "Turn: " + turn + EscapeSequences.RESET_TEXT_COLOR + status,
                EscapeSequences.SET_TEXT_COLOR_YELLOW + "═══════════════════════════════════════════════" +
                        EscapeSequences.RESET_TEXT_COLOR,
                EscapeSequences.SET_TEXT_COLOR_GREEN + "Commands:" + EscapeSequences.RESET_TEXT_COLOR,
                "  move <from> <to>     e.g. move e2 e4",
                "  highlight <sq>       e.g. highlight e4",
                "  redraw             clear             leave",
                "  resign             help",
                EscapeSequences.SET_TEXT_COLOR_YELLOW + "═══════════════════════════════════════════════" +
                        EscapeSequences.RESET_TEXT_COLOR,
                ""
        });
    }

    public void setGameOver(String message) {
        isGameOver = true;
        gameOverMessage = "Game Over " + message;
    }

    public void setIDIfNotNull(int gameID) {
        if (gameID != currentGameID) {
            isGameOver = false;
            gameOverMessage = "";
        }
        this.currentGameID = gameID;
    }

    public void setTeamColor(ChessGame.TeamColor teamColor) {
        this.myColor = teamColor;
    }

    public void setIsObserver(boolean isObserver) {
        this.isObserver = isObserver;
    }
}