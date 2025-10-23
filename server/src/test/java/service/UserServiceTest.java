package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import service.UserService;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    private DataAccess dataAccess;
    private UserService userService;

    @BeforeEach
    void setUp() throws DataAccessException {
        dataAccess = new MemoryDataAccess();
        userService = new UserService(dataAccess);
        dataAccess.clear();
    }

    // Register Tests
    @Test
    void registerPositive() throws DataAccessException {
        RegisterRequest request = new RegisterRequest("testuser", "password123", "test@email.com");
        RegisterResult result = userService.register(request);

        assertNull(result.message(), "Should not have error message");
        assertNotNull(result.authToken(), "Should return auth token");
        assertEquals("testuser", result.username(), "Should return correct username");

        // Verify user was created in database
        UserData user = dataAccess.getUser("testuser");
        assertNotNull(user, "User should exist in database");
        assertEquals("testuser", user.username());
        assertTrue(BCrypt.checkpw("password123", user.password()), "Password should be hashed correctly");
    }

    @Test
    void registerNegativeAlreadyTaken() throws DataAccessException {
        // Register first user
        RegisterRequest request1 = new RegisterRequest("testuser", "password123", "test@email.com");
        userService.register(request1);

        // Try to register same username
        RegisterRequest request2 = new RegisterRequest("testuser", "different", "other@email.com");
        RegisterResult result = userService.register(request2);

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("already taken"), "Should indicate username is taken");
        assertNull(result.authToken(), "Should not return auth token");
    }

    @Test
    void registerNegativeBadRequest() {
        // Null username
        RegisterRequest request1 = new RegisterRequest(null, "password", "email@test.com");
        RegisterResult result1 = userService.register(request1);
        assertTrue(result1.message().contains("bad request"));

        // Empty password
        RegisterRequest request2 = new RegisterRequest("user", "", "email@test.com");
        RegisterResult result2 = userService.register(request2);
        assertTrue(result2.message().contains("bad request"));

        // Null email
        RegisterRequest request3 = new RegisterRequest("user", "password", null);
        RegisterResult result3 = userService.register(request3);
        assertTrue(result3.message().contains("bad request"));
    }

    // Login Tests
    @Test
    void loginPositive() throws DataAccessException {
        // First register a user
        RegisterRequest regRequest = new RegisterRequest("testuser", "password123", "test@email.com");
        userService.register(regRequest);

        // Now login
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        LoginResult result = userService.login(loginRequest);

        assertNull(result.message(), "Should not have error message");
        assertNotNull(result.authToken(), "Should return auth token");
        assertEquals("testuser", result.username(), "Should return correct username");

        // Verify auth was created
        AuthData auth = dataAccess.getAuth(result.authToken());
        assertNotNull(auth, "Auth should exist in database");
        assertEquals("testuser", auth.username());
    }

    @Test
    void loginNegativeWrongPassword() throws DataAccessException {
        // Register a user
        RegisterRequest regRequest = new RegisterRequest("testuser", "password123", "test@email.com");
        userService.register(regRequest);

        // Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");
        LoginResult result = userService.login(loginRequest);

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("unauthorized"), "Should indicate unauthorized");
        assertNull(result.authToken(), "Should not return auth token");
    }

    @Test
    void loginNegativeUserDoesNotExist() {
        LoginRequest loginRequest = new LoginRequest("nonexistent", "password123");
        LoginResult result = userService.login(loginRequest);

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("unauthorized"), "Should indicate unauthorized");
        assertNull(result.authToken(), "Should not return auth token");
    }

    @Test
    void loginNegativeBadRequest() {
        // Null username
        LoginRequest request1 = new LoginRequest(null, "password");
        LoginResult result1 = userService.login(request1);
        assertTrue(result1.message().contains("bad request"));

        // Empty password
        LoginRequest request2 = new LoginRequest("user", "");
        LoginResult result2 = userService.login(request2);
        assertTrue(result2.message().contains("bad request"));
    }

    // Logout Tests
    @Test
    void logoutPositive() throws DataAccessException {
        // Register and get auth token
        RegisterRequest regRequest = new RegisterRequest("testuser", "password123", "test@email.com");
        RegisterResult regResult = userService.register(regRequest);
        String authToken = regResult.authToken();

        // Logout
        LogoutResult result = userService.logout(authToken);

        assertNull(result.message(), "Should not have error message");

        // Verify auth was deleted
        AuthData auth = dataAccess.getAuth(authToken);
        assertNull(auth, "Auth should be deleted from database");
    }

    @Test
    void logoutNegativeInvalidToken() {
        LogoutResult result = userService.logout("invalid-token-12345");

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("unauthorized"), "Should indicate unauthorized");
    }

    @Test
    void logoutNegativeNullToken() {
        LogoutResult result = userService.logout(null);

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("unauthorized"), "Should indicate unauthorized");
    }

    @Test
    void logoutNegativeEmptyToken() {
        LogoutResult result = userService.logout("");

        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("unauthorized"), "Should indicate unauthorized");
    }
}