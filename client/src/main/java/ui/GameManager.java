package ui;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import client.data.ServerFacade;
import helpers.Common;
import models.AuthData;
import models.CreateGameResult;
import models.GameListEntry;
import websocket.WebSocketFacade;

import java.util.*;

public class GameManager {
    public static boolean running = false;
    private final Scanner scanner = new Scanner(System.in);
    private final Renderer renderer = new Renderer();
    private final ServerFacade server = new ServerFacade(8080);

    private GameState currentState = GameState.MENU;
    private AuthData authData;
    private final BoardRenderer boardRenderer = new BoardRenderer();
    private ChessGame.TeamColor myColor;
    private ChessGame currentGame;
    private int currentGameID;

    private WebSocketFacade webSocketFacade;
    private String username;
    private ChessPosition lastHighlightedPosition = null;

    private boolean isGameOver = false;
    private String gameOverMessage = null;
    private boolean hasShownLoadingMessage = false;

    private enum GameState {
        MENU,
        LOGIN,
        CREATE_ACCOUNT,
        VIEW_GAMES,
        PLAYING,
        LOGOUT,
        ERROR,
        OBSERVING,
        QUIT
    }

    public void start() {
        running = true;
        renderer.start();
        webSocketFacade = new WebSocketFacade(renderer, this);

        while (running) {
            renderer.enqueueRenderTask(EscapeSequences.ERASE_SCREEN);
            update();
        }
    }

    public void update() {
        if (currentState == GameState.QUIT) {
            running = false;
        } else if (currentState == GameState.MENU) {
            displayMainMenu();
        } else if (currentState == GameState.LOGIN) {
            displayLogin();
        } else if (currentState == GameState.CREATE_ACCOUNT) {
            displayCreateAccount();
        } else if (currentState == GameState.VIEW_GAMES) {
            displayViewGames();
        } else if (currentState == GameState.PLAYING || currentState == GameState.OBSERVING) {
            playChess();
        } else if (currentState == GameState.LOGOUT) {
            displayLogout();
        }
    }

    // Called by WebSocketFacade when LOAD_GAME message is received
    public void updateGame(ChessGame game) {
        // Simply update the game state
        // The board will be redrawn on next iteration of playChess() loop
        this.currentGame = game;

        // Check for game over conditions
        if (game.isInCheckmate(ChessGame.TeamColor.WHITE) ||
                game.isInCheckmate(ChessGame.TeamColor.BLACK) ||
                game.isInStalemate(ChessGame.TeamColor.WHITE) ||
                game.isInStalemate(ChessGame.TeamColor.BLACK) ||
                game.isGameOver()) {
            isGameOver = true;
        }
    }

    private void redrawBoardWithHighlight() {
        if (currentGame == null) {
            renderer.enqueueRenderTask("No game loaded yet.");
            return;
        }

        Set<ChessPosition> highlights = null;

        // Re-apply highlight if one was active
        if (lastHighlightedPosition != null) {
            var validMoves = currentGame.validMoves(lastHighlightedPosition);
            if (validMoves != null && !validMoves.isEmpty()) {
                highlights = new HashSet<>();
                highlights.add(lastHighlightedPosition);
                for (ChessMove m : validMoves) {
                    highlights.add(m.getEndPosition());
                }
            } else {
                // Piece moved or captured - clear highlight
                lastHighlightedPosition = null;
            }
        }

        String[] boardLines = boardRenderer.drawBoard(
                currentGame.getBoard(),
                myColor == null ? ChessGame.TeamColor.WHITE : myColor,
                highlights
        );
        renderer.enqueueRenderTasks(boardLines);
    }

    private String formatPosition(ChessPosition pos) {
        return "" + (char)('a' + pos.getColumn() - 1) + pos.getRow();
    }

