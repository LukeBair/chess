import chess.*;
import client.data.ServerFacade;
import ui.GameManager;

public class Main {
    public static void main(String[] args) {
        GameManager gameManager = new GameManager();
        gameManager.start();
    }
}