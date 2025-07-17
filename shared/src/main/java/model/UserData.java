package model;

public record UserData(String username, String password, String email) {
    public boolean isValid() {
        if(username == null || password == null || email == null) { return false; }
        return true;
    }
}
