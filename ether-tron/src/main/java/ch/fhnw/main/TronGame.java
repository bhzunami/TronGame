package ch.fhnw.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.tool.ITool;
import ch.fhnw.ether.controller.tool.NavigationTool;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.view.DefaultView;
import ch.fhnw.ether.view.IView;
import ch.fhnw.model.GameWorld;
import ch.fhnw.model.Player;
import ch.fhnw.util.math.Vec3;

public class TronGame {

    public static void main(String[] args) throws IOException {
        Player p = new Player("Nicolas", new Vec3(0, 0, 0), true);
        Player p2 = new Player("Thomas", new Vec3(0, 0, 0), false);

        Float a = 0.4f;
        byte[] send = ByteBuffer.allocate(4).putFloat(a).array();
        List<Player> players = new ArrayList<>();
        players.add(p);
        // players.add(p2);
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("localhost");
        byte[] sendData = new byte[1024];
        String sentence = " ";
        while(!sentence.isEmpty()) {
            sentence = inFromUser.readLine();
            sendData = sentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 7604);
            clientSocket.send(sendPacket);
        }
     
        clientSocket.close();

        // new TronGame(players);

    }

    public TronGame(List<Player> players) {
        Platform.get().init();

        // Create controller
        IController controller = new DefaultController();
        GameWorld gameWorld = new GameWorld(controller);

        ITool evenntHandler = new EventHandler(controller, new NavigationTool(controller), gameWorld);

        controller.run(time -> {
            // Create view
            final IView view = new DefaultView(controller, 150, 20, 800, 600, IView.INTERACTIVE_VIEW, "Tron Game");

            // Create scene
            IScene scene = new DefaultScene(controller);
            controller.setScene(scene);
            controller.setTool(evenntHandler);

            for (Player p : players)
                gameWorld.addPlayer(p);

            gameWorld.createWorld(scene, view);

            gameWorld.run();

        });

        Platform.get().run();
    }

}