    private void handleMove(String fromStr, String toStr) {
        if (isGameOver) {
            renderer.enqueueRenderTask("Game is over. Type 'leave' to exit.");
            return;
        }

        if (currentState == GameState.OBSERVING) {
            renderer.enqueueRenderTask("Observers cannot make moves.");
            return;
        }

        try {
            ChessPosition from = parsePosition(fromStr);
            ChessPosition to = parsePosition(toStr);

            if (from == null || to == null) {
                renderer.enqueueRenderTask("Invalid position format. Use format like 'e2' or 'a1'");
                return;
            }

            // Check for pawn promotion
            ChessPiece piece = currentGame.getBoard().getPiece(from);
            ChessPiece.PieceType promotionPiece = null;

            if (piece != null && piece.getPieceType() == ChessPiece.PieceType.PAWN) {
                int endRow = to.getRow();
                if ((piece.getTeamColor() == ChessGame.TeamColor.WHITE && endRow == 8) ||
                        (piece.getTeamColor() == ChessGame.TeamColor.BLACK && endRow == 1)) {

                    renderer.enqueueRenderTask("Pawn promotion! Choose piece (QUEEN/ROOK/BISHOP/KNIGHT):");
                    String promoInput = getInput().trim().toUpperCase();

                    promotionPiece = switch (promoInput) {
                        case "QUEEN", "Q" -> ChessPiece.PieceType.QUEEN;
                        case "ROOK", "R" -> ChessPiece.PieceType.ROOK;
                        case "BISHOP", "B" -> ChessPiece.PieceType.BISHOP;
                        case "KNIGHT", "N", "K" -> ChessPiece.PieceType.KNIGHT;
                        default -> {
                            renderer.enqueueRenderTask("Invalid piece. Defaulting to QUEEN.");
                            yield ChessPiece.PieceType.QUEEN;
                        }
                    };
                }
            }

            ChessMove move = new ChessMove(from, to, promotionPiece);
            webSocketFacade.makeMove(authData.authToken(), currentGameID, move);
            lastHighlightedPosition = null;

            renderer.enqueueRenderTask("Move sent. Press Enter to see updated board.");
        } catch (Exception e) {
            renderer.enqueueRenderTask("Error making move: " + e.getMessage());
        }
    }

    private void handleHighlight(String posStr) {
        try {
            ChessPosition pos = parsePosition(posStr);
            if (pos == null) {
                renderer.enqueueRenderTask("Invalid position format. Use format like 'e2' or 'a1'");
                return;
            }

            if (currentGame == null) {
                renderer.enqueueRenderTask("No game loaded.");
                return;
            }

            var validMoves = currentGame.validMoves(pos);

            if (validMoves == null || validMoves.isEmpty()) {
                renderer.enqueueRenderTask("No valid moves for piece at " + posStr);
                return;
            }

            lastHighlightedPosition = pos;

            Set<ChessPosition> highlightPositions = new HashSet<>();
            highlightPositions.add(pos);
            for (ChessMove move : validMoves) {
                highlightPositions.add(move.getEndPosition());
            }

            String[] boardLines = boardRenderer.drawBoard(
                    currentGame.getBoard(),
                    myColor == null ? ChessGame.TeamColor.WHITE : myColor,
                    highlightPositions
            );
            renderer.enqueueRenderTasks(boardLines);
            renderer.enqueueRenderTask("Showing legal moves for piece at " + posStr + ".");

            // Show gameplay help so user can immediately make a move
            displayGameplayHelp();

        } catch (Exception e) {
            renderer.enqueueRenderTask("Error highlighting moves: " + e.getMessage());
        }
    }

    private void handleLeave() {
        try {
            webSocketFacade.leaveGame(authData.authToken(), currentGameID);
            currentGame = null;
            currentGameID = 0;
            myColor = null;
            lastHighlightedPosition = null;
            isGameOver = false;
            gameOverMessage = null;
            currentState = GameState.VIEW_GAMES;
        } catch (Exception e) {
            renderer.enqueueRenderTask("Error leaving game: " + e.getMessage());
        }
    }

