package ch.fhnw.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.tool.ITool;
import ch.fhnw.ether.controller.tool.NavigationTool;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.view.DefaultView;
import ch.fhnw.ether.view.IView;
import ch.fhnw.helper.GeneralResponse;
import ch.fhnw.helper.JoinResponseDao;
import ch.fhnw.main.events.EventHandler;
import ch.fhnw.model.GameWorld;
import ch.fhnw.model.Player;

public class TronGame {

    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("Please specify the server address");
            return;
        }

        new TronGame(args[0]);
    }

    public TronGame(String serverAddress) throws IOException {
        Platform.get().init();

        DatagramSocket udpSocket = new DatagramSocket();
        udpSocket.setSoTimeout(5);
        // Create controller
        IController controller = new DefaultController();

        Player player = new Player(null, true);

        GameWorld gameWorld = new GameWorld(controller, player, udpSocket, serverAddress);

        ITool eventHandler = new EventHandler(controller, new NavigationTool(controller), gameWorld);

        controller.run(time -> {
            // Create view
            final IView view = new DefaultView(controller, 150, 20, 800, 600, IView.INTERACTIVE_VIEW, "Tron Game");

            // Create scene
            IScene scene = new DefaultScene(controller);
            controller.setScene(scene);
            controller.setTool(eventHandler);

            // Set game world objects
            try {
                JoinResponseDao gameData = sendPlayer(udpSocket.getLocalPort(), serverAddress).getResponse();
                gameWorld.createWorld(view, gameData);
            } catch (JsonProcessingException ex) {
                System.out.println("Could not read answer from server");
            } catch (IOException ex) {
                System.out.println("Could not connect to server: " + ex.getMessage());
            }

            gameWorld.run();

        });

        // Start main game loop
        Platform.get().run();
    }

    private GeneralResponse<JoinResponseDao> sendPlayer(int udpLocalPort, String serverAddress)
            throws JsonProcessingException, IOException {
        Socket tcpSocket = new Socket(serverAddress, 7606);
        OutputStream out = tcpSocket.getOutputStream();
        InputStream in = tcpSocket.getInputStream();
        // Prepare data to send
        HashMap<String, Object> playerMap = new HashMap<>();
        playerMap.put("name", "");
        playerMap.put("port", udpLocalPort);

        HashMap<String, Object> joinRequest = new HashMap<>();
        joinRequest.put("action", "join");
        joinRequest.put("payload", playerMap);

        // Write player data to Server as TCP
        out.write(mapper.writeValueAsString(joinRequest).getBytes());
        out.flush();
        // Get player id
        GeneralResponse<JoinResponseDao> response = mapper.readValue(in,
                new TypeReference<GeneralResponse<JoinResponseDao>>() {
                });

        tcpSocket.close();

        // Check if error
        if (response.isError()) {
            throw new RuntimeException("Response Error");
        }

        return response;
    }

}
