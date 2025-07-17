package service;

import com.google.gson.Gson;
import model.AuthData;
import model.ErrorModel;
import model.UserData;
import spark.Request;
import spark.Response;

import java.util.UUID;

public class UserService {
    private final Gson serializer = new Gson();

    public String createUser(Request req, Response res) {
        try {
            var input = serializer.fromJson(req.body(), UserData.class);

            if (!input.isValid()) {
                res.status(400);
                return serializer.toJson(new ErrorModel("Error: bad request"));
            }

            //TODO: email validation ???

            var userData = new UserData(input.username(), input.password(), input.email());

            res.status(200);
            return serializer.toJson(userData);
        } catch (Exception e) {
            res.status(500);
            return serializer.toJson(new ErrorModel("Error: " + e.getMessage()));
        }
    }

    public String login(Request req, Response res) {
        try {
            var input = serializer.fromJson(req.body(), UserData.class);

            if(!input.isValid()) {
                res.status(401);
                return serializer.toJson(new ErrorModel("Error: unauthorized"));
            }

            var authData = new AuthData(input.username(), generateToken());

            res.status(200);
            return serializer.toJson(authData);
        } catch (Exception e) {
            res.status(500);
            return serializer.toJson(new ErrorModel("Error: " + e.getMessage()));
        }
    }

    public String logout(Request req, Response res) {
        try {
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