    private void handleResign() {
        if (currentState == GameState.OBSERVING) {
            renderer.enqueueRenderTask("Observers cannot resign.");
            return;
        }

        if (isGameOver) {
            renderer.enqueueRenderTask("Game is already over.");
            return;
        }

        // Require confirmation
        renderer.enqueueRenderTask(EscapeSequences.SET_TEXT_COLOR_RED +
                "Are you sure you want to resign? This will end the game. (yes/no)" +
                EscapeSequences.RESET_TEXT_COLOR);
        String confirm = getInput().trim().toLowerCase();

        if (confirm.equals("yes") || confirm.equals("y")) {
            try {
                webSocketFacade.resign(authData.authToken(), currentGameID);

                // Determine winner
                String winner = myColor == ChessGame.TeamColor.WHITE ? "Black" : "White";
                gameOverMessage = "Game Over - " + winner + " wins by resignation!";
                isGameOver = true;

                renderer.enqueueRenderTask(gameOverMessage);
                renderer.enqueueRenderTask("Type 'leave' to return to game list.");

            } catch (Exception e) {
                renderer.enqueueRenderTask("Error resigning: " + e.getMessage());
            }
        } else {
            renderer.enqueueRenderTask("Resign cancelled.");
        }
    }

    private ChessPosition parsePosition(String pos) {
        if (pos == null || pos.length() != 2) {
            return null;
        }

        char col = pos.charAt(0);
        char row = pos.charAt(1);

        if (col < 'a' || col > 'h' || row < '1' || row > '8') {
            return null;
        }

        int colNum = col - 'a' + 1;  // a=1, b=2, ..., h=8
        int rowNum = row - '0';       // 1=1, 2=2, ..., 8=8

        return new ChessPosition(rowNum, colNum);
    }

    private void displayLogout() {
        try {
            server.logout(authData.authToken());
        } catch (Exception e) {
            renderer.enqueueRenderTask("Logout error: " + e.getMessage());
        }
        authData = null;
        currentState = GameState.MENU;
    }

    private void displayViewGames() {
        try {
            var games = server.listGames(authData.authToken());

            ArrayList<String> renderTasks = new ArrayList<>();
            renderTasks.add(Common.GAME_TITLE);
            renderTasks.add("\n\n\n");
            renderTasks.add("Available Games:");

            for (int i = 0; i < games.length; i++) {
                String white = games[i].whiteUsername() != null ? games[i].whiteUsername() + " (WHITE)" : "UNASSIGNED";
                String black = games[i].blackUsername() != null ? games[i].blackUsername() + " (BLACK)" : "UNASSIGNED";
                renderTasks.add(String.format("%d. %-15s | %-15s | %-15s", i+1, games[i].gameName(), white, black));
            }
            renderTasks.add("\nCommands:");
            renderTasks.add("create <name> - Create a new game");
            renderTasks.add("join <index|name> - Join as player (auto WHITE/BLACK)");
            renderTasks.add("observe <index|name> - Observe (white view)");
            renderTasks.add("refresh - refresh games list");
            renderTasks.add("logout - Logout");
            renderTasks.add("quit - Quit");
            renderer.enqueueRenderTasks(renderTasks.toArray(new String[0]));

            String input = getInput().trim();
            if (input.isEmpty()) {
                return;
            }

            parseViewGamesCommand(games, input.toLowerCase());
        } catch (Exception e) {
            renderer.enqueueRenderTask("Error listing games: " + e.getMessage());
            getInput();
        }
    }

    private void parseViewGamesCommand(GameListEntry[] games, String input) {
        String[] parts = input.split("\\s+", 3);
        if (parts.length < 1) {
            return;
        }

        String cmd = parts[0];
        switch (cmd) {
            case "create" -> {
                if (parts.length < 2) {
                    renderer.enqueueRenderTask("Usage: create <name>");
                    return;
                }
                handleCreateGame(parts[1]);
            }
            case "join" -> {
                if (parts.length < 2) {
                    renderer.enqueueRenderTask("Usage: join <index|name>");
                    return;
                }
                handleJoinOrObserve(games, parts[1], false);
            }
            case "observe" -> {
                if (parts.length < 2) {
                    renderer.enqueueRenderTask("Usage: observe <index|name>");
                    return;
                }
                handleJoinOrObserve(games, parts[1], true);
            }
            case "logout" -> currentState = GameState.LOGOUT;
            case "quit" -> currentState = GameState.QUIT;
            case "refresh" -> {}
            case "help" -> {
                renderer.enqueueRenderTasks(new String[]{
                        "\nCommands:",
                        "create <name> - Create a new game",
                        "join <index|name> - Join as player (auto WHITE/BLACK)",
                        "observe <index|name> - Observe (white view)",
                        "refresh - refresh games list",
                        "logout - Logout",
                        "quit - Quit",
                        "Please hit enter to continue"
                });
                getInput();
            }
            default -> renderer.enqueueRenderTask("Unknown command: " + cmd + ". Type 'help' for options.");
        }
    }

