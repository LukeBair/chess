package java.service;

import Service.GameService;
import Service.UserService;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.Req_Res.*;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {
    private DataAccess dataAccess;
    private GameService gameService;
    private UserService userService;
    private String validAuthToken;

    @BeforeEach
    void setUp() throws DataAccessException {
        dataAccess = new MemoryDataAccess();
        gameService = new GameService(dataAccess);
        userService = new UserService(dataAccess);
        dataAccess.clear();

        // Register a user and get auth token for testing
        RegisterRequest regRequest = new RegisterRequest("testuser", "password123", "test@email.com");
        RegisterResult regResult = userService.register(regRequest);
        validAuthToken = regResult.authToken();
    }

    // List Games Tests
    @Test
    void listGamesPositive() throws DataAccessException {
        // Create some games
        CreateGameRequest createReq1 = new CreateGameRequest("Game 1");
        CreateGameRequest createReq2 = new CreateGameRequest("Game 2");
        gameService.createGame(createReq1, validAuthToken);
        gameService.createGame(createReq2, validAuthToken);

        // List games
        ListGamesResult result = gameService.listGames(validAuthToken);

        assertNull(result.message(), "Should not have error message");
        assertNotNull(result.games(), "Should return games list");
        assertEquals(2, result.games().size(), "Should return 2 games");
    }

    @Test
    void listGamesNegativeUnauthorized() {
        ListGamesResult result = gameService.listGames("invalid-token");

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("unauthorized"), "Should indicate unauthorized");
        assertNull(result.games(), "Should not return games list");
    }

    @Test
    void listGamesPositiveEmptyList() {
        ListGamesResult result = gameService.listGames(validAuthToken);

        assertNull(result.message(), "Should not have error message");
        assertNotNull(result.games(), "Should return games list");
        assertEquals(0, result.games().size(), "Should return empty list");
    }

    @Test
    void listGamesNegativeNullToken() {
        ListGamesResult result = gameService.listGames(null);

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("unauthorized"));
    }

    @Test
    void listGamesNegativeEmptyToken() {
        ListGamesResult result = gameService.listGames("");

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("unauthorized"));
    }

    // Create Game Tests
    @Test
    void createGamePositive() throws DataAccessException {
        CreateGameRequest request = new CreateGameRequest("My Chess Game");
        CreateGameResult result = gameService.createGame(request, validAuthToken);

        assertNull(result.message(), "Should not have error message");
        assertTrue(result.gameID() > 0, "Should return valid game ID");

        // Verify game was created
        GameData game = dataAccess.getGame(result.gameID());
        assertNotNull(game, "Game should exist in database");
        assertEquals("My Chess Game", game.gameName());
        assertNull(game.whiteUsername(), "White username should be null");
        assertNull(game.blackUsername(), "Black username should be null");
    }

    @Test
    void createGameNegativeUnauthorized() {
        CreateGameRequest request = new CreateGameRequest("Game Name");
        CreateGameResult result = gameService.createGame(request, "invalid-token");

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("unauthorized"), "Should indicate unauthorized");
        assertEquals(-1, result.gameID(), "Should return -1 for game ID");
    }

    @Test
    void createGameNegativeBadRequestNullName() {
        CreateGameRequest request = new CreateGameRequest(null);
        CreateGameResult result = gameService.createGame(request, validAuthToken);

        assertNotNull(result.message());
        assertTrue(result.message().contains("bad request"));
        assertEquals(-1, result.gameID());
    }

    @Test
    void createGameNegativeBadRequestEmptyName() {
        CreateGameRequest request = new CreateGameRequest("");
        CreateGameResult result = gameService.createGame(request, validAuthToken);

        assertNotNull(result.message());
        assertTrue(result.message().contains("bad request"));
        assertEquals(-1, result.gameID());
    }

    @Test
    void createGameNegativeNullToken() {
        CreateGameRequest request = new CreateGameRequest("Game");
        CreateGameResult result = gameService.createGame(request, null);

        assertTrue(result.message().contains("unauthorized"));
        assertEquals(-1, result.gameID());
    }

    @Test
    void createGamePositiveMultipleGames() throws DataAccessException {
        CreateGameRequest request1 = new CreateGameRequest("Game 1");
        CreateGameRequest request2 = new CreateGameRequest("Game 2");
        CreateGameRequest request3 = new CreateGameRequest("Game 3");

        CreateGameResult result1 = gameService.createGame(request1, validAuthToken);
        CreateGameResult result2 = gameService.createGame(request2, validAuthToken);
        CreateGameResult result3 = gameService.createGame(request3, validAuthToken);

        assertNull(result1.message());
        assertNull(result2.message());
        assertNull(result3.message());

        // All game IDs should be different
        assertNotEquals(result1.gameID(), result2.gameID());
        assertNotEquals(result2.gameID(), result3.gameID());
        assertNotEquals(result1.gameID(), result3.gameID());
    }

    // Join Game Tests
    @Test
    void joinGamePositiveWhite() throws DataAccessException {
        // Create a game
        CreateGameRequest createReq = new CreateGameRequest("Test Game");
        CreateGameResult createResult = gameService.createGame(createReq, validAuthToken);
        int gameID = createResult.gameID();

        // Join as white
        JoinGameRequest joinReq = new JoinGameRequest("WHITE", gameID);
        JoinGameResult result = gameService.joinGame(joinReq, validAuthToken);

        assertNull(result.message(), "Should not have error message");

        // Verify game was updated
        GameData game = dataAccess.getGame(gameID);
        assertEquals("testuser", game.whiteUsername(), "White player should be set");
        assertNull(game.blackUsername(), "Black player should still be null");
    }

    @Test
    void joinGamePositiveBlack() throws DataAccessException {
        // Create a game
        CreateGameRequest createReq = new CreateGameRequest("Test Game");
        CreateGameResult createResult = gameService.createGame(createReq, validAuthToken);
        int gameID = createResult.gameID();

        // Join as black
        JoinGameRequest joinReq = new JoinGameRequest("BLACK", gameID);
        JoinGameResult result = gameService.joinGame(joinReq, validAuthToken);

        assertNull(result.message(), "Should not have error message");

        // Verify game was updated
        GameData game = dataAccess.getGame(gameID);
        assertNull(game.whiteUsername(), "White player should still be null");
        assertEquals("testuser", game.blackUsername(), "Black player should be set");
    }

    @Test
    void joinGameNegativeAlreadyTakenWhite() throws DataAccessException {
        // Create a game and join as white
        CreateGameRequest createReq = new CreateGameRequest("Test Game");
        CreateGameResult createResult = gameService.createGame(createReq, validAuthToken);
        int gameID = createResult.gameID();

        JoinGameRequest joinReq1 = new JoinGameRequest("WHITE", gameID);
        gameService.joinGame(joinReq1, validAuthToken);

        // Register second user and try to join as white
        RegisterRequest regReq2 = new RegisterRequest("user2", "pass", "email2@test.com");
        RegisterResult regResult2 = userService.register(regReq2);

        JoinGameRequest joinReq2 = new JoinGameRequest("WHITE", gameID);
        JoinGameResult result = gameService.joinGame(joinReq2, regResult2.authToken());

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("already taken"), "Should indicate spot is taken");
    }

    @Test
    void joinGameNegativeAlreadyTakenBlack() throws DataAccessException {
        // Create a game and join as black
        CreateGameRequest createReq = new CreateGameRequest("Test Game");
        CreateGameResult createResult = gameService.createGame(createReq, validAuthToken);
        int gameID = createResult.gameID();

        JoinGameRequest joinReq1 = new JoinGameRequest("BLACK", gameID);
        gameService.joinGame(joinReq1, validAuthToken);

        // Register second user and try to join as black
        RegisterRequest regReq2 = new RegisterRequest("user2", "pass", "email2@test.com");
        RegisterResult regResult2 = userService.register(regReq2);

        JoinGameRequest joinReq2 = new JoinGameRequest("BLACK", gameID);
        JoinGameResult result = gameService.joinGame(joinReq2, regResult2.authToken());

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("already taken"), "Should indicate spot is taken");
    }

    @Test
    void joinGameNegativeUnauthorized() throws DataAccessException {
        CreateGameRequest createReq = new CreateGameRequest("Test Game");
        CreateGameResult createResult = gameService.createGame(createReq, validAuthToken);

        JoinGameRequest joinReq = new JoinGameRequest("WHITE", createResult.gameID());
        JoinGameResult result = gameService.joinGame(joinReq, "invalid-token");

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("unauthorized"));
    }

    @Test
    void joinGameNegativeBadRequestNullGameID() {
        JoinGameRequest joinReq = new JoinGameRequest("WHITE", null);
        JoinGameResult result = gameService.joinGame(joinReq, validAuthToken);

        assertNotNull(result.message());
        assertTrue(result.message().contains("bad request"));
    }

    @Test
    void joinGameNegativeBadRequestInvalidColor() throws DataAccessException {
        CreateGameRequest createReq = new CreateGameRequest("Test Game");
        CreateGameResult createResult = gameService.createGame(createReq, validAuthToken);

        JoinGameRequest joinReq = new JoinGameRequest("RED", createResult.gameID());
        JoinGameResult result = gameService.joinGame(joinReq, validAuthToken);

        assertNotNull(result.message());
        assertTrue(result.message().contains("bad request"));
    }

    @Test
    void joinGameNegativeBadRequestNullColor() throws DataAccessException {
        CreateGameRequest createReq = new CreateGameRequest("Test Game");
        CreateGameResult createResult = gameService.createGame(createReq, validAuthToken);

        JoinGameRequest joinReq = new JoinGameRequest(null, createResult.gameID());
        JoinGameResult result = gameService.joinGame(joinReq, validAuthToken);

        assertNotNull(result.message());
        assertTrue(result.message().contains("bad request"));
    }

    @Test
    void joinGameNegativeGameDoesNotExist() {
        JoinGameRequest joinReq = new JoinGameRequest("WHITE", 99999);
        JoinGameResult result = gameService.joinGame(joinReq, validAuthToken);

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("bad request"), "Should indicate bad request");
    }

    @Test
    void joinGamePositiveCaseInsensitiveColor() throws DataAccessException {
        // Create a game
        CreateGameRequest createReq = new CreateGameRequest("Test Game");
        CreateGameResult createResult = gameService.createGame(createReq, validAuthToken);
        int gameID = createResult.gameID();

        // Join with lowercase color
        JoinGameRequest joinReq = new JoinGameRequest("white", gameID);
        JoinGameResult result = gameService.joinGame(joinReq, validAuthToken);

        assertNull(result.message(), "Should accept lowercase color");
        GameData game = dataAccess.getGame(gameID);
        assertEquals("testuser", game.whiteUsername());
    }

    @Test
    void joinGamePositiveBothPlayersDifferentUsers() throws DataAccessException {
        // Create a game
        CreateGameRequest createReq = new CreateGameRequest("Test Game");
        CreateGameResult createResult = gameService.createGame(createReq, validAuthToken);
        int gameID = createResult.gameID();

        // First user joins as white
        JoinGameRequest joinReq1 = new JoinGameRequest("WHITE", gameID);
        JoinGameResult result1 = gameService.joinGame(joinReq1, validAuthToken);
        assertNull(result1.message());

        // Register and login second user
        RegisterRequest regReq2 = new RegisterRequest("user2", "pass2", "email2@test.com");
        RegisterResult regResult2 = userService.register(regReq2);

        // Second user joins as black
        JoinGameRequest joinReq2 = new JoinGameRequest("BLACK", gameID);
        JoinGameResult result2 = gameService.joinGame(joinReq2, regResult2.authToken());
        assertNull(result2.message());

        // Verify both players are set
        GameData game = dataAccess.getGame(gameID);
        assertEquals("testuser", game.whiteUsername());
        assertEquals("user2", game.blackUsername());
    }
}