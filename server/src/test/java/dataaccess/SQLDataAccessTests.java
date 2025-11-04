package dataaccess;

import chess.ChessGame;
import models.AuthData;
import models.GameData;
import models.UserData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SQLDataAccessTests {
    private static SQLDataAccess sqlDataAccess;

    @BeforeAll
    public static void setup() {
        try {
            sqlDataAccess = new SQLDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException("Unable to initialize SQLDataAccess: " + e.getMessage());
        }
    }

    @BeforeEach
    public void clearLastState() {
        try {
            sqlDataAccess.clear();
        } catch (DataAccessException e) {
            throw new RuntimeException("Unable to clear database: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Successfull clear database test")
    public void clearDatabaseTest() {
        try {
            UserData user = new UserData("testuser", "password", "test@email.com");
            sqlDataAccess.insertUser(user);

            AuthData auth = new AuthData("token123", "testuser");
            sqlDataAccess.createAuth(auth);

            GameData game = new GameData(1, "white", "black", "game", null);
            sqlDataAccess.createGame(game);

            sqlDataAccess.clear();

            assertNull(sqlDataAccess.getUser("testuser"));
            assertNull(sqlDataAccess.getAuth("token123"));
            assertNull(sqlDataAccess.getGame(1));
            assertTrue(sqlDataAccess.listGames().isEmpty());
        } catch (DataAccessException e) {
            fail("DataAccessException thrown: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Successful Insert User")
    public void insertUserTest() {
        try {
            UserData user = new UserData("testuser", "password", "test@gmail.com");
            sqlDataAccess.insertUser(user);

            UserData retrievedUser = sqlDataAccess.getUser("testuser");
            assertNotNull(retrievedUser);
            assertEquals("testuser", retrievedUser.username());
            assertEquals("password", retrievedUser.password());
            assertEquals("test@gmail.com", retrievedUser.email());
        } catch (DataAccessException e) {
            fail("DataAccessException thrown: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Failed Insert User - Duplicate Username")
    public void insertUserDuplicateTest() {
        try {
            UserData user1 = new UserData("testuser", "password1", "test@gmail.com");
            sqlDataAccess.insertUser(user1);
            UserData user2 = new UserData("testuser", "password2", "test2@gmail.com");
            assertThrows(DataAccessException.class, () -> sqlDataAccess.insertUser(user2));
        } catch (DataAccessException e) {
            fail("DataAccessException thrown: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Successful Get User")
    public void getUserPositive() throws DataAccessException {
        UserData user = new UserData("joe", "password", "test@gmail.com");
        sqlDataAccess.insertUser(user);

        UserData retrieved = sqlDataAccess.getUser("joe");
        assertNotNull(retrieved);
        assertEquals("joe", retrieved.username());
        assertEquals("password", retrieved.password());
        assertEquals("test@gmail.com", retrieved.email());
    }

    @Test
    @DisplayName("Failed Get User - User Does Not Exist")
    public void getUserNegative() throws DataAccessException {
        UserData retrieved = sqlDataAccess.getUser("nonexistent");
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Successful Create Game")
    public void createGamePositive() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(1, "white", "black", "game1", game);

        int gameID = sqlDataAccess.createGame(gameData);
        assertEquals(1, gameID);

        GameData retrieved = sqlDataAccess.getGame(1);
        assertNotNull(retrieved);
        assertEquals("game1", retrieved.gameName());
        assertEquals("white", retrieved.whiteUsername());
        assertEquals("black", retrieved.blackUsername());
    }

    @Test
    @DisplayName("Failed Create Game - Duplicate Game ID")
    public void createGameNegative() throws DataAccessException {
        ChessGame game1 = new ChessGame();
        GameData gameData1 = new GameData(1, "white1", "black1", "game1", game1);
        sqlDataAccess.createGame(gameData1);

        ChessGame game2 = new ChessGame();
        GameData gameData2 = new GameData(1, "white2", "black2", "game2", game2);
        assertThrows(DataAccessException.class, () -> sqlDataAccess.createGame(gameData2));
    }

    @Test
    @DisplayName("Successful Get Game")
    public void getGamePositive() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(1, "white", "black", "game1", game);
        sqlDataAccess.createGame(gameData);

        GameData retrieved = sqlDataAccess.getGame(1);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.gameID());
        assertEquals("white", retrieved.whiteUsername());
        assertEquals("black", retrieved.blackUsername());
        assertEquals("game1", retrieved.gameName());
        assertNotNull(retrieved.game());
    }

    @Test
    @DisplayName("Failed Get Game - Game Does Not Exist")
    public void getGameNegative() throws DataAccessException {
        GameData retrieved = sqlDataAccess.getGame(9999);
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Successful List Games")
    public void listGamesPositive() throws DataAccessException {
        ChessGame game1 = new ChessGame();
        ChessGame game2 = new ChessGame();
        ChessGame game3 = new ChessGame();

        sqlDataAccess.createGame(new GameData(1, "a", "b", "game1", game1));
        sqlDataAccess.createGame(new GameData(2, "c", "d", "game2", game2));
        sqlDataAccess.createGame(new GameData(3, "e", "f", "game3", game3));

        var games = sqlDataAccess.listGames();
        assertEquals(3, games.size());
    }

    @Test
    @DisplayName("Failed List Games - Empty List")
    public void listGamesNegative() throws DataAccessException {
        var games = sqlDataAccess.listGames();
        assertNotNull(games);
        assertTrue(games.isEmpty());
    }

    @Test
    @DisplayName("Successful Update Game")
    public void updateGamePositive() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData original = new GameData(1, null, null, "original", game);
        sqlDataAccess.createGame(original);

        GameData updated = new GameData(1, "white", "black", "updated", game);
        sqlDataAccess.updateGame(updated);

        GameData retrieved = sqlDataAccess.getGame(1);
        assertNotNull(retrieved);
        assertEquals("white", retrieved.whiteUsername());
        assertEquals("black", retrieved.blackUsername());
        assertEquals("updated", retrieved.gameName());
    }

    @Test
    @DisplayName("Failed Update Game - Game Does Not Exist")
    public void updateGameNegative() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(9999, "white", "black", "nonexistant", game);

        assertDoesNotThrow(() -> sqlDataAccess.updateGame(gameData));

        GameData retrieved = sqlDataAccess.getGame(9999);
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Successful Create Auth")
    public void createAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("authtoken", "testuser");
        sqlDataAccess.createAuth(auth);

        AuthData retrieved = sqlDataAccess.getAuth("authtoken");
        assertNotNull(retrieved);
        assertEquals("authtoken", retrieved.authToken());
        assertEquals("testuser", retrieved.username());
    }

    @Test
    @DisplayName("Failed Create Auth - Duplicate Auth Token")
    public void createAuthNegative() throws DataAccessException {
        AuthData auth1 = new AuthData("duplicate", "user1");
        sqlDataAccess.createAuth(auth1);

        AuthData auth2 = new AuthData("duplicate", "user2");
        assertThrows(DataAccessException.class, () -> sqlDataAccess.createAuth(auth2));
    }

    @Test
    @DisplayName("Successful Get Auth")
    public void getAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("authtoken", "name");
        sqlDataAccess.createAuth(auth);

        AuthData retrieved = sqlDataAccess.getAuth("authtoken");
        assertNotNull(retrieved);
        assertEquals("authtoken", retrieved.authToken());
        assertEquals("name", retrieved.username());
    }

    @Test
    @DisplayName("Failed Get Auth - Auth Does Not Exist")
    public void getAuthNegative() throws DataAccessException {
        AuthData retrieved = sqlDataAccess.getAuth("nonexistent");
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Successful Delete Auth")
    public void deleteAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("delete-this", "name");
        sqlDataAccess.createAuth(auth);

        assertNotNull(sqlDataAccess.getAuth("delete-this"));

        sqlDataAccess.deleteAuth("delete-this");

        assertNull(sqlDataAccess.getAuth("delete-this"));
    }

    @Test
    @DisplayName("Failed Delete Auth - Auth Does Not Exist")
    public void deleteAuthNegative() {
        assertThrows(DataAccessException.class,
                () -> sqlDataAccess.deleteAuth("nonexistent"));
    }

    @Test
    @DisplayName("Successful Integration Test - Store and Retrieve Complex Game State")
    public void complexGameStateTest() throws DataAccessException {
        ChessGame game = new ChessGame();
        game.getBoard().resetBoard();

        GameData gameData = new GameData(1, "player1", "player2", "game1", game);
        sqlDataAccess.createGame(gameData);

        GameData retrieved = sqlDataAccess.getGame(1);
        assertNotNull(retrieved);
        assertNotNull(retrieved.game());
        assertNotNull(retrieved.game().getBoard());

        assertEquals(game.getBoard().getPiece(new chess.ChessPosition(1, 1)),
                retrieved.game().getBoard().getPiece(new chess.ChessPosition(1, 1)));
    }
}