    private void handleCreateGame(String gameName) {
        if (gameName.isEmpty()) {
            renderer.enqueueRenderTask("Game name cannot be empty.");
            return;
        }
        try {
            CreateGameResult res = server.createGame(gameName, authData.authToken());
            renderer.enqueueRenderTask("Game '" + gameName + "' created (ID: " + res.gameID() + "). List games to see it.");
        } catch (Exception e) {
            renderer.enqueueRenderTask("Create failed: " + e.getMessage());
        }
    }

    private void handleJoinOrObserve(GameListEntry[] games, String target, boolean isObserve) {
        GameListEntry game = null;
        try {
            int idx = Integer.parseInt(target) - 1;
            if (idx >= 0 && idx < games.length) {
                game = games[idx];
            }
        } catch (NumberFormatException ignored) {
            game = Arrays.stream(games)
                    .filter(g -> g.gameName().equalsIgnoreCase(target))
                    .findFirst().orElse(null);
        }

        if (game == null) {
            renderer.enqueueRenderTask("Game not found: " + target + ", please hit enter");
            getInput();
            return;
        }

        try {
            String playerColor = isObserve ? "UNASSIGNED" :
                    (game.whiteUsername() == null ? "WHITE" : (game.blackUsername() == null ? "BLACK" : null));

            if (!isObserve && playerColor == null) {
                renderer.enqueueRenderTask("Game full! Observe instead. Please hit enter to continue");
                getInput();
                return;
            }

            // HTTP join (only for players, not observers)
            if (!isObserve) {
                server.joinGame(game.gameID(), playerColor, authData.authToken());
            }

            // Store game info
            currentGameID = game.gameID();
            myColor = playerColor.equals("WHITE") ? ChessGame.TeamColor.WHITE :
                    playerColor.equals("BLACK") ? ChessGame.TeamColor.BLACK : null;

            // WebSocket connect
            webSocketFacade.connect(authData.authToken(), game.gameID());

            currentState = isObserve ? GameState.OBSERVING : GameState.PLAYING;

        } catch (Exception e) {
            renderer.enqueueRenderTask((isObserve ? "Observe" : "Join") + " failed: " + e.getMessage());
        }
    }

    private void displayMainMenu() {
        renderer.enqueueRenderTasks(new String[]{
                Common.GAME_TITLE,
                "\n\n\n",
                "Commands:",
                "login  - Login to your account",
                "create  - Create a new account",
                "quit  - Exit the game"
        });
        String input = getInput().trim().toLowerCase();
        if ("login".equalsIgnoreCase(input)) {
            currentState = GameState.LOGIN;
        } else if ("create".equalsIgnoreCase(input)) {
            currentState = GameState.CREATE_ACCOUNT;
        } else if ("quit".equalsIgnoreCase(input)) {
            currentState = GameState.QUIT;
        } else if ("help".equalsIgnoreCase(input)) {
            renderer.enqueueRenderTasks(new String[]{
                    Common.GAME_TITLE,
                    "\n\n\n",
                    "Commands:",
                    "login  - Login to your account",
                    "create  - Create a new account",
                    "quit  - Exit the game",
                    "please hit enter to continue"
            });
            getInput();
        }
    }

