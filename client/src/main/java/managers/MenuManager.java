package managers;

import client.data.ServerFacade;
import helpers.Common;
import managers.GameManager;
import models.AuthData;
import models.GameListEntry;
import ui.GameListUI;
import ui.Renderer;

public class MenuManager {
    private final GameManager gameManager;
    private final Renderer renderer;
    private final ServerFacade server;

    public MenuManager(GameManager gameManager, Renderer renderer, ServerFacade server) {
        this.gameManager = gameManager;
        this.renderer = renderer;
        this.server = server;
    }

    public void displayMainMenu() {
        renderer.enqueueRenderTasks(new String[]{
                Common.GAME_TITLE, "\n\n\n",
                "Commands:",
                "login  - Login to your account",
                "create - Create a new account",
                "quit   - Exit the game",
                "\nEnter command:"
        });

        String input = gameManager.getInput().trim().toLowerCase();
        switch (input) {
            case "login" -> gameManager.setState(GameManager.GameState.LOGIN);
            case "create" -> gameManager.setState(GameManager.GameState.CREATE_ACCOUNT);
            case "quit" -> gameManager.setState(GameManager.GameState.QUIT);
            case "help" -> showMainMenuHelp();
            case "" -> {}
            default -> renderer.enqueueRenderTask("Unknown command. Type 'help' or 'quit'.");
        }
    }

    private void showMainMenuHelp() {
        renderer.enqueueRenderTasks(new String[]{
                Common.GAME_TITLE, "\n\n\n",
                "Commands:", "login", "create", "quit", "help",
                "\nPress enter to continue"
        });
        gameManager.getInput();
    }

    public void displayCreateAccount() {
        renderer.enqueueRenderTasks(new String[]{Common.GAME_TITLE, "\n\n\n", "Enter username:"});
        String username = gameManager.getInput().trim();
        renderer.enqueueRenderTasks(new String[]{"Enter password:"});
        String password = gameManager.getInput();
        renderer.enqueueRenderTasks(new String[]{"Enter email:"});
        String email = gameManager.getInput().trim();

        try {
            AuthData auth = server.register(username, password, email);
            gameManager.setAuthData(auth);
            renderer.enqueueRenderTask("Account created successfully!");
            renderer.enqueueRenderTask("Press enter to continue");
            gameManager.getInput();
            gameManager.setState(GameManager.GameState.VIEW_GAMES);
        } catch (Exception e) {
            renderer.enqueueRenderTask("Registration failed: " + e.getMessage());
            renderer.enqueueRenderTask("Press enter to return");
            gameManager.getInput();
            gameManager.setState(GameManager.GameState.MENU);
        }
    }

    public void displayLogin() {
        renderer.enqueueRenderTasks(new String[]{Common.GAME_TITLE, "\n\n\n", "Enter username:"});
        String username = gameManager.getInput().trim();
        renderer.enqueueRenderTasks(new String[]{"Enter password:"});
        String password = gameManager.getInput();

        try {
            AuthData auth = server.login(username, password);
            gameManager.setAuthData(auth);
            renderer.enqueueRenderTask("Logged in successfully!");
            renderer.enqueueRenderTask("Press enter to continue");
            gameManager.getInput();
            gameManager.setState(GameManager.GameState.VIEW_GAMES);
        } catch (Exception e) {
            renderer.enqueueRenderTask("Login failed: " + e.getMessage());
            renderer.enqueueRenderTask("Press enter to return");
            gameManager.getInput();
            gameManager.setState(GameManager.GameState.MENU);
        }
    }

    public void displayViewGames(AuthData authData) {
        if (authData == null) {
            gameManager.setState(GameManager.GameState.MENU);
            return;
        }

        try {
            GameListEntry[] games = server.listGames(authData.authToken());
            GameListUI.showGameListAndGetCommand(games, gameManager, renderer, server, authData);
        } catch (Exception e) {
            renderer.enqueueRenderTask("Error loading games: " + e.getMessage());
            gameManager.getInput();
        }
    }

    public void performLogout(AuthData authData) {
        if (authData != null) {
            try {
                server.logout(authData.authToken());
            } catch (Exception ignored) {}
        }
        gameManager.setAuthData(null);
        renderer.enqueueRenderTask("Logged out successfully.");
        gameManager.getInput();
        gameManager.setState(GameManager.GameState.MENU);
    }
}