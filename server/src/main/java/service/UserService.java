package service;

import com.google.gson.Gson;
import dataacess.DataAccess;
import model.AuthData;
import model.ErrorModel;
import model.LogoutHeader;
import model.UserData;
import spark.Request;
import spark.Response;

import javax.xml.crypto.Data;
import java.util.UUID;

public class UserService {
    private final Gson serializer = new Gson();

    public String createUser(Request req, Response res) {
        try {
            var input = serializer.fromJson(req.body(), UserData.class);

            if (!input.isValid()) {
                res.status(400);
                return serializer.toJson(new ErrorModel("Error: already taken"));
            }

            if(DataAccess.INSTANCE.userExists(input.username())) {
                res.status(403);
                return serializer.toJson(new ErrorModel("Error: user already exists"));
            }

            //TODO: email validation ???

            var userData = new UserData(input.username(), input.password(), input.email());
            DataAccess.INSTANCE.addUser(userData);
            var authData = new AuthData(userData.username(), generateToken());
            DataAccess.INSTANCE.addAuthData(authData);
            res.status(200);
            return serializer.toJson(authData);
        } catch (Exception e) {
            res.status(500);
            return serializer.toJson(new ErrorModel("Error: " + e.getMessage()));
        }
    }

    public String login(Request req, Response res) {
        try {
            var input = serializer.fromJson(req.body(), UserData.class);

            if(!input.isValid()) {
                res.status(400);
                return serializer.toJson(new ErrorModel("Error: unauthorized"));
            }

            if(!DataAccess.INSTANCE.userExists(input.username())) {
                res.status(401);
                return serializer.toJson(new ErrorModel("Error: unauthorized"));
            }

            if(!DataAccess.INSTANCE.getUser(input.username()).password().equals(input.password())) {
                res.status(401);
                return serializer.toJson(new ErrorModel("Error: unauthorized"));
            }
            var authData = DataAccess.INSTANCE.getAuthData(input.username());
            res.status(200);
            return serializer.toJson(authData);
        } catch (Exception e) {
            res.status(500);
            return serializer.toJson(new ErrorModel("Error: " + e.getMessage()));
        }
    }

    public String logout(Request req, Response res) {
        try {
            LogoutHeader logoutHeader = serializer.fromJson(req.body(), LogoutHeader.class);

            if (!DataAccess.INSTANCE.authDataExists(logoutHeader.authorization())) {
                res.status(401);
                return serializer.toJson(new ErrorModel("Error: unauthorized"));
            }
            DataAccess.INSTANCE.removeUser(DataAccess.INSTANCE.getAuthData(logoutHeader.authorization()).username());
            DataAccess.INSTANCE.removeAuthData(logoutHeader.authorization());
            res.status(200);
            return "";
        } catch (Exception e) {
            res.status(500);
            return serializer.toJson(new ErrorModel("Error: " + e.getMessage()));
        }
    }

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }
}
