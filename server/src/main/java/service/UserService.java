package service;

import com.google.gson.Gson;
import model.AuthData;
import model.ErrorModel;
import model.UserData;
import spark.Request;
import spark.Response;

public class UserService {
    private final Gson serializer = new Gson();

    public String createUser(Request req, Response res) {
        try {
            var userData = new UserData("", "", "");

            res.status(200);
            return serializer.toJson(userData);
        } catch (Exception e) {
            res.status(500);
            return serializer.toJson(new ErrorModel("Error: " + e.getMessage()));
        }
    }

    public String login(Request req, Response res) {
        try {
            var authData = new AuthData("", "");

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
}
