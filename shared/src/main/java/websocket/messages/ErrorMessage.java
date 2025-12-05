package websocket.messages;

public class ErrorMessage extends ServerMessage {
    private final int errorCode;
    private final String errorMessage;
    public ErrorMessage(int  errorCode, String errorMessage) {
        super(ServerMessageType.ERROR);

        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
}
