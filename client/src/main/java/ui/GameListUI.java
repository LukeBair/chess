package ui;

import helpers.Common;
import managers.GameManager;
import managers.GamePlayManager;
import models.AuthData;
import models.GameListEntry;
import client.data.ServerFacade;
import models.CreateGameResult;

import java.util.Arrays;

public class GameListUI {

    public static void showGameListAndGetCommand(GameListEntry[] games,
                                                 GameManager gm,
                                                 Renderer renderer,
                                                 ServerFacade server,
                                                 AuthData authData) {
        var lines = new java.util.ArrayList<String>();
        lines.add(Common.GAME_TITLE);
        lines.add("\n\n");
        lines.add("Available Games:\n");
        for (int i = 0; i < games.length; i++) {
            String white = games[i].whiteUsername() != null ? games[i].whiteUsername() + " (WHITE)" : "UNASSIGNED";
            String black = games[i].blackUsername() != null ? games[i].blackUsername() + " (BLACK)" : "UNASSIGNED";
            lines.add(String.format("%d. %-20s | %-15s | %-15s", i + 1, games[i].gameName(), white, black));
        }
        lines.add("\nCommands:");
        lines.add("create <name>     - Create a new game");
        lines.add("join <number|name> - Join as player");
        lines.add("observe <number|name> - Observe game");
        lines.add("refresh           - Refresh list");
        lines.add("logout            - Log out");
        lines.add("quit              - Exit");
        lines.add("help              - Show help");
        renderer.enqueueRenderTasks(lines.toArray(new String[0]));

        String input = gm.getInput().trim().toLowerCase();
        if (input.isEmpty()) return;

        String[] parts = input.split("\\s+", 3);
        String cmd = parts[0];

        switch (cmd) {
            case "create" -> handleCreate(parts, server, authData, renderer);
            case "join" -> handleJoinOrObserve(games, parts, false, gm, renderer, server, authData);
            case "observe" -> handleJoinOrObserve(games, parts, true, gm, renderer, server, authData);
            case "refresh", "" -> {}
            case "logout" -> gm.setState(GameManager.GameState.LOGOUT);
            case "quit" -> gm.setState(GameManager.GameState.QUIT);
            case "help" -> showHelp(renderer, gm);
            default -> renderer.enqueueRenderTask("Unknown command: " + cmd);
        }
    }

    private static void handleCreate(String[] parts, ServerFacade server, AuthData auth, Renderer r) {
        if (parts.length < 2) { r.enqueueRenderTask("Usage: create <name>"); return; }
        String name = parts[1];
        try {
            CreateGameResult res = server.createGame(name, auth.authToken());
            r.enqueueRenderTask("Game '" + name + "' created! ID: " + res.gameID());
        } catch (Exception e) {
            r.enqueueRenderTask("Failed to create game: " + e.getMessage());
        }
    }

    private static void handleJoinOrObserve(GameListEntry[] games, String[] parts,
                                            boolean observe, GameManager gm, Renderer r,
                                            ServerFacade server, AuthData auth) {
        if (parts.length < 2) {
            r.enqueueRenderTask("Usage: " + (observe ? "observe" : "join") + " <number|name>");
            return;
        }
        String target = parts[1];

        GameListEntry game = findGame(games, target);
        if (game == null) {
            r.enqueueRenderTask("Game not found: " + target);
            gm.getInput();
            return;
        }

        String colorStr = observe ? null :
                (game.whiteUsername() == null ? "WHITE" : game.blackUsername() == null ? "BLACK" : null);

        if (!observe && colorStr == null) {
            r.enqueueRenderTask("Game is full. Use 'observe' instead.");
            gm.getInput();
            return;
        }

        try {
            if (!observe) {
                server.joinGame(game.gameID(), colorStr, auth.authToken());
            }

            gm.currentGameID = game.gameID();
            gm.myColor = colorStr == null ? null :
                    colorStr.equals("WHITE") ? chess.ChessGame.TeamColor.WHITE : chess.ChessGame.TeamColor.BLACK;

            gm.getWebSocketFacade().connect(auth.authToken(), game.gameID());
            gm.setState(observe ? GameManager.GameState.OBSERVING : GameManager.GameState.PLAYING);

        } catch (Exception e) {
            r.enqueueRenderTask((observe ? "Observe" : "Join") + " failed: " + e.getMessage());
        }
    }

    private static GameListEntry findGame(GameListEntry[] games, String target) {
        try {
            int idx = Integer.parseInt(target) - 1;
            if (idx >= 0 && idx < games.length) return games[idx];
        } catch (NumberFormatException ignored) {}
        return Arrays.stream(games)
                .filter(g -> g.gameName().equalsIgnoreCase(target))
                .findFirst()
                .orElse(null);
    }

    private static void showHelp(Renderer r, GameManager gm) {
        r.enqueueRenderTasks(new String[]{
                "Commands:", "create <name>", "join <num|name>", "observe <num|name>",
                "refresh", "logout", "quit", "help", "\nPress enter"
        });
        gm.getInput();
    }
}