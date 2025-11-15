package ui;
import chess.ChessGame;
import client.data.ServerFacade;
import helpers.CommandMapping;
import helpers.Common;
import models.AuthData;
import models.CreateGameResult;
import models.GameListEntry;
import models.JoinGameResult;

import java.util.*;

/*
 * inspired by unity game object classes
 */
public class GameManager {
    public static boolean running = false;
    private final int screenHeight = 100;

    private final Scanner scanner = new Scanner(System.in);
    private final Renderer renderer = new Renderer();
    private final ServerFacade server = new ServerFacade(8080);
    private final List<CommandMapping<GameState, String[]>> commandMapping = List.of(
            new CommandMapping<>(GameState.MENU, new String[]{"create account", "login", "quit", "help"}),
            new CommandMapping<>(GameState.VIEW_GAMES, new String[] {"create game", "join game", "observe game", "logout", "quit", "help"}),
            new CommandMapping<>(GameState.PLAYING, new String[] {"quit"}),
            new CommandMapping<>(GameState.OBSERVING, new String[] {"quit"})
    );

    private GameState currentState = GameState.MENU;
    private AuthData authData;
    private final BoardRenderer boardRenderer = new BoardRenderer();
    private ChessGame.TeamColor myColor;
    private GameListEntry[] lastGames;
    private final Set<Integer> lastGameIds = new HashSet<>();

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
            displayGameBoard();
        } else if (currentState == GameState.LOGOUT) {
            displayLogout();
        }
    }

    private void displayGameBoard() {
        String[] boardLines = boardRenderer.drawInitialBoard(myColor == null ? ChessGame.TeamColor.WHITE : myColor);
        renderer.enqueueRenderTasks(boardLines);
        renderer.enqueueRenderTask("\nEnter 'quit' to exit.");
        String input = getInput().trim().toLowerCase();
        if ("quit".equals(input)) {
            myColor = null;  // Reset
            currentState = GameState.VIEW_GAMES;
        }
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
            lastGames = server.listGames(authData.authToken());
            updateLastGameIds();  // Update tracker

            ArrayList<String> renderTasks = new ArrayList<>();
            renderTasks.add(Common.GAME_TITLE);
            renderTasks.add("\n\n\n");
            renderTasks.add("Available Games (refreshes every 3s on change):");

            for (int i = 0; i < lastGames.length; i++) {
                String white = lastGames[i].whiteUsername() != null ? lastGames[i].whiteUsername() + " (WHITE)" : "UNASSIGNED";
                String black = lastGames[i].blackUsername() != null ? lastGames[i].blackUsername() + " (BLACK)" : "UNASSIGNED";
                renderTasks.add(String.format("%d. %-15s | %-15s | %-15s", i+1, lastGames[i].gameName(), white, black));
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
            if (input.isEmpty()) { return; }

            parseViewGamesCommand(input.toLowerCase());
        } catch (Exception e) {
            renderer.enqueueRenderTask("Error listing games: " + e.getMessage());
            getInput();  // Wait for ack
        }
    }

    private void parseViewGamesCommand(String input) {
        String[] parts = input.split("\\s+", 3);  // Max 3 parts: command arg1 arg2 (if needed)
        if (parts.length < 1) { return; }

        String cmd = parts[0];
        switch (cmd) {
            case "create" -> {
                if (parts.length < 2) {
                    renderer.enqueueRenderTask("Usage: create game <name>");
                    return;
                }
                handleCreateGame(parts[1]);
            }
            case "join" -> {
                if (parts.length < 2) {
                    renderer.enqueueRenderTask("Usage: join game <index|name>");
                    return;
                }
                handleJoinOrObserve(parts[1], false);
            }
            case "observe" -> {
                if (parts.length < 2) {
                    renderer.enqueueRenderTask("Usage: observe game <index|name>");
                    return;
                }
                handleJoinOrObserve(parts[1], true);
            }
            case "logout" -> currentState = GameState.LOGOUT;
            case "quit" -> currentState = GameState.QUIT;
            case "refresh" -> {}
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

    private void handleJoinOrObserve(String target, boolean isObserve) {
        GameListEntry game = null;
        try {
            // Try as index (1-based)
            int idx = Integer.parseInt(target) - 1;
            if (idx >= 0 && idx < lastGames.length) {
                game = lastGames[idx];
            }
        } catch (NumberFormatException ignored) {
            // Try as name
            game = Arrays.stream(lastGames)
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
            if (isObserve) {
                server.observeGame(game.gameID(), authData.authToken());
                myColor = ChessGame.TeamColor.WHITE;  // Spectator view
                currentState = GameState.OBSERVING;
            } else {
                JoinGameResult res = server.joinGame(game.gameID(), playerColor, authData.authToken());
                myColor = "WHITE".equals(playerColor) ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
                currentState = GameState.PLAYING;
            }
            renderer.enqueueRenderTask(isObserve ? "Observing game." : "Joined as " + playerColor + ".");
        } catch (Exception e) {
            renderer.enqueueRenderTask((isObserve ? "Observe" : "Join") + " failed: " + e.getMessage());
        }
    }

    private void updateLastGameIds() {
        lastGameIds.clear();
        for (GameListEntry g : lastGames) {
            lastGameIds.add(g.gameID());
        }
    }

    private void displayMainMenu() {
        renderer.enqueueRenderTasks(new String[] {
                Common.GAME_TITLE,
                "\n\n\n",
                "Commands:",
                "login  - Login to your account",
                "create account  - Create a new account",
                "quit  - Exit the game"
        });
        String input = getInput().trim().toLowerCase();
        if ("login".equals(input)) {
            currentState = GameState.LOGIN;
        } else if ("create account".equals(input)) {
            currentState = GameState.CREATE_ACCOUNT;
        } else if ("quit".equals(input)) {
            currentState = GameState.QUIT;
        }
    }

    private void displayCreateAccount() {
        renderer.enqueueRenderTasks(new String[] {
                Common.GAME_TITLE,
                "\n\n\n",
                "Enter username:"
        });
        String username = getInput().trim();

        renderer.enqueueRenderTasks(new String[] {
                "Enter password:"
        });
        String password = getInput();

        renderer.enqueueRenderTasks(new String[] {
                "Enter email:"
        });
        String email = getInput().trim();

        try {
            authData = server.register(username, password, email);
            renderer.enqueueRenderTasks(new String[] { "Account created! Press enter to continue." });
            getInput();
            currentState = GameState.VIEW_GAMES;
        } catch (Exception e) {
            renderer.enqueueRenderTask("Registration failed (e.g., username taken): " + e.getMessage() + "\nPress enter.");
            getInput();
            currentState = GameState.MENU;  // Back to menu on fail
        }
    }

    public void displayLogin() {
        renderer.enqueueRenderTasks(new String[] {
                Common.GAME_TITLE,
                "\n\n\n",
                "Enter username:"
        });
        String username = getInput().trim();

        renderer.enqueueRenderTasks(new String[] {
                "Enter password:"
        });
        String password = getInput();

        try {
            authData = server.login(username, password);
            renderer.enqueueRenderTasks(new String[] { "Logged in! Press enter to continue." });
            getInput();
            currentState = GameState.VIEW_GAMES;
        } catch (Exception e) {
            renderer.enqueueRenderTask("Login failed: " + e.getMessage() + "\nPress enter.");
            getInput();
            currentState = GameState.MENU;  // Back to menu on fail
        }
    }

    public String getInput() {
        return scanner.nextLine();
    }
}