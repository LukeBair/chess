import com.google.gson.Gson;
import model.UserData;
import service.UserService;

public class Tests {
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(UserService.generateToken());
        }
    }
}
