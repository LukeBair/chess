package client.data;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.rmi.ServerException;
import java.util.Map;

import chess.ChessGame;
import com.google.gson.Gson;
import models.*;

public class ServerFacade {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;

    public ServerFacade(int port) {
        this.baseUrl = "http://localhost:" + port + "/";
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public AuthData register(String username, String password, String email) throws IOException, InterruptedException {
        String jsonBody = gson.toJson(new RegisterRequest(username, password, email));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "user"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Parse success response into AuthData
            return gson.fromJson(response.body(), AuthData.class);
        } else {
            // Handle error (e.g., throw custom exception or return null)
            throw new RuntimeException("Registration failed: " + response.body());
        }
    }

    public AuthData login(String username, String password) throws IOException, InterruptedException {
        String jsonBody = gson.toJson(new LoginRequest(username, password));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "session"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), AuthData.class);
        } else {
            throw new RuntimeException("Login failed: " + response.body());
        }
    }

    public void logout(String authToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "session"))
                .header("Authorization", authToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Logout failed: " + response.body());
        }
    }

    public CreateGameResult createGame(String gameName, String authToken) throws IOException, InterruptedException {
        String jsonBody = gson.toJson(new CreateGameRequest(gameName));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "game"))
                .header("Content-Type", "application/json")
                .header("Authorization", authToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
           //TODO: return intial game state
            return gson.fromJson(response.body(), CreateGameResult.class); //res
        } else {
            throw new RuntimeException("Create game failed: " + response.body());
        }
    }

    public GameListEntry[] listGames(String authToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "game"))
                .header("Authorization", authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            ListGamesResult res = gson.fromJson(response.body(), ListGamesResult.class);
            return res != null && res.games() != null ? res.games().toArray(new GameListEntry[]{}) : new GameListEntry[0];
        } else {
            throw new RuntimeException("List games failed: " + response.body());
        }
    }

    public JoinGameResult joinGame(int gameId, String playerColor, String authToken) throws IOException, InterruptedException {
        String jsonBody = gson.toJson(new JoinGameRequest(playerColor, gameId));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "game"))
                .header("Content-Type", "application/json")
                .header("Authorization",  authToken)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), JoinGameResult.class);
        } else {
            throw new RuntimeException("Join game failed: " + response.body());
        }
    }

    public JoinGameResult observeGame(int gameId, String authToken) throws IOException, InterruptedException {
        return joinGame(gameId, "UNASSIGNED", authToken);  // Reuse
    }

    public void test() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "test"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}