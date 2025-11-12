package ui;
import client.data.ServerFacade;

import java.util.Scanner;
/*
* inspired by unity game object classes
*/
public class GameManager {
    public static boolean running = false;
    private final int screenHeight = 100;


    private final Scanner scanner = new Scanner(System.in);
    private final Renderer renderer = new Renderer();

    private GameState currentState = GameState.MENU;

    private enum GameState {
        MENU,
        LOGIN,
        CREATE_ACCOUNT,
        VIEW_GAMES,
        PLAYING,
        LOGOUT,
        ERROR
    }

    public void start() {
        int port = 8080;
        ServerFacade serverFacade = new ServerFacade(port);

        String input = "";

        running = true;
        renderer.start();

        try {
            serverFacade.test();
        } catch (Exception e) {
            currentState = GameState.ERROR;
            renderer.enqueueRenderTasks(new String[] {
                    Common.ERROR_404,
                    "\n\n\n",
                    EscapeSequences.SET_TEXT_BOLD,
                    "\t\tUnable to connect to the server!",
                    EscapeSequences.SET_TEXT_BOLD
            });
        }

        while (running) {
            update(input);
            input = getInput();
        }
    }

    public void update(String input) {
        if (input.equals("exit")) {
            running = false;
        } else {
            renderer.enqueueRenderTask(EscapeSequences.ERASE_SCREEN);
            if (currentState == GameState.MENU) {
                renderer.enqueueRenderTasks(new String[] {
                        Common.GAME_TITLE,
                        "\n\n\n",
                        "login - Login to your account",
                        "create - Create a new account",
                        "exit - Exit the game"
                });
            }
        }
    }

    // get and validate user input
    public String getInput() {
        return scanner.nextLine();

        // Todo: add validation
    }
}
