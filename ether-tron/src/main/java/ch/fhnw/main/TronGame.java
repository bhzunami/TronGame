package ch.fhnw.main;

import java.awt.Button;
import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.tool.ITool;
import ch.fhnw.ether.controller.tool.NavigationTool;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.view.DefaultView;
import ch.fhnw.ether.view.IView;
import ch.fhnw.model.GameWorld;
import ch.fhnw.model.Player;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class TronGame {

    public static void main(String[] args) {
        Player p = new Player("Nicolas", new Vec3(0,0,0), true);
        Player p2 = new Player("Thomas", new Vec3(0,0,0), false);
        
        List<Player> players = new ArrayList<>();
        players.add(p);
        //players.add(p2);
        new TronGame(players);

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
            
            
            for(Player p : players)
                gameWorld.addPlayer(p);
            
            gameWorld.createWorld(scene, view);
            
            
            gameWorld.run();

        });
        
        Platform.get().run();
    }

}
