package websocket;

import chess.ChessMove;
import chess.ChessPosition;
import org.junit.jupiter.api.Test;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import javax.swing.text.Position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebSocketTests {
    @Test
    public void testMakeMoveSerialization() {
        ChessMove move = new ChessMove(new ChessPosition(6,4), new ChessPosition(4,4), null);
        MakeMoveCommand cmd = new MakeMoveCommand("token", 123, move);
        String json = cmd.toJson();
        assertTrue(json.contains("\"commandType\":\"MAKE_MOVE\""));
        assertTrue(json.contains("\"row\":6"));
        UserGameCommand deserialized = UserGameCommand.fromJson(json);
        assertEquals(UserGameCommand.CommandType.MAKE_MOVE, deserialized.getCommandType());
        assertEquals(6, ((MakeMoveCommand) deserialized).getChessMove().getStartPosition().getRow());
    }
}
