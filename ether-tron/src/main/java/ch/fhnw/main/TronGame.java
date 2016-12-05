package ch.fhnw.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        
        DatagramSocket udpSocket = new DatagramSocket();
        udpSocket.setSoTimeout(100);
        Player p = new Player(args[0]);
        
        String serverAddress = args[1];
       
        p.setPosition(new Vec3(0,0,0));
        new TronGame(p, udpSocket, serverAddress);

    }

    public TronGame(Player player, DatagramSocket socket, String serverAddress) {
        Platform.get().init();

        // Create controller
        IController controller = new DefaultController();
        GameWorld gameWorld = new GameWorld(controller, player, socket);

        ITool evenntHandler = new EventHandler(controller, new NavigationTool(controller), gameWorld);

        controller.run(time -> {
            // Create view
            final IView view = new DefaultView(controller, 150, 20, 800, 600, IView.INTERACTIVE_VIEW, "Tron Game");

            // Create scene
            IScene scene = new DefaultScene(controller);
            controller.setScene(scene);
            controller.setTool(evenntHandler);
            
            // Set game world objects
            gameWorld.addPlayer(player);
            try {
                gameWorld.createWorld(scene, view);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            gameWorld.run();

        });
        
        try {
            // Update ID
            player.setId(sendPlayer(player, socket, serverAddress));
        } catch(JsonProcessingException ex) {
            System.out.println("Could not read ansewr from server");
        } catch(IOException ex) {
            System.out.println("Could not connect to server");
        }
        
        // Start main game loop
        Platform.get().run();
    }
    
    @SuppressWarnings("unchecked")
    private String sendPlayer(Player player, DatagramSocket socket, String serverAddress) throws JsonProcessingException, IOException {
        // TCP Socket connection
        Socket clientSocket = new Socket(serverAddress, 7606);
        OutputStream out = clientSocket.getOutputStream();
        InputStream in = clientSocket.getInputStream();
        
        // Prepare data to send
        HashMap<String, Object> playerMap = new HashMap<>();
        playerMap.put("name", player.getName());
        playerMap.put("port", socket.getLocalPort());
        playerMap.put("host", socket.getLocalAddress().getHostAddress());
        
        HashMap<String, Object> joinRequest = new HashMap<>();
        joinRequest.put("action", "join");
        joinRequest.put("payload", playerMap);
        
        // Write player data to Server as TCP
        out.write(mapper.writeValueAsString(joinRequest).getBytes());
        out.flush();
        
        // Get player id
        HashMap<String, Object> response = new HashMap<>();
        response = mapper.readValue(in, response.getClass());
        
        // Check if error
        if(response.get("error") != null) {
            System.out.println("Error");
        }
        
        String id = ((HashMap<String, String>)response.get("response")).get("id");
        System.out.println("Player: " +id);
        clientSocket.close();
        return id;
    }

}
