package java.service;
import org.junit.jupiter.api.Test;
import service.UserService;
import spark.Request;
import spark.Response;
import spark.Spark;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceTest {
    @Test
    void createUserPos() {
        String body = """
                { "username":"melol", "password":"123cba", "email":"melol@lol.com" }
                """;

        UserService userService = new UserService();
        userService.createUser(body);
    }

    @Test
    void createUserNeg() {

    }

    @Test
    void loginUserPos() {

    }

    @Test
    void loginUserNeg() {

    }

    @Test
    void logoutUserPos() {

    }

    @Test
    void logoutUserNeg() {

    }

    @Test
    void createGamePos() {

    }

    @Test
    void createGameNeg() {

    }

    @Test
    void getAllGamePos() {

    }

    @Test
    void getAllGameNeg() {

    }

    @Test
    void joinGamePos() {

    }

    @Test
    void joinGameNeg() {

    }
}
