package ui;
import java.util.Scanner;
/*
* inspired by unity game object classes
*/
public class GameManager {
    Scanner scanner = new Scanner(System.in);
    Renderer renderer = new Renderer();

    public static boolean running = false;


    public void start() {
//        System.out.println("Game Manager started.");
        String input = "";

        running = true;
        renderer.start();

        while (running) {
            input = getInput();
            update(input);
        }
    }

    public void update(String input) {
        if (input.equals("exit")) {
            running = false;
        } else {
            renderer.enqueueRenderTask("Render input: " + input);
        }
    }

    // get and validate user input
    public String getInput() {
        return scanner.nextLine();

        // Todo: add validation
    }
}
