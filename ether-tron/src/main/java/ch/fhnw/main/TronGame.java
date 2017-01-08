package ch.fhnw.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
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
import ch.fhnw.util.math.Vec3;

public class TronGame {
    
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        
        if(args.length < 2 ) {
            System.out.println("Please specify a player name and the server address");
            return;
        }
        
        Player p = new Player(args[0]);
        String serverAddress = args[1];
        
        p.setPosition(new Vec3(0,0,0));
        new TronGame(p, serverAddress);

    }

    public TronGame(Player player, String serverAddress) throws IOException {
        Platform.get().init();
        
        DatagramSocket udpSocket = new DatagramSocket();
        udpSocket.setSoTimeout(5);
        // Create controller
        IController controller = new DefaultController();
        GameWorld gameWorld = new GameWorld(controller, player, udpSocket, serverAddress);

        ITool evenntHandler = new EventHandler(controller, new NavigationTool(controller), gameWorld);

        controller.run(time -> {
            // Create view
            final IView view = new DefaultView(controller, 150, 20, 800, 600, IView.INTERACTIVE_VIEW, "Tron Game");

            // Create scene
            IScene scene = new DefaultScene(controller);
            controller.setScene(scene);
            controller.setTool(evenntHandler);
            
            // Set game world objects
            try {
                JoinResponseDao gameData = sendPlayer(player, udpSocket, serverAddress).getResponse();
                // Update ID
                player.setId(gameData.getId());
                gameWorld.addPlayer(player, true);
                gameWorld.addBlocks(gameData.getBlocks());
                gameWorld.addPowerUps(gameData.getPowerUps());
            } catch(JsonProcessingException ex) {
                System.out.println("Could not read ansewr from server");
            } catch(IOException ex) {
                System.out.println("Could not connect to server: " +ex.getMessage());
            }
            
            
            try {
                gameWorld.createWorld(scene, view);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            gameWorld.run();

        });
        
        
        // Start main game loop
        Platform.get().run();
    }
    
    @SuppressWarnings("unchecked")
    private GeneralResponse<JoinResponseDao> sendPlayer(Player player, DatagramSocket udpSocket, String serverAddress) throws JsonProcessingException, IOException {
        Socket tcpSocket = new Socket(serverAddress, 7606);
        OutputStream out = tcpSocket.getOutputStream();
        InputStream in = tcpSocket.getInputStream();
        // Prepare data to send
        HashMap<String, Object> playerMap = new HashMap<>();
        playerMap.put("name", player.getName());
        playerMap.put("port", udpSocket.getLocalPort());
        
        HashMap<String, Object> joinRequest = new HashMap<>();
        joinRequest.put("action", "join");
        joinRequest.put("payload", playerMap);
        
        // Write player data to Server as TCP
        out.write(mapper.writeValueAsString(joinRequest).getBytes());
        out.flush();
        // Get player id
        GeneralResponse<JoinResponseDao> response = new GeneralResponse<>();
        response = mapper.readValue(in, new TypeReference<GeneralResponse<JoinResponseDao>>() {});
        
        // Check if error
        if(response.isError()) {
            throw new RuntimeException("Response Error");
        }

        tcpSocket.close();
        return response;
    }

}
