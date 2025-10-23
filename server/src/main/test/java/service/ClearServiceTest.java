package java.service;

import models.*;
import service.ClearService;
import service.GameService;
import service.UserService;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClearServiceTest {
    private DataAccess dataAccess;
    private ClearService clearService;
    private UserService userService;
    private GameService gameService;

    @BeforeEach
    void setUp() throws DataAccessException {
        dataAccess = new MemoryDataAccess();
        clearService = new ClearService(dataAccess);
        userService = new UserService(dataAccess);
        gameService = new GameService(dataAccess);
        dataAccess.clear();
    }

    @Test
    void clearPositive() throws DataAccessException {
        // Add multiple users
        RegisterRequest regReq1 = new RegisterRequest("user1", "pass1", "email1@test.com");
        RegisterRequest regReq2 = new RegisterRequest("user2", "pass2", "email2@test.com");
        RegisterRequest regReq3 = new RegisterRequest("user3", "pass3", "email3@test.com");

        RegisterResult regResult1 = userService.register(regReq1);
        RegisterResult regResult2 = userService.register(regReq2);
        RegisterResult regResult3 = userService.register(regReq3);

        // Create multiple games
        CreateGameRequest createReq1 = new CreateGameRequest("Game 1");
        CreateGameRequest createReq2 = new CreateGameRequest("Game 2");
        CreateGameRequest createReq3 = new CreateGameRequest("Game 3");
        CreateGameRequest createReq4 = new CreateGameRequest("Game 4");

        CreateGameResult gameResult1 = gameService.createGame(createReq1, regResult1.authToken());
        CreateGameResult gameResult2 = gameService.createGame(createReq2, regResult1.authToken());
        CreateGameResult gameResult3 = gameService.createGame(createReq3, regResult2.authToken());
        CreateGameResult gameResult4 = gameService.createGame(createReq4, regResult3.authToken());

        // Join some games to create more data
        JoinGameRequest joinReq1 = new JoinGameRequest("WHITE", gameResult1.gameID());
        JoinGameRequest joinReq2 = new JoinGameRequest("BLACK", gameResult1.gameID());
        gameService.joinGame(joinReq1, regResult1.authToken());
        gameService.joinGame(joinReq2, regResult2.authToken());

        // Verify data exists before clear
        assertNotNull(dataAccess.getUser("user1"), "User1 should exist");
        assertNotNull(dataAccess.getUser("user2"), "User2 should exist");
        assertNotNull(dataAccess.getUser("user3"), "User3 should exist");
        assertNotNull(dataAccess.getAuth(regResult1.authToken()), "Auth1 should exist");
        assertNotNull(dataAccess.getAuth(regResult2.authToken()), "Auth2 should exist");
        assertNotNull(dataAccess.getAuth(regResult3.authToken()), "Auth3 should exist");
        assertNotNull(dataAccess.getGame(gameResult1.gameID()), "Game1 should exist");
        assertNotNull(dataAccess.getGame(gameResult2.gameID()), "Game2 should exist");
        assertNotNull(dataAccess.getGame(gameResult3.gameID()), "Game3 should exist");
        assertNotNull(dataAccess.getGame(gameResult4.gameID()), "Game4 should exist");
        assertEquals(4, dataAccess.listGames().size(), "Should have 4 games");

        // Clear the database
        clearService.clear();

        // Verify ALL users are deleted
        assertNull(dataAccess.getUser("user1"), "User1 should be deleted");
        assertNull(dataAccess.getUser("user2"), "User2 should be deleted");
        assertNull(dataAccess.getUser("user3"), "User3 should be deleted");

        assertNull(dataAccess.getAuth(regResult1.authToken()), "Auth1 should be deleted");
        assertNull(dataAccess.getAuth(regResult2.authToken()), "Auth2 should be deleted");
        assertNull(dataAccess.getAuth(regResult3.authToken()), "Auth3 should be deleted");

        assertNull(dataAccess.getGame(gameResult1.gameID()), "Game1 should be deleted");
        assertNull(dataAccess.getGame(gameResult2.gameID()), "Game2 should be deleted");
        assertNull(dataAccess.getGame(gameResult3.gameID()), "Game3 should be deleted");
        assertNull(dataAccess.getGame(gameResult4.gameID()), "Game4 should be deleted");

        assertTrue(dataAccess.listGames().isEmpty(), "Games list should be empty");
    }
}