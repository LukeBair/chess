package server;

import service.GameService;
import service.UserService;
import spark.*;

public class Server {
    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");
        Spark.get("/", (req, res) -> {)
            res.redirect("/index.html");
            return null; // Spark requires a return value, but we redirect
        });

        Spark.delete("/db", (req, res) -> {;
            try {
                // Logic to delete the database
                // go to data access or smt
                res.status(200);
                return "";
            } catch (Exception e) {
                res.status(500);
                return "Error: " + e.getMessage();
            }
        });

        UserService userService = new UserService();
        Spark.post("/user", userService::login);
        Spark.get("/user", userService::createUser);
        Spark.delete("/user", userService::logout);

        GameService gameService = new GameService();
        Spark.post("/game", gameService::createGame);
        Spark.get("/game", gameService::getAllGames);
        Spark.post("/game", gameService::joinGame);




        //This line initializes the server and can be removed once you have a functioning endpoint
        Spark.init();

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}