package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import models.*;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class UserService {
    private final DataAccess dao;

    public UserService(DataAccess dao) {
        this.dao = dao;
    }

    public RegisterResult register(RegisterRequest request) {
        try {
            if (request.username() == null || request.password() == null || request.email() == null ||
                    request.username().trim().isEmpty() || request.password().trim().isEmpty() || request.email().trim().isEmpty()) {
                return new RegisterResult(null, null, "Error: bad request");
            }
            if (dao.getUser(request.username()) != null) {
                return new RegisterResult(null, null, "Error: already taken");
            }
            String hashedPassword = BCrypt.hashpw(request.password(), BCrypt.gensalt());
            UserData user = new UserData(request.username(), hashedPassword, request.email());
            dao.insertUser(user);

            String authToken = UUID.randomUUID().toString();
            AuthData authData = new AuthData(authToken, user.username());
            dao.createAuth(authData);

            return new RegisterResult(user.username(), authToken, null);
        } catch (DataAccessException e) {
            return new RegisterResult(null, null, "Error: " + e.getMessage());
        }
    }

    public LoginResult login(LoginRequest request) {
        try {
            if (request.username() == null || request.password() == null ||
                    request.username().trim().isEmpty() || request.password().trim().isEmpty()) {
                return new LoginResult(null, null, "Error: bad request");
            }
            UserData user = dao.getUser(request.username());
            if (user == null || !BCrypt.checkpw(request.password(), user.password())) {
                return new LoginResult(null, null, "Error: unauthorized");
            }
            String authToken = UUID.randomUUID().toString();
            AuthData authData = new AuthData(authToken, user.username());
            dao.createAuth(authData);
            return new LoginResult(user.username(), authToken, null);
        } catch (DataAccessException e) {
            return new LoginResult(null, null, "Error: " + e.getMessage());
        }
    }

    public LogoutResult logout(String authToken) {
        try {
            if (authToken == null || authToken.trim().isEmpty()) {
                return new LogoutResult("Error: unauthorized");
            }
            AuthData authData = dao.getAuth(authToken);
            if (authData == null) {
                return new LogoutResult("Error: unauthorized");
            }
            dao.deleteAuth(authToken);
            return new LogoutResult(null);
        } catch (DataAccessException e) {
            return new LogoutResult("Error: " + e.getMessage());
        }
    }
}