package server;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import io.javalin.*;
import io.javalin.http.Context;
import models.*;
import org.jetbrains.annotations.NotNull;
import service.ClearService;
import service.GameService;
import service.UserService;

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

        // Global exception handler for unhandled errors
        javalin.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.json(Map.of("message", "Error: " + e.getMessage()));
        });

        javalin.exception(DataAccessException.class, (e, ctx) -> {
            ctx.status(500);
            ctx.json(Map.of("message", "Error: " + e.getMessage()));
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

    /**
     * Handles common result error patterns: sets status and responds with message if present.
     * Assumes success results have no message(); error results do.
     * @param ctx The Javalin context.
     * @param result The service result to check.
     * @param successResponse A lambda to execute on success.
     */
    private void handleResult(@NotNull Context ctx, Object result, Runnable successResponse) {
        try {
            String message = getMessageFromResult(result);
            if (message != null) {
                int status = determineStatus(message);
                ctx.status(status);
                ctx.json(Map.of("message", "Error: " + message));
                return;
            }
            successResponse.run();
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private String getMessageFromResult(Object result) {
        // Fixed pattern matching for records - access via bound variable
        if (result instanceof RegisterResult regRes) {
            return regRes.message();
        } else if (result instanceof LoginResult loginRes) {
            return loginRes.message();
        } else if (result instanceof LogoutResult logoutRes) {
            return logoutRes.message();
        } else if (result instanceof CreateGameResult createRes) {
            return createRes.message();
        } else if (result instanceof JoinGameResult joinRes) {
            return joinRes.message();
        } else if (result instanceof ListGamesResult listRes) {
            return listRes.message();
        }
        // Add more as needed for other results
        return null;
    }

    private int determineStatus(String message) {
        if (message == null) {
            return 200;
        }
        if (message.contains("bad request")) {
            return 400;
        } else if (message.contains("unauthorized")) {
            return 401;
        } else if (message.contains("already taken")) {
            return 403;
        } else {
            return 500;
        }
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
            if (authToken == null) {
                context.status(401);
                context.json(Map.of("message", "Error: unauthorized"));
                return;
            }
            JoinGameRequest request = context.bodyAsClass(JoinGameRequest.class);
            JoinGameResult result = gameService.joinGame(request, authToken);
            handleResult(context, result, () -> {
                context.status(200);
                context.json(Map.of());
            });
        } catch (Exception e) {
            // Deserialization or other input errors
            context.status(400);
            context.json(Map.of("message", "Error: bad request"));
        }
    }

    private void createGame(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");
            if (authToken == null) {
                context.status(401);
                context.json(Map.of("message", "Error: unauthorized"));
                return;
            }
            CreateGameRequest request = context.bodyAsClass(CreateGameRequest.class);
            CreateGameResult result = gameService.createGame(request, authToken);
            handleResult(context, result, () -> {
                context.status(200);
                context.json(Map.of("gameID", result.gameID()));
            });
        } catch (Exception e) {
            // Deserialization or other input errors
            context.status(400);
            context.json(Map.of("message", "Error: bad request"));
        }
    }

    private void listGames(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");
            if (authToken == null) {
                context.status(401);
                context.json(Map.of("message", "Error: unauthorized"));
                return;
            }
            ListGamesResult result = gameService.listGames(authToken);
            handleResult(context, result, () -> {
                context.status(200);
                context.json(Map.of("games", result.games()));
            });
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
            handleResult(context, result, () -> {
                context.status(200);
                context.json(Map.of());
            });
        } catch (Exception e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void login(@NotNull Context context) {
        try {
            LoginRequest request = context.bodyAsClass(LoginRequest.class);
            LoginResult result = userService.login(request);
            handleResult(context, result, () -> {
                context.status(200);
                context.json(Map.of("username", result.username(), "authToken", result.authToken()));
            });
        } catch (Exception e) {
            // Deserialization or other input errors
            context.status(400);
            context.json(Map.of("message", "Error: bad request"));
        }
    }

    private void registerUser(@NotNull Context context) {
        try {
            RegisterRequest request = context.bodyAsClass(RegisterRequest.class);
            RegisterResult result = userService.register(request);
            handleResult(context, result, () -> {
                context.status(200);
                context.json(Map.of("username", result.username(), "authToken", result.authToken()));
            });
        } catch (Exception e) {
            // Deserialization or other input errors
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