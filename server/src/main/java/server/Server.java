package server;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import io.javalin.*;
import io.javalin.http.Context;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;
import server.Req_Res.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Server {

    private final Javalin javalin;
    private final DataAccess dataAccess;

    public Server() {
        this.dataAccess = new MemoryDataAccess();

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
            dataAccess.clear();
            context.status(200);
            context.json(Map.of());
        } catch (DataAccessException e) {
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

            AuthData authData = dataAccess.getAuth(authToken);

            if (authData == null) {
                context.status(401);
                context.json(Map.of("message", "Error: unauthorized"));
                return;
            }

            JoinGameRequest request = context.bodyAsClass(JoinGameRequest.class);
            if (request.gameID() == null) {
                context.status(400);
                context.json(Map.of("message", "Error: bad request"));
                return;
            }

            GameData game = dataAccess.getGame(request.gameID());
            if (game == null) {
                context.status(400);
                context.json(Map.of("message", "Error: bad request"));
                return;
            }

            if (request.playerColor() != null) {
                if (!request.playerColor().equalsIgnoreCase("WHITE") &&
                        !request.playerColor().equalsIgnoreCase("BLACK")) {
                    context.status(400);
                    context.json(Map.of("message", "Error: bad request"));
                    return;
                }

                String username = authData.username();

                if (request.playerColor().equalsIgnoreCase("WHITE")) {
                    if (game.whiteUsername() != null) {
                        context.status(403);
                        context.json(Map.of("message", "Error: already taken"));
                        return;
                    }

                    GameData updatedGame = new GameData(
                            game.gameID(),
                            username,  // whiteUsername
                            game.blackUsername(),
                            game.gameName(),
                            game.game()
                    );
                    dataAccess.updateGame(updatedGame);

                } else if (request.playerColor().equalsIgnoreCase("BLACK")) { // BLACK
                    if (game.blackUsername() != null) {
                        context.status(403);
                        context.json(Map.of("message", "Error: already taken"));
                        return;
                    }

                    GameData updatedGame = new GameData(
                            game.gameID(),
                            game.whiteUsername(),
                            username,  // blackUsername
                            game.gameName(),
                            game.game()
                    );
                    dataAccess.updateGame(updatedGame);
                } else {
                    context.status(400);
                    context.json(Map.of("message", "Error: bad request"));
                    return;
                }
            }
            else {
                context.status(400);
                context.json(Map.of("message", "Error: bad request"));
                return;
            }
        } catch (DataAccessException e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
        } catch (Exception e) {
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

            AuthData authData = dataAccess.getAuth(authToken);

            if (authData == null) {
                context.status(401);
                context.json(Map.of("message", "Error: unauthorized"));
                return;
            }

            CreateGameRequest request = context.bodyAsClass(CreateGameRequest.class);
            if (request.gameName() == null) {
                context.status(400);
                context.json(Map.of("message", "Error: bad request"));
                return;
            }

            int gameID = Math.abs(UUID.randomUUID().hashCode());
            GameData game = new GameData(
                    gameID,
                    null,
                    null,
                    request.gameName(),
                    new chess.ChessGame()
            );
            dataAccess.createGame(game);

            context.status(200);
            context.json(new CreateGameResponse(gameID));

        } catch (DataAccessException e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
        } catch (Exception e) {
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

            AuthData authData = dataAccess.getAuth(authToken);

            if (authData == null) {
                context.status(401);
                context.json(Map.of("message", "Error: unauthorized"));
                return;
            }

            List<GameData> games = dataAccess.listGames();

            // Convert to simplified list entries
            List<GameListEntry> gameList = games.stream()
                    .map(g -> new GameListEntry(g.gameID(), g.whiteUsername(), g.blackUsername(), g.gameName()))
                    .toList();

            context.status(200);
            context.json(new ListGamesResponse(gameList));

        } catch (DataAccessException e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void logout(@NotNull Context context) {
        try {
            String authToken = context.header("Authorization");

            if (authToken == null) {
                context.status(400);
                context.json(Map.of("message", "Error: bad request"));
                return;
            }

            AuthData authData = dataAccess.getAuth(authToken);

            if (authData == null) {
                context.status(401);
                context.json(Map.of("message", "Error: unauthorized"));
                return;
            }

            dataAccess.deleteAuth(authToken);

            context.status(200);
            context.json(Map.of());

        } catch (DataAccessException e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void login(@NotNull Context context) {
        try {
            LoginRequest request = context.bodyAsClass(LoginRequest.class);

            if (request.username() == null || request.password() == null) {
                context.status(400);
                context.json(Map.of("message", "Error: bad request"));
                return;
            }

            UserData user = dataAccess.getUser(request.username());

            if (user == null || !BCrypt.checkpw(request.password(), user.password())) {
                context.status(401);
                context.json(Map.of("message", "Error: unauthorized"));
                return;
            }

            String authToken = UUID.randomUUID().toString();
            AuthData authData = new AuthData(authToken, user.username());
            dataAccess.createAuth(authData);

            context.status(200);
            context.json(new LoginResponse(user.username(), authToken));

        } catch (DataAccessException e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
        } catch (Exception e) {
            context.status(400);
            context.json(Map.of("message", "Error: bad request"));
        }
    }

    private void registerUser(@NotNull Context context) {
        try {
            RegisterRequest request = context.bodyAsClass(RegisterRequest.class);

            if (request.username() == null || request.password() == null || request.email() == null) {
                context.status(400);
                context.json(Map.of("message", "Error: bad request"));
                return;
            }

            if (dataAccess.getUser(request.username()) != null) {
                context.status(403);
                context.json(Map.of("message", "Error: already taken"));
                return;
            }

            String hashedPassword = BCrypt.hashpw(request.password(), BCrypt.gensalt());
            UserData user = new UserData(request.username(), hashedPassword, request.email());
            dataAccess.insertUser(user);

            String authToken = UUID.randomUUID().toString();
            AuthData authData = new AuthData(authToken, user.username());
            dataAccess.createAuth(authData);

            context.status(200);
            context.json(new RegisterResponse(user.username(), authToken));

        } catch (DataAccessException e) {
            context.status(500);
            context.json(Map.of("message", "Error: " + e.getMessage()));
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