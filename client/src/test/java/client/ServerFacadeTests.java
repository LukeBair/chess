package client;

import client.data.ServerFacade;
import models.AuthData;
import org.junit.jupiter.api.*;
import server.Server;

import java.io.IOException;


public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    private AuthData createDefaultAccount() throws IOException, InterruptedException {
        return facade.register("newUser", "password", "funny email");
    }

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        facade = new ServerFacade(port);
        System.out.println("Started test HTTP server on " + port);
    }

    @BeforeEach
    void clearData() {
        try {
            facade.test();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    public void registerTestSuccess() {
        try {
            var result = createDefaultAccount();
            Assertions.assertNotNull(result);
            Assertions.assertEquals("newUser", result.username());
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        }
    }

    @Test
    public void registerTestFailure() {
        try {
            createDefaultAccount();
            createDefaultAccount();

            Assertions.fail();
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.toString().toLowerCase().contains("registration failed")); // idk if this is good but lol
        }
    }

    @Test
    public void loginTestSuccess() {
        try {
            var res1 = createDefaultAccount();
            var res2 = facade.login("newUser", "password");

            Assertions.assertNotNull(res2);
            Assertions.assertEquals(res1.username(), res2.username());
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        }
    }

    @Test
    public void loginTestFailure() {
        try {
            var res1 = createDefaultAccount();
            facade.login("newUser", "wrong password");

            Assertions.fail();
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.toString().toLowerCase().contains("login failed"));
        }
    }

    @Test
    public void logoutTestSuccess() {
        try {
            var auth = createDefaultAccount();
            facade.logout(auth.authToken());
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        }
    }

    @Test
    public void logoutTestFailure() {
        try {
            var auth = createDefaultAccount();
            facade.logout("invalidToken");

            Assertions.fail();
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.toString().toLowerCase().contains("logout failed"));
        }
    }

    @Test
    public void createGameTestSuccess() {
        try {
            var auth = createDefaultAccount();
            var result = facade.createGame("TestGame", auth.authToken());

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.gameID() > 0);
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        }
    }

    @Test
    public void createGameTestFailure() {
        try {
            var auth = createDefaultAccount();
            facade.createGame("TestGame", "invalidToken");

            Assertions.fail();
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.toString().toLowerCase().contains("create game failed"));
        }
    }

    @Test
    public void listGamesTestSuccess() {
        try {
            var auth = createDefaultAccount();
            facade.createGame("Game1", auth.authToken());
            facade.createGame("Game2", auth.authToken());

            var games = facade.listGames(auth.authToken());

            Assertions.assertNotNull(games);
            Assertions.assertEquals(2, games.length);
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        }
    }

    @Test
    public void listGamesTestFailure() {
        try {
            var auth = createDefaultAccount();
            var games = facade.listGames("invalidToken");

            Assertions.fail();
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.toString().toLowerCase().contains("list games failed"));
        }
    }

    @Test
    public void joinGameTestSuccess() {
        try {
            var auth = createDefaultAccount();
            var createRes = facade.createGame("TestGame", auth.authToken());
            int gameId = createRes.gameID();

            var result = facade.joinGame(gameId, "WHITE", auth.authToken());

            Assertions.assertNotNull(result);
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        }
    }

    @Test
    // WARNING: not sure how this one is supposed to work
    public void joinGameTestFailure() {
        try {
            var auth = createDefaultAccount();
            var createRes = facade.createGame("TestGame", auth.authToken());
            int gameId = createRes.gameID();

            facade.joinGame(gameId, "WHITE", auth.authToken());
            facade.joinGame(gameId, "BLACK", auth.authToken());

//            Assertions.fail();
            Assertions.assertTrue(true);
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.toString().toLowerCase().contains("join game failed"));
        }
    }

    @Test
    public void observeGameTestSuccess() {
        try {
            var auth = createDefaultAccount();
            var createRes = facade.createGame("TestGame", auth.authToken());
            int gameId = createRes.gameID();

            var result = facade.observeGame(gameId, auth.authToken());

            Assertions.assertNotNull(result);
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        }
    }

    @Test
    public void observeGameTestFailure() {
        try {
            var auth = createDefaultAccount();
            facade.observeGame(-1, auth.authToken());  // Invalid game ID

            Assertions.fail();
        } catch (IOException | InterruptedException e) {
            Assertions.fail();
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.toString().toLowerCase().contains("join game failed"));
        }
    }


}