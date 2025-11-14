package ui;
import client.data.ServerFacade;
import models.AuthData;
import models.GameListEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
/*
* inspired by unity game object classes
*/
public class GameManager {
    public static boolean running = false;
    private final int screenHeight = 100;


    private final Scanner scanner = new Scanner(System.in);
    private final Renderer renderer = new Renderer();
    private final ServerFacade server = new ServerFacade(8080);
    private GameState currentState = GameState.MENU;

    private AuthData authData;

    private enum GameState {
        MENU,
        LOGIN,
        CREATE_ACCOUNT,
        VIEW_GAMES,
        PLAYING,
        LOGOUT,
        ERROR,
        QUIT
    }

    public void start() {
        String input = "";

        running = true;
        renderer.start();

        try {
            server.test();
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
//            input = getInput();
        }
    }

    public void update(String input) {
        if (currentState == GameState.QUIT) {
            // WARNING: for testing only right now
            running = false;
        } else if (currentState == GameState.MENU) {
            displayMainMenu();
        } else if (currentState == GameState.LOGIN) {
            displayLogin();
        } else if (currentState == GameState.CREATE_ACCOUNT) {
            displayCreateAccount();
        } else if (currentState == GameState.VIEW_GAMES) {
            displayViewGames();
        } else if (currentState == GameState.PLAYING) {
            displayPlayingGame();
        } else if (currentState == GameState.LOGOUT) {
            displayLogout();
        }
    }

    private void displayPlayingGame() {

    }

    private void displayLogout() {
        
    }

    private void displayViewGames() {
        try {
            GameListEntry[] games = server.listGames(authData.authToken());
            ArrayList<String> renderTasks = new ArrayList<>();

            renderTasks.add(Common.GAME_TITLE);
            renderTasks.add("\n\n\n");
            renderTasks.add("Games:");

            for (GameListEntry game : games) {
                String whitePlayer = (game.whiteUsername() != null) ? game.whiteUsername() + " (WHITE)" : "UNASSIGNED (WHITE)";
                String blackPlayer = (game.blackUsername() != null) ? game.blackUsername() + " (BLACK)" : "UNASSIGNED (BLACK)";

                String players;
                if (game.whiteUsername() != null && game.blackUsername() != null) {
                    players = whitePlayer + " vs " + blackPlayer;
                } else if (game.whiteUsername() != null) {
                    players = whitePlayer + " or " + blackPlayer;
                } else if (game.blackUsername() != null) {
                    players = blackPlayer + " or " + whitePlayer;
                } else {
                    players = whitePlayer + " or " + blackPlayer;
                }
                String formatted = game.gameName() + " -------- " + players + ", game id " + game.gameID();
                renderTasks.add(formatted);
            }


            renderer.enqueueRenderTasks(renderTasks.toArray(new String[] {}));

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void displayMainMenu() {
        renderer.enqueueRenderTask(EscapeSequences.ERASE_SCREEN);
        renderer.enqueueRenderTasks(new String[] {
                Common.GAME_TITLE,
                "\n\n\n",
                "login - Login to your account",
                "create - Create a new account",
                "exit - Exit the game"
        });

        var input = getInput();

        if(input.equalsIgnoreCase("login")) {
            currentState = GameState.LOGIN;
        } else if (input.equalsIgnoreCase("create")) {
            currentState = GameState.CREATE_ACCOUNT;
        } else if(input.equalsIgnoreCase("exit")) {
            currentState = GameState.QUIT;
        }
    }

    private void displayCreateAccount() {
        renderer.enqueueRenderTasks(new String[] {
                Common.GAME_TITLE,
                "\n\n\n",
                "What will your username be?"
        });

        String username = getInput();

        renderer.enqueueRenderTasks(new String[] {
                "\n",
                "What will your password be?"
        });

        String password = getInput();

        renderer.enqueueRenderTasks(new String[] {
                "\n",
                "Finally, what is your email address?"
        });

        String email = getInput();

        try {
            authData = server.register(username, password, email);
            renderer.enqueueRenderTasks(new String [] { "\n", "Successfully created account!" });
            Thread.sleep(100);
            currentState = GameState.VIEW_GAMES;
        } catch (IOException | InterruptedException e) {
            // TODO: PROPER ERROR HANDLING NEEDED
            renderer.enqueueRenderTask(e.toString());
            throw new RuntimeException(e);
        }
    }

    // DISPLAYS
    public void displayLogin() {
        renderer.enqueueRenderTasks(new String[] {
                Common.GAME_TITLE,
                "\n\n\n",
                "Please enter your username"
        });

        String username = getInput();

        renderer.enqueueRenderTasks(new String[] {
                "\n",
                "Please enter your password"
        });

        String password = getInput();

        try {
            authData = server.login(username, password);
            renderer.enqueueRenderTasks(new String [] { "\n", "You have successfully logged in!" });
            currentState = GameState.VIEW_GAMES;
        } catch (IOException | InterruptedException e) {
            // TODO: proper error handling
            renderer.enqueueRenderTask(e.toString());
            throw new RuntimeException(e);
        }
    }


    // get and validate user input
    public String getInput() {
        return scanner.nextLine();

        // Todo: add validation
    }
}
