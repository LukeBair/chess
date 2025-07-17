package model;

public record AuthData(String username, String authToken) {
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AuthData) {
            return username.equals(((AuthData) obj).username) && authToken.equals(((AuthData) obj).authToken);
        }
        return false;
    }
}
