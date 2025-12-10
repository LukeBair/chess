package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.SQLDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import models.*;
import org.jetbrains.annotations.NotNull;
import server.websocket.WebSocketManager;
import service.ClearService;
import service.GameService;
import service.UserService;

import java.time.Duration;
import java.util.Map;

public class Server {

    private final Javalin javalin;
    private final GameService gameService;
    private final UserService userService;
    private final ClearService clearService;
    private final WebSocketManager webSocketHandler;
    private final Gson gson = new Gson();

    public Server() {
        SQLDataAccess sqlDataAccess;
        try {
            sqlDataAccess = new SQLDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException("Unable to initialize SQLDataAccess: " + e.getMessage());
        }

        this.userService = new UserService(sqlDataAccess);
        this.gameService = new GameService(sqlDataAccess);
        this.clearService = new ClearService(sqlDataAccess);
        this.webSocketHandler = new WebSocketManager(gameService, userService);

        javalin = Javalin.create(config -> {
            config.staticFiles.add("web");
            config.jetty.modifyWebSocketServletFactory(factory -> {
                factory.setIdleTimeout(Duration.ofMinutes(10));
            });
        });

        // Global exception handlers
        javalin.exception(DataAccessException.class, (e, ctx) -> {
            sendErrorResponse(ctx, 500, e.getMessage());
        });
        javalin.exception(Exception.class, (e, ctx) -> {
            sendErrorResponse(ctx, 500, e.getMessage());
        });

        // Routes
        javalin.post("/user", this::registerUser);
        javalin.post("/session", this::login);
        javalin.delete("/session", this::logout);
        javalin.get("/game", this::listGames);
        javalin.post("/game", this::createGame);
        javalin.put("/game", this::joinGame);
        javalin.delete("/db", this::clear);

        javalin.ws("/ws", ws -> {
            ws.onConnect(webSocketHandler);
            ws.onClose(webSocketHandler);
            ws.onMessage(webSocketHandler);
        });
    }

    private void sendErrorResponse(@NotNull Context ctx, int status, String message) {
        ctx.status(status);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(Map.of("message", "Error: " + message)));
    }

    private void sendSuccessResponse(@NotNull Context ctx, Map<String, Object> data) {
        ctx.status(200);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(data));
    }

    private String getAuthToken(@NotNull Context ctx) {
        return ctx.header("Authorization");
    }

    private void handleResult(@NotNull Context ctx, Object result, Runnable onSuccess) {
        String message = getMessageFromResult(result);
        if (message != null) {
            int status = determineStatus(message);
            sendErrorResponse(ctx, status, message);
        } else {
            onSuccess.run();
        }
    }

    private String getMessageFromResult(Object result) {
        if (result instanceof RegisterResult r) {
            return r.message();
        }
        if (result instanceof LoginResult r) {
            return r.message();
        }
        if (result instanceof LogoutResult(String message)) {
            return message;
        }
        if (result instanceof CreateGameResult r) {
            return r.message();
        }
        if (result instanceof JoinGameResult(String message)) {
            return message;
        }
        if (result instanceof ListGamesResult r) {
            return r.message();
        }

        return null;
    }

    private int determineStatus(String message) {
        if (message == null) return 200;
        String lower = message.toLowerCase();
        if (lower.contains("bad request") || lower.contains("invalid")) return 400;
        if (lower.contains("unauthorized")) return 401;
        if (lower.contains("already taken")) return 403;
        return 500;
    }

    // ====================== ENDPOINTS ======================

    private void clear(@NotNull Context ctx) {
        try {
            clearService.clear();
            sendSuccessResponse(ctx, Map.of());
        } catch (Exception e) {
            sendErrorResponse(ctx, 500, e.getMessage());
        }
    }

    private void registerUser(@NotNull Context ctx) {
        try {
            RegisterRequest req = gson.fromJson(ctx.body(), RegisterRequest.class);
            if (req == null || req.username() == null || req.password() == null || req.email() == null) {
                sendErrorResponse(ctx, 400, "bad request");
                return;
            }
            RegisterResult result = userService.register(req);
            handleResult(ctx, result, () -> sendSuccessResponse(ctx,
                    Map.of("username", result.username(), "authToken", result.authToken())));
        } catch (Exception e) {
            sendErrorResponse(ctx, 500, e.getMessage());
        }
    }

    private void login(@NotNull Context ctx) {
        try {
            LoginRequest req = gson.fromJson(ctx.body(), LoginRequest.class);
            if (req == null || req.username() == null || req.password() == null) {
                sendErrorResponse(ctx, 400, "bad request");
                return;
            }
            LoginResult result = userService.login(req);
            handleResult(ctx, result, () -> sendSuccessResponse(ctx,
                    Map.of("username", result.username(), "authToken", result.authToken())));
        } catch (Exception e) {
            sendErrorResponse(ctx, 500, e.getMessage());
        }
    }

    private void logout(@NotNull Context ctx) {
        try {
            String authToken = getAuthToken(ctx);
            LogoutResult result = userService.logout(authToken);  // Let service handle null/invalid
            handleResult(ctx, result, () -> sendSuccessResponse(ctx, Map.of()));
        } catch (Exception e) {
            sendErrorResponse(ctx, 500, e.getMessage());
        }
    }

    private void createGame(@NotNull Context ctx) {
        try {
            String authToken = getAuthToken(ctx);
            CreateGameRequest req = gson.fromJson(ctx.body(), CreateGameRequest.class);

            if (req == null || req.gameName() == null || req.gameName().isBlank()) {
                sendErrorResponse(ctx, 400, "bad request");
                return;
            }

            CreateGameResult result = gameService.createGame(req, authToken);
            handleResult(ctx, result, () -> sendSuccessResponse(ctx, Map.of("gameID", result.gameID())));
        } catch (Exception e) {
            sendErrorResponse(ctx, 500, e.getMessage());
        }
    }

    private void listGames(@NotNull Context ctx) {
        try {
            String authToken = getAuthToken(ctx);
            ListGamesResult result = gameService.listGames(authToken);  // Will throw if DB down or token invalid
            handleResult(ctx, result, () -> sendSuccessResponse(ctx, Map.of("games", result.games())));
        } catch (Exception e) {
            sendErrorResponse(ctx, 500, e.getMessage());
        }
    }

    private void joinGame(@NotNull Context ctx) {
        try {
            String authToken = getAuthToken(ctx);
            JoinGameRequest req = gson.fromJson(ctx.body(), JoinGameRequest.class);

            JoinGameResult result = gameService.joinGame(req, authToken);
            handleResult(ctx, result, () -> sendSuccessResponse(ctx, Map.of()));
        } catch (Exception e) {
            sendErrorResponse(ctx, 500, e.getMessage());
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