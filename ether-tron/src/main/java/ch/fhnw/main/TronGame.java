package ch.fhnw.main;

import java.awt.Button;

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
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class TronGame {

    public static void main(String[] args) {
        new TronGame();

    }
    
    public TronGame() {
        Platform.get().init();
        
        // Create controller
        IController controller = new DefaultController();
        GameWorld gameWorld = new GameWorld(controller);
        
        ITool evenntHandler = new EventHandler(controller, new NavigationTool(controller));
        
        controller.run(time -> {
            // Create view
            final IView view = new DefaultView(controller, 150, 20, 800, 600, IView.INTERACTIVE_VIEW, "Tron Game");       
    
            // Create scene
            IScene scene = new DefaultScene(controller);
            controller.setScene(scene);
            controller.setTool(evenntHandler);
            gameWorld.createWorld(scene, view);

        });
        
        Platform.get().run();
    }

}
