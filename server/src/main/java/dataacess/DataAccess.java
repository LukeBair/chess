package dataacess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.HashMap;

public class DataAccess {
    public final static DataAccess INSTANCE = new DataAccess();

    private final HashMap<String, UserData> userDataMap = new HashMap<>();
    private final HashMap<String, AuthData> authDataMap = new HashMap<>();
    private final HashMap<Integer, GameData> gameDataHashMap = new HashMap<>();

    public void addUser(UserData userData) {
        userDataMap.put(userData.username(), userData);
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
        authDataMap.put(authData.username(), authData);
    }

    public AuthData getAuthDataByUsername(String username) {
        return authDataMap.get(username);
    }

    // This is kinda stupid but it works
    public AuthData getAuthDataByAuthToken(String authToken) {
        return authDataMap.values().stream().filter(authData -> authData.authToken().equals(authToken)).findFirst().orElse(null);
    }

    // This is kinda stupid but it works
    public boolean authDataExistsByAuthToken(String authToken) {
        return authDataMap.values().stream().anyMatch(authData -> authData.authToken().equals(authToken));
    }

    public boolean authDataExistsByUsername(String username) {
        return authDataMap.containsKey(username);
    }

    public void removeAuthData(String username) {
        authDataMap.remove(username);
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

    public void removeGameData(Integer gameId) {
        gameDataHashMap.remove(gameId);
    }

    public void clear() {
        userDataMap.clear();
        authDataMap.clear();
        gameDataHashMap.clear();
    }

    public GameData[] getAllGames() {
        return gameDataHashMap.values().toArray(new GameData[0]);
    }
}