    private void displayCreateAccount() {
        renderer.enqueueRenderTasks(new String[]{
                Common.GAME_TITLE,
                "\n\n\n",
                "Enter username:"
        });
        username = getInput().trim();

        renderer.enqueueRenderTasks(new String[]{"Enter password:"});
        String password = getInput();

        renderer.enqueueRenderTasks(new String[]{"Enter email:"});
        String email = getInput().trim();

        try {
            authData = server.register(username, password, email);
            renderer.enqueueRenderTasks(new String[]{"Account created! Press enter to continue."});
            getInput();
            currentState = GameState.VIEW_GAMES;
        } catch (Exception e) {
            renderer.enqueueRenderTask("Registration failed: " + e.getMessage() + "\nPress enter.");
            getInput();
            currentState = GameState.MENU;
        }
    }

    public void displayLogin() {
        renderer.enqueueRenderTasks(new String[]{
                Common.GAME_TITLE,
                "\n\n\n",
                "Enter username:"
        });
        username = getInput().trim();

        renderer.enqueueRenderTasks(new String[]{"Enter password:"});
        String password = getInput();

        try {
            authData = server.login(username, password);
            renderer.enqueueRenderTasks(new String[]{"Logged in! Press enter to continue."});
            getInput();
            currentState = GameState.VIEW_GAMES;
        } catch (Exception e) {
            renderer.enqueueRenderTask("Login failed: " + e.getMessage() + "\nPress enter.");
            getInput();
            currentState = GameState.MENU;
        }
    }

    public String getInput() {
        return scanner.nextLine();
    }

    private void playChess() {
        if (currentGame == null) {
            // Only show loading message ONCE
            if (!hasShownLoadingMessage) {
                renderer.enqueueRenderTasks(new String[]{
                        EscapeSequences.ERASE_SCREEN,
                        EscapeSequences.SET_TEXT_COLOR_BLUE + EscapeSequences.SET_TEXT_BOLD,
                        "    Loading game from server...",
                        EscapeSequences.RESET_TEXT_COLOR,
                        "\n\n   Please wait a moment",
                        "\n   (You’ll see the board as soon as it arrives)"
                });
                hasShownLoadingMessage = true;
            }
            return; // Don't accept input yet
        }

        // Game is now loaded — this runs exactly once when game arrives
        if (hasShownLoadingMessage) {
            hasShownLoadingMessage = false; // Reset for next time
            renderer.enqueueRenderTask(EscapeSequences.ERASE_SCREEN); // Clear loading screen
        }

        redrawBoardWithHighlight();
        displayGameplayHelp();

        String input = getInput().trim().toLowerCase();
        if (!input.isEmpty()) {
            parseGameplayCommand(input);
        }
    }

