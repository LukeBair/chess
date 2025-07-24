package service;

import com.google.gson.Gson;
import dataacess.DataAccess;
import model.AuthData;
import model.ErrorModel;
import model.UserData;
import java.util.UUID;

public class UserService {
    private final Gson serializer = new Gson();

    public AuthData createUser(String body) {
        try {
            var input = serializer.fromJson(body, UserData.class);

            if (!input.isValid()) {
                throw new HTTPExepction(new ErrorModel("Error: bad request"), 400);
            }

            if(DataAccess.INSTANCE.userExists(input.username())) {
                throw new HTTPExepction(new ErrorModel("Error: already Taken"), 403);
            }

            //TODO: email validation ???

            var userData = new UserData(input.username(), input.password(), input.email());
            DataAccess.INSTANCE.addUser(userData);
            var authData = new AuthData(userData.username(), generateToken());
            DataAccess.INSTANCE.addAuthData(authData);
            return authData;
        } catch (Exception e) {
            if (e instanceof HTTPExepction) {
                throw e; // rethrow the custom exception
            }

            throw new HTTPExepction(new ErrorModel("Error: " + e.getMessage()), 500);
        }
    }

    // why the heck can you login multiple times with the same user?
    public AuthData login(String body) {
        try {
            var input = serializer.fromJson(body, UserData.class);

            if(!input.isValid()) {
                throw new HTTPExepction(new ErrorModel("Error: unauthorized"), 400);
            }

            if(!DataAccess.INSTANCE.userExists(input.username())) {
                throw new HTTPExepction(new ErrorModel("Error: unauthorized"), 401);
            }

            if(!DataAccess.INSTANCE.getUser(input.username()).password().equals(input.password())) {
                throw new HTTPExepction(new ErrorModel("Error: unauthorized"), 401);
            }
            var authData = new AuthData(input.username(), generateToken());
            DataAccess.INSTANCE.addAuthData(authData);
            return authData;
        } catch (Exception e) {
            if (e instanceof HTTPExepction) {
                throw e; // rethrow the custom exception
            }

            throw new HTTPExepction(new ErrorModel("Error: " + e.getMessage()), 500);
        }
    }

    public String logout(String headers) {
        try {
            if (!DataAccess.INSTANCE.authDataExistsByAuthToken(headers)) {
                throw new HTTPExepction(new ErrorModel("Error: unauthorized"), 401);
            }

            DataAccess.INSTANCE.removeUser(headers);
            DataAccess.INSTANCE.removeAuthData(headers);
            return "";
        } catch (Exception e) {
            if (e instanceof HTTPExepction) {
                throw e; // rethrow the custom exception
            }

            throw new HTTPExepction(new ErrorModel("Error: " + e.getMessage()), 500);
        }
    }

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }
}
