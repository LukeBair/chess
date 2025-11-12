import chess.*;
import client.data.ServerFacade;
import ui.GameManager;

public class Main {
    public static void main(String[] args) {
        int port = 8080;
        ServerFacade serverFacade = new ServerFacade(port);

        try {
            serverFacade.test();
        } catch (Exception e) {
            System.out.println("Error during server test: " + e.getMessage());
        }

        GameManager gameManager = new GameManager();
        gameManager.start();
    }
}