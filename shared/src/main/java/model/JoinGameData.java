package model;

public record JoinGameData(PlayerColor playerColor, Integer gameID) {
    public JoinGameData {
        if (playerColor == null || gameID == null) {
            throw new IllegalArgumentException("Player color and game ID cannot be null");
        }
    }
}
