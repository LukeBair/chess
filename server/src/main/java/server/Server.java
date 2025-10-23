package server;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import io.javalin.*;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;
import Service.ClearService;
import Service.GameService;
import Service.UserService;
import server.Req_Res.*;

import java.util.Map;

public class Server {

    private final Javalin javalin;
    private final UserService userService;
    private final GameService gameService;
    private final ClearService clearService;

    public Server() {
        DataAccess dataAccess = new MemoryDataAccess();
        this.userService = new UserService(dataAccess);
        this.gameService = new GameService(dataAccess);
        this.clearService = new ClearService(dataAccess);

        javalin = Javalin.create(config -> {
            config.staticFiles.add("web");
        });

        // Register your endpoints
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
            clearService.clear();
            context.status(200);
            context.json(Map.of());
        } catch (DataAccessException e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
        } catch (Exception e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void joinGame(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");
            JoinGameRequest request = context.bodyAsClass(JoinGameRequest.class);
            JoinGameResult result = gameService.joinGame(request, authToken);

            if (result.message() != null) {
                if (result.message().contains("bad request")) context.status(400);
                else if (result.message().contains("unauthorized")) context.status(401);
                else if (result.message().contains("already taken")) context.status(403);
                else context.status(500);
                context.json(Map.of("message", result.message()));
                return;
            }
            context.status(200);
            context.json(Map.of());
        } catch (Exception e) {
            context.status(400);
            context.json(Map.of("message", "Error: bad request"));
        }
    }

    private void createGame(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");
            CreateGameRequest request = context.bodyAsClass(CreateGameRequest.class);
            CreateGameResult result = gameService.createGame(request, authToken);

            if (result.message() != null) {
                if (result.message().contains("bad request")) context.status(400);
                else if (result.message().contains("unauthorized")) context.status(401);
                else context.status(500);
                context.json(Map.of("message", result.message()));
                return;
            }
            context.status(200);
            context.json(Map.of("gameID", result.gameID()));
        } catch (Exception e) {
            context.status(400);
            context.json(Map.of("message", "Error: bad request"));
        }
    }

    private void listGames(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");
            ListGamesResult result = gameService.listGames(authToken);

            if (result.message() != null) {
                if (result.message().contains("unauthorized")) context.status(401);
                else context.status(500);
                context.json(Map.of("message", result.message()));
                return;
            }
            context.status(200);
            context.json(Map.of("games", result.games()));
        } catch (Exception e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void logout(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");
            if (authToken == null) {
                context.status(401);
                context.json(Map.of("message", "Error: unauthorized"));
                return;
            }
            LogoutResult result = userService.logout(authToken);

            if (result.message() != null) {
                if (result.message().contains("unauthorized")) context.status(401);
                else context.status(500);
                context.json(Map.of("message", result.message()));
                return;
            }
            context.status(200);
            context.json(Map.of());
        } catch (Exception e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void login(@NotNull Context context) {
        try {
            LoginRequest request = context.bodyAsClass(LoginRequest.class);
            LoginResult result = userService.login(request);

            if (result.message() != null) {
                if (result.message().contains("bad request")) context.status(400);
                else if (result.message().contains("unauthorized")) context.status(401);
                else context.status(500);
                context.json(Map.of("message", result.message()));
                return;
            }
            context.status(200);
            context.json(Map.of("username", result.username(), "authToken", result.authToken()));
        } catch (Exception e) {
            context.status(400);
            context.json(Map.of("message", "Error: bad request"));
        }
    }

    private void registerUser(@NotNull Context context) {
        try {
            RegisterRequest request = context.bodyAsClass(RegisterRequest.class);
            RegisterResult result = userService.register(request);

            if (result.message() != null) {
                if (result.message().contains("bad request")) context.status(400);
                else if (result.message().contains("already taken")) context.status(403);
                else context.status(500);
                context.json(Map.of("message", result.message()));
                return;
            }
            context.status(200);
            context.json(Map.of("username", result.username(), "authToken", result.authToken()));
        } catch (Exception e) {
            context.status(400);
            context.json(Map.of("message", "Error: bad request"));
        }
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}