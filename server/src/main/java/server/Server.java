package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import dataaccess.SQLDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import models.*;
import org.jetbrains.annotations.NotNull;
import service.ClearService;
import service.GameService;
import service.UserService;

import java.util.Map;

public class Server {

    private final Javalin javalin;

    private final GameService gameService;
    private final UserService userService;
    private final ClearService clearService;

    private final Gson gson;

    public Server() {
        // removed the memory data access cause im not sure how to use it properly here. Will ask proffesor in class
        DataAccess dataAccess = new MemoryDataAccess();
        SQLDataAccess sqlDataAccess;

        try {
            sqlDataAccess = new SQLDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException("Unable to initialize SQLDataAccess: " + e.getMessage());
        }

        this.userService = new UserService(sqlDataAccess);
        this.gameService = new GameService(sqlDataAccess);
        this.clearService = new ClearService(sqlDataAccess);

        this.gson = new Gson();


        javalin = Javalin.create(config -> {
            config.staticFiles.add("web");
        });

        // Global exception handler for unhandled errors
        javalin.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            String json = gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            ctx.result(json);
            ctx.contentType("application/json");
        });

        javalin.exception(DataAccessException.class, (e, ctx) -> {
            ctx.status(500);
            String json = gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            ctx.result(json);
            ctx.contentType("application/json");
        });

        // Register endpoints
        javalin.post("/user", this::registerUser);
        javalin.post("/session", this::login);
        javalin.delete("/session", this::logout);
        javalin.get("/game", this::listGames);
        javalin.post("/game", this::createGame);
        javalin.put("/game", this::joinGame);
        javalin.delete("/db", this::clear);
    }

    private void handleResult(@NotNull Context ctx, Object result, Runnable successResponse) {
        try {
            String message = getMessageFromResult(result);
            if (message != null) {
                int status = determineStatus(message);
                ctx.status(status);
                String json = gson.toJson(Map.of("message", "Error: " + message));
                ctx.result(json);
                ctx.contentType("application/json");
                return;
            }
            successResponse.run();
        } catch (Exception e) {
            ctx.status(500);
            String json = gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            ctx.result(json);
            ctx.contentType("application/json");
        }
    }

    private String getMessageFromResult(Object result) {
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
            String json = gson.toJson(Map.of());
            context.result(json);
            context.contentType("application/json");
        } catch (DataAccessException e) {
            context.status(500);
            String json = gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            context.result(json);
            context.contentType("application/json");
        } catch (Exception e) {
            context.status(500);
            String json = gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            context.result(json);
            context.contentType("application/json");
        }
    }

    private void joinGame(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");
            if (authToken == null) {
                context.status(401);
                String json = gson.toJson(Map.of("message", "Error: unauthorized"));
                context.result(json);
                context.contentType("application/json");
                return;
            }
            String body = context.body();
            JoinGameRequest request = gson.fromJson(body, JoinGameRequest.class);
            JoinGameResult result = gameService.joinGame(request, authToken);
            handleResult(context, result, () -> {
                context.status(200);
                String json = gson.toJson(Map.of());
                context.result(json);
                context.contentType("application/json");
            });
        } catch (Exception e) {
            context.status(400);
            String json = gson.toJson(Map.of("message", "Error: bad request"));
            context.result(json);
            context.contentType("application/json");
        }
    }

    private void createGame(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");
            if (authToken == null) {
                context.status(401);
                String json = gson.toJson(Map.of("message", "Error: unauthorized"));
                context.result(json);
                context.contentType("application/json");
                return;
            }
            String body = context.body();
            CreateGameRequest request = gson.fromJson(body, CreateGameRequest.class);
            CreateGameResult result = gameService.createGame(request, authToken);
            handleResult(context, result, () -> {
                context.status(200);
                String json = gson.toJson(Map.of("gameID", result.gameID()));
                context.result(json);
                context.contentType("application/json");
            });
        } catch (Exception e) {
            context.status(400);
            String json = gson.toJson(Map.of("message", "Error: bad request"));
            context.result(json);
            context.contentType("application/json");
        }
    }

    private void listGames(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");
            if (authToken == null) {
                context.status(401);
                String json = gson.toJson(Map.of("message", "Error: unauthorized"));
                context.result(json);
                context.contentType("application/json");
                return;
            }
            ListGamesResult result = gameService.listGames(authToken);
            handleResult(context, result, () -> {
                context.status(200);
                String json = gson.toJson(Map.of("games", result.games()));
                context.result(json);
                context.contentType("application/json");
            });
        } catch (Exception e) {
            context.status(500);
            String json = gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            context.result(json);
            context.contentType("application/json");
        }
    }

    private void logout(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");
            if (authToken == null) {
                context.status(401);
                String json = gson.toJson(Map.of("message", "Error: unauthorized"));
                context.result(json);
                context.contentType("application/json");
                return;
            }
            LogoutResult result = userService.logout(authToken);
            handleResult(context, result, () -> {
                context.status(200);
                String json = gson.toJson(Map.of());
                context.result(json);
                context.contentType("application/json");
            });
        } catch (Exception e) {
            context.status(500);
            String json = gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            context.result(json);
            context.contentType("application/json");
        }
    }

    private void login(@NotNull Context context) {
        try {
            String body = context.body();
            LoginRequest request = gson.fromJson(body, LoginRequest.class);
            LoginResult result = userService.login(request);
            handleResult(context, result, () -> {
                context.status(200);
                String json = gson.toJson(Map.of("username", result.username(), "authToken", result.authToken()));
                context.result(json);
                context.contentType("application/json");
            });
        } catch (Exception e) {
            context.status(400);
            String json = gson.toJson(Map.of("message", "Error: bad request"));
            context.result(json);
            context.contentType("application/json");
        }
    }

    private void registerUser(@NotNull Context context) {
        try {
            String body = context.body();
            RegisterRequest request = gson.fromJson(body, RegisterRequest.class);
            RegisterResult result = userService.register(request);
            handleResult(context, result, () -> {
                context.status(200);
                String json = gson.toJson(Map.of("username", result.username(), "authToken", result.authToken()));
                context.result(json);
                context.contentType("application/json");
            });
        } catch (Exception e) {
            context.status(400);
            String json = gson.toJson(Map.of("message", "Error: bad request"));
            context.result(json);
            context.contentType("application/json");
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