package server;

import com.google.gson.Gson;
import dataacess.DataAccess;
import service.GameService;
import service.HTTPExepction;
import service.UserService;
import spark.*;

public class Server {
    public int run(int desiredPort) {
        Gson gson = new Gson();
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");
        Spark.get("/", (req, res) -> {
            res.redirect("/index.html");
            return null; // Spark requires a return value, but we redirect
        });

        Spark.delete("/db", (req, res) -> {;
            try {
                DataAccess.INSTANCE.clear();
                res.status(200);
                return "";
            } catch (Exception e) {
                res.status(500);
                return "Error: " + e.getMessage();
            }
        });

        UserService userService = new UserService();
        Spark.post("/session", (req, res) -> {;
            try {
                var x = userService.login(req.body());
                res.status(200);
                return gson.toJson(x);

            } catch (HTTPExepction e) {
                res.status(e.statusCode);
                return gson.toJson(e.model);
            }
        });

        Spark.post("/user", (req, res) -> {
            try {
                var x = userService.createUser(req.body());
                res.status(200);
                return gson.toJson(x);
            } catch (HTTPExepction e) {
                res.status(e.statusCode);
                return gson.toJson(e.model);
            }
        });
        Spark.delete("/session", (req, res) -> {;
            try {
               var x = userService.logout(req.headers("Authorization"));
                res.status(200);
                return "";
            } catch (HTTPExepction e) {
                res.status(e.statusCode);
                return gson.toJson(e.model);
            }
        });

        GameService gameService = new GameService();
        Spark.post("/game", (req, res) -> {
            try {
                var x = gameService.createGame(req.headers("Authorization"), req.body());
                res.status(200);
                return gson.toJson(x);
            } catch (HTTPExepction e) {
                res.status(e.statusCode);
                return gson.toJson(e.model);
            }
        });
        Spark.get("/game", (req, res) -> {;
            try {
                var x = gameService.getAllGames(req.headers("Authorization"));
                res.status(200);
                return gson.toJson(x);
            } catch (HTTPExepction e) {
                res.status(e.statusCode);
                return gson.toJson(e.model);
            }
        });

        Spark.put("/game", ((req, res) -> {
            try {
                var x = gameService.joinGame(req.headers("Authorization"), req.body());
                res.status(200);
                return "";
            } catch (HTTPExepction e) {
                res.status(e.statusCode);
                return gson.toJson(e.model);
            }
        }));

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