    private void displayGameplayHelp() {
        // Show whose turn it is
        String turnInfo = EscapeSequences.SET_TEXT_COLOR_BLUE + "Turn: " +
                currentGame.getTeamTurn() + EscapeSequences.RESET_TEXT_COLOR;

        // Show game status
        String status = "";
        if (currentGame.isInCheckmate(ChessGame.TeamColor.WHITE)) {
            status = EscapeSequences.SET_TEXT_COLOR_RED + " [CHECKMATE! Black wins!]" + EscapeSequences.RESET_TEXT_COLOR;
            isGameOver = true;
        } else if (currentGame.isInCheckmate(ChessGame.TeamColor.BLACK)) {
            status = EscapeSequences.SET_TEXT_COLOR_RED + " [CHECKMATE! White wins!]" + EscapeSequences.RESET_TEXT_COLOR;
            isGameOver = true;
        } else if (currentGame.isInStalemate(ChessGame.TeamColor.WHITE) || currentGame.isInStalemate(ChessGame.TeamColor.BLACK)) {
            status = EscapeSequences.SET_TEXT_COLOR_RED + " [STALEMATE!]" + EscapeSequences.RESET_TEXT_COLOR;
            isGameOver = true;
        } else if (currentGame.isInCheck(currentGame.getTeamTurn())) {
            status = EscapeSequences.SET_TEXT_COLOR_RED + " [CHECK!]" + EscapeSequences.RESET_TEXT_COLOR;
        } else if (isGameOver && gameOverMessage != null) {
            status = EscapeSequences.SET_TEXT_COLOR_RED + " [" + gameOverMessage + "]" + EscapeSequences.RESET_TEXT_COLOR;
        }

        renderer.enqueueRenderTasks(new String[]{
                turnInfo + status,
                "\n" + EscapeSequences.SET_TEXT_COLOR_YELLOW + "═══════════════════════════════════════════════" + EscapeSequences.RESET_TEXT_COLOR,
                EscapeSequences.SET_TEXT_COLOR_GREEN + "Available Commands:" + EscapeSequences.RESET_TEXT_COLOR,
                "  " + EscapeSequences.SET_TEXT_COLOR_BLUE + "move <from> <to>" + EscapeSequences.RESET_TEXT_COLOR +
                        " - Make a move (e.g., 'move e2 e4')",
                "  " + EscapeSequences.SET_TEXT_COLOR_BLUE + "highlight <pos>" + EscapeSequences.RESET_TEXT_COLOR +
                        "   - Show legal moves for a piece (e.g., 'highlight e2')",
                "  " + EscapeSequences.SET_TEXT_COLOR_BLUE + "clear" + EscapeSequences.RESET_TEXT_COLOR +
                        "             - Remove highlight",
                "  " + EscapeSequences.SET_TEXT_COLOR_BLUE + "redraw" + EscapeSequences.RESET_TEXT_COLOR +
                        "          - Redraw the chess board",
                "  " + EscapeSequences.SET_TEXT_COLOR_BLUE + "leave" + EscapeSequences.RESET_TEXT_COLOR +
                        "           - Leave the game",
                "  " + EscapeSequences.SET_TEXT_COLOR_BLUE + "resign" + EscapeSequences.RESET_TEXT_COLOR +
                        "          - Forfeit the game",
                "  " + EscapeSequences.SET_TEXT_COLOR_BLUE + "help" + EscapeSequences.RESET_TEXT_COLOR +
                        "            - Show this help menu",
                EscapeSequences.SET_TEXT_COLOR_YELLOW + "═══════════════════════════════════════════════" + EscapeSequences.RESET_TEXT_COLOR,
                ""
        });
    }

    private void parseGameplayCommand(String input) {
        String[] parts = input.split("\\s+");
        if (parts.length < 1) {
            return;
        }

        String cmd = parts[0];
        switch (cmd) {
            case "help" -> {
                redrawBoardWithHighlight();
                displayGameplayHelp();
            }
            case "redraw" -> {
                redrawBoardWithHighlight();
                displayGameplayHelp();
            }
            case "leave" -> handleLeave();
            case "clear", "unhighlight" -> {
                lastHighlightedPosition = null;
                redrawBoardWithHighlight();
                renderer.enqueueRenderTask("Highlight cleared.");
                displayGameplayHelp();
            }
            case "move", "m" -> {
                if (isGameOver) {
                    renderer.enqueueRenderTask("Cannot move. Game is over.");
                    return;
                }

                if (parts.length < 3) {
                    renderer.enqueueRenderTask(EscapeSequences.SET_TEXT_COLOR_RED +
                            "Usage: move <from> <to> (e.g., 'move e2 e4')" + EscapeSequences.RESET_TEXT_COLOR);
                    return;
                }
                handleMove(parts[1], parts[2]);
            }
            case "resign" -> {
                if (isGameOver) {
                    renderer.enqueueRenderTask("Game is already over.");
                    return;
                }

                handleResign();
            }
            case "highlight", "h" -> {
                if (parts.length < 2) {
                    renderer.enqueueRenderTask(EscapeSequences.SET_TEXT_COLOR_RED +
                            "Usage: highlight <position> (e.g., 'highlight e2')" + EscapeSequences.RESET_TEXT_COLOR);
                    return;
                }
                handleHighlight(parts[1]);
            }
            default -> renderer.enqueueRenderTask(EscapeSequences.SET_TEXT_COLOR_RED +
                    "Unknown command: " + cmd + ". Type 'help' for options." + EscapeSequences.RESET_TEXT_COLOR);
        }
    }

    public void setGameOver(String message) {
        this.isGameOver = true;
        this.gameOverMessage = "Game Over - " + message;
    }
}