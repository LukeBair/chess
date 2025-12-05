package websocket.commands;

import chess.ChessMove;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * Represents a command a user can send the server over a websocket
 * <p>
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class UserGameCommand {

    private final CommandType commandType;

    private final String authToken;

    private final Integer gameID;

    public UserGameCommand(CommandType commandType, String authToken, Integer gameID) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
    }

    public enum CommandType {
        CONNECT,
        MAKE_MOVE,
        LEAVE,
        RESIGN
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Integer getGameID() {
        return gameID;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static UserGameCommand fromJson(String json) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        CommandType commandType = gson.fromJson(jsonObject.get("commandType"), CommandType.class);
        Integer gameID = jsonObject.get("gameID").getAsInt();
        String authToken = jsonObject.get("authToken").getAsString();

        switch  (commandType) {
            case CONNECT:
                return new ConnectCommand(authToken, gameID);
            case  MAKE_MOVE:
                ChessMove chessMove = gson.fromJson(jsonObject.get("chessMove"), ChessMove.class);
                return new MakeMoveCommand(authToken, gameID, chessMove);
            case  LEAVE:
                return new LeaveCommand(authToken, gameID);
            case  RESIGN:
                return new ResignCommand(authToken, gameID);
            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserGameCommand that)) {
            return false;
        }
        return getCommandType() == that.getCommandType() &&
                Objects.equals(getAuthToken(), that.getAuthToken()) &&
                Objects.equals(getGameID(), that.getGameID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCommandType(), getAuthToken(), getGameID());
    }
}
