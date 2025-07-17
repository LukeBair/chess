package service;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dataacess.DataAccess;
import model.*;
import spark.Request;
import spark.Response;

public class GameService {
    Gson serializer = new Gson();
    public GameData createGame(String headers, String body) {
        try {
            if(!DataAccess.INSTANCE.authDataExistsByAuthToken(headers)) {
                throw new HTTPExepction(new ErrorModel("Error: unauthorized"), 401);
            }

            //if body is null it body returns "{}" which is not valid
            if (body.equals("{}")) {
                throw new HTTPExepction(new ErrorModel("Error: bad request"), 400);
            }

            String gameName = serializer.fromJson(body, JsonObject.class).get("gameName").getAsString();
            if (gameName == null || gameName.isEmpty()) {
                throw new HTTPExepction(new ErrorModel("Error: bad request"), 400);
            }

            if(DataAccess.INSTANCE.gameDataExistsByGameName(gameName)) {
                throw new HTTPExepction(new ErrorModel("Error: bad request"), 400);
            }

            var data = new GameData(DataAccess.INSTANCE.numGames() + 1, null, null, gameName, new ChessGame());
            DataAccess.INSTANCE.addGameData(data);

            return data;
        } catch (Exception e) {
            if (e instanceof HTTPExepction) {
                throw e; // rethrow the custom exception
            }
            throw new HTTPExepction(new ErrorModel("Error: " + e.getMessage()), 500);
        }
    }

    public GamesData getAllGames(String headers) {
        try {
            if(!DataAccess.INSTANCE.authDataExistsByAuthToken(headers)) {
                throw new HTTPExepction(new ErrorModel("Error: unauthorized"), 401);
            }

            GamesData gameData = DataAccess.INSTANCE.getAllGames();

            return gameData;
        } catch (Exception e) {
            if (e instanceof HTTPExepction) {
                throw e; // rethrow the custom exception
            }
            throw new HTTPExepction(new ErrorModel("Error: " + e.getMessage()), 500);
        }
    }

    public String joinGame(String headers, String body) {
        try {
            if(!DataAccess.INSTANCE.authDataExistsByAuthToken(headers)) {
                throw new HTTPExepction(new ErrorModel("Error: unauthorized"), 401);
            }

            JoinGameData joinGameData;

            try {
                joinGameData = serializer.fromJson(body, JoinGameData.class);
            } catch (Exception e) {
                throw new HTTPExepction(new ErrorModel("Error: bad request"), 400);
            }

            if(!DataAccess.INSTANCE.gameDataExists(joinGameData.gameID())) {
                throw new HTTPExepction(new ErrorModel("Error: bad request"), 400);
            }

            //atrocious way to check if the user is already in the game
            if (joinGameData.playerColor() == null ||
                    (joinGameData.playerColor() == PlayerColor.WHITE && !(DataAccess.INSTANCE.getGameData(joinGameData.gameID()).whiteUsername() == null)) ||
                    (joinGameData.playerColor() == PlayerColor.BLACK && !(DataAccess.INSTANCE.getGameData(joinGameData.gameID()).blackUsername() == null))) {
                throw new HTTPExepction(new ErrorModel("Error: already taken"), 403);
            }

            String username = DataAccess.INSTANCE.getAuthDataByAuthToken(headers).username();

            // why are we supposed to use records if the username fields are mutable
            // i could create a class copy of it and have a builder method to turn it into a record when both players are set
            // and the game started, but that is wayyyyyy to complex for this
            // so guess i gotta make a copy, delete the old one and add the new one :(
            GameData old = DataAccess.INSTANCE.getGameData(joinGameData.gameID());
            GameData newData = new GameData(
                    old.gameID(),
                    joinGameData.playerColor() == PlayerColor.WHITE ? username : old.whiteUsername(),
                    joinGameData.playerColor() == PlayerColor.BLACK ? username : old.blackUsername(),
                    old.gameName(),
                    old.game()
            );
            DataAccess.INSTANCE.removeGameData(joinGameData.gameID());
            DataAccess.INSTANCE.addGameData(newData);

            return "";
        } catch (Exception e) {
            if (e instanceof HTTPExepction) {
                throw e; // rethrow the custom exception
            }
            throw new HTTPExepction(new ErrorModel("Error: " + e.getMessage()), 500);
        }
    }
}
