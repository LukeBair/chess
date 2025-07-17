package service;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;
import model.JoinGameData;
import model.PlayerColor;
import spark.Request;
import spark.Response;

public class GameService {
    Gson serializer = new Gson();
    public String createGame(Request request, Response response) {
        try {
            response.status(200);
            return "{ \"gameName\":\"\" }";
        } catch (Exception e) {
            response.status(500);
            return serializer.toJson("Error: " + e.getMessage());
        }
    }

    public String getAllGames(Request request, Response response) {
        try {
            GameData[] gameData = new GameData[1];
            gameData[0] = new GameData(1, "", "", "", new ChessGame());
            response.status(200);
            return serializer.toJson(gameData);
        } catch (Exception e) {
            response.status(500);
            return serializer.toJson("Error: " + e.getMessage());
        }
    }

    public String joinGame(Request request, Response response) {
        try {
            // Logic to join a game
            JoinGameData joinGameData = new JoinGameData(PlayerColor.WHITE, 1);
            response.status(200);
            return serializer.toJson(joinGameData);
        } catch (Exception e) {
            response.status(500);
            return serializer.toJson("Error: " + e.getMessage());
        }
    }
}
