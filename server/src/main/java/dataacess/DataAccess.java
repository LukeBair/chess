package dataacess;

import model.AuthData;
import model.GameData;
import model.GamesData;
import model.UserData;

import java.util.HashMap;

public class DataAccess {
    public final static DataAccess INSTANCE = new DataAccess();

    private final HashMap<String, UserData> userDataMap = new HashMap<>();
    private final HashMap<String, AuthData> authDataMap = new HashMap<>();
    private final HashMap<Integer, GameData> gameDataHashMap = new HashMap<>();

    public void addUser(UserData userData) {
        userDataMap.put(userData.username(), userData);
        DatabaseManager.addUser(userData);
    }

    public UserData getUser(String username) {
        return userDataMap.get(username);
    }

    public boolean userExists(String username) {
        return userDataMap.containsKey(username);
    }

    public void removeUser(String username) {
        userDataMap.remove(username);
    }

    public void addAuthData(AuthData authData) {
        authDataMap.put(authData.authToken(), authData);
    }

    public AuthData getAuthDataByAuthToken(String authToken) {
        return authDataMap.get(authToken);
    }

    public boolean authDataExistsByAuthToken(String authToken) {
        return authDataMap.containsKey(authToken);
    }

    public void removeAuthData(String authToken) {
        authDataMap.remove(authToken);
    }

    public void addGameData(GameData gameData) {
        gameDataHashMap.put(gameData.gameID(), gameData);
    }

    public GameData getGameData(Integer gameId) {
        return gameDataHashMap.get(gameId);
    }

    public boolean gameDataExists(Integer gameId) {
        return gameDataHashMap.containsKey(gameId);
    }

    public boolean gameDataExistsByGameName(String gameName) {
        return gameDataHashMap.values().stream().anyMatch(gameData -> gameData.gameName().equals(gameName));
    }


    public void removeGameData(Integer gameId) {
        gameDataHashMap.remove(gameId);
    }

    public void clear() {
        userDataMap.clear();
        authDataMap.clear();
        gameDataHashMap.clear();
    }

    public GamesData getAllGames() {
        return new GamesData(gameDataHashMap.values().toArray(new GameData[0]));
    }

    public Integer numGames() {
        return gameDataHashMap.size();
    }
}
