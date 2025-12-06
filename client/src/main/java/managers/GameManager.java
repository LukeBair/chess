package managers;

import client.data.ServerFacade;
import helpers.Common;
import managers.GamePlayManager;
import managers.MenuManager;
import models.AuthData;
import ui.EscapeSequences;
import ui.Renderer;
import websocket.WebSocketFacade;

import java.util.Scanner;

public class GameManager {
    public static boolean running = false;

    private final Scanner scanner = new Scanner(System.in);
    private final Renderer renderer = new Renderer();
    private final ServerFacade server = new ServerFacade(8080);

    GameState currentState = GameState.MENU;
    private AuthData authData = null;

    // Managers
    private final MenuManager menuManager = new MenuManager(this, renderer, server);
    private final GamePlayManager gamePlayManager = new GamePlayManager(this, renderer);

    private WebSocketFacade webSocketFacade;

    public enum GameState {
        MENU, LOGIN, CREATE_ACCOUNT, VIEW_GAMES, PLAYING, OBSERVING, LOGOUT, QUIT
    }

    public void start() {
        running = true;
        renderer.start();
        webSocketFacade = new WebSocketFacade(renderer, gamePlayManager);

        while (running) {
            renderer.enqueueRenderTask(EscapeSequences.ERASE_SCREEN);
            update();
        }
        scanner.close();
    }

    void update() {
        switch (currentState) {
            case QUIT -> running = false;
            case MENU -> menuManager.displayMainMenu();
            case LOGIN -> menuManager.displayLogin();
            case CREATE_ACCOUNT -> menuManager.displayCreateAccount();
            case VIEW_GAMES -> menuManager.displayViewGames(authData);
            case PLAYING, OBSERVING -> gamePlayManager.playChess();
            case LOGOUT -> menuManager.performLogout(authData);
        }
    }

    // Called from WebSocket when game state updates
    public void updateGame(chess.ChessGame game) {
        gamePlayManager.updateGame(game);
    }

    // === Getters / Setters for child classes ===
    public void setState(GameState state) { this.currentState = state; }
    public void setAuthData(AuthData auth) { this.authData = auth; }
    public AuthData getAuthData() { return authData; }
    public WebSocketFacade getWebSocketFacade() { return webSocketFacade; }
    public String getInput() { return scanner.nextLine(); }

    // Used by GameListManager
    public int currentGameID;
    public chess.ChessGame.TeamColor myColor;
}