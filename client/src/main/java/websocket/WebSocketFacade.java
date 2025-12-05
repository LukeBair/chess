package websocket;

import com.google.gson.Gson;

import jakarta.websocket.*;
import ui.Renderer;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketFacade extends Endpoint {
    Session session;
    private final Renderer renderer;

    public WebSocketFacade(Renderer renderer) {
        this.renderer = renderer;

        try {
            URI socketURI = new URI("ws://localhost:8080/ws");

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, socketURI);

            //set message handler
            this.session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    ServerMessage serverMessage = new Gson().fromJson(message, ServerMessage.class);

                    if (serverMessage.getServerMessageType() == ServerMessage.ServerMessageType.NOTIFICATION) {
                        NotificationMessage notif = new Gson().fromJson(message, NotificationMessage.class);
                        renderer.enqueueRenderTask(notif.getMessage());
                    } else if (serverMessage.getServerMessageType() == ServerMessage.ServerMessageType.ERROR) {
                        ErrorMessage error = new Gson().fromJson(message, ErrorMessage.class);
                        renderer.enqueueRenderTask("ERROR: " + error.getErrorMessage());
                    } else if (serverMessage.getServerMessageType() == ServerMessage.ServerMessageType.LOAD_GAME) {
                        LoadGameMessage loadGame = new Gson().fromJson(message, LoadGameMessage.class);
                        renderer.enqueueRenderTask(loadGame.getMessage());
                    }
                }
            });

        } catch (DeploymentException | IOException | URISyntaxException ex) {
            //TODO: issues
            System.out.println(ex);
        }
    }

    //Endpoint requires this method, but you don't have to do anything
    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) { }

    public void loadGame(String visitorName, String role, int gameID) {
        try {
            var action = new LoadGameMessage(visitorName, role, gameID);
            this.session.getBasicRemote().sendText(new Gson().toJson(action));
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    public void leavePetShop(String visitorName) {
//        try {
////            var action = new Action(Action.Type.EXIT, visitorName);
////            this.session.getBasicRemote().sendText(new Gson().toJson(action));
//        } catch (IOException ex) {
//            System.out.println(ex);
//            // TODO: fix
//        }
    }

}
