package ch.fhnw.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

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
        
        Player p = new Player(args[0]);
        sendPlayer(p, 7606);
        
        p.setPosition(new Vec3(0,0,0));
        
        // Connect to server
//        Socket tcpSocket = new Socket(args[1], 6789);
//        DataOutputStream outToServer = new DataOutputStream(tcpSocket.getOutputStream());
//        String m = "New player: "+p.getId();
//        outToServer.writeBytes(m + '\n');
//        tcpSocket.close();

        
        // players.add(p2);
//        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
//        DatagramSocket udpSocekt = new DatagramSocket();
//        InetAddress IPAddress = InetAddress.getByName("localhost");
//        byte[] sendData = new byte[1024];
//        String sentence = " ";
//        while(!sentence.isEmpty()) {
//            sentence = inFromUser.readLine();
//            sendData = sentence.getBytes();
//            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 7604);
//            udpSocekt.send(sendPacket);
//        }
//     
//        udpSocekt.close();

        new TronGame(p);

    }

    public TronGame(Player player) {
        Platform.get().init();

        // Create controller
        IController controller = new DefaultController();
        GameWorld gameWorld = new GameWorld(controller, player);

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
            gameWorld.createWorld(scene, view);
            gameWorld.run();

        });

        Platform.get().run();
        
    }
    
    
    private static void sendPlayer(Player player, int port) throws JsonProcessingException, IOException {
        Socket clientSocket = new Socket("localhost", port);
        OutputStream out = clientSocket.getOutputStream();
        InputStream in = clientSocket.getInputStream();
        out.write(mapper.writeValueAsString(player).getBytes());
        out.flush();
        player = mapper.readValue(in, Player.class);
        System.out.println("Player: " + player.getId() +" name: " +player.getName());
        clientSocket.close();
    }

}
