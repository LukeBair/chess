import com.google.gson.Gson;
import model.UserData;

public class Tests {
    public static void main(String[] args) {
        UserData userData = new UserData("test", "pass", "test@example.com");
        System.out.println(new Gson().toJson(userData));
    }
}
