package server;

import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import io.javalin.*;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Server {

    private final Javalin javalin;
    private final MemoryDataAccess memoryDataAccess;

    public Server() {
        this.memoryDataAccess = new MemoryDataAccess();
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        // Register your endpoints and exception handlers here.
        javalin.post("/user", this::registerUser);
        javalin.post("/session", this::login);
        javalin.delete("/session", this::logout);
        javalin.get("/game", this::listGames);
        javalin.post("/game", this::createGame);
        javalin.put("/game", this::joinGame);
        javalin.delete("/db", this::clear);
    }

    private void clear(@NotNull Context context) {
        try {
            memoryDataAccess.clear();
            context.status(200);
            context.json(Map.of());
        } catch (DataAccessException e) {
            context.status(500);
            context.json(Map.of("message:", "Internal server error"));
        }
    }

    private void joinGame(@NotNull Context context) {
    }

    private void createGame(@NotNull Context context) {
    }

    private void listGames(@NotNull Context context) {
    }

    private void logout(@NotNull Context context) {
    }

    private void login(@NotNull Context context) {
    }

    private void registerUser(@NotNull Context context) {
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
