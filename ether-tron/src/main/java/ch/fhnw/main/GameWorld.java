package ch.fhnw.main;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.view.IView;
import ch.fhnw.util.math.Vec3;

public class GameWorld {
    
    private IMesh player;
    private IController controller;
    
    public GameWorld(IController controller) {
        this.controller = controller;
        this.buildWorld();  
    }
    
    public void createWorld(IScene scene, IView view) {
        System.out.println("Create world");

        // Create and add camera
//        ICamera camera = new Camera(new Vec3(0, -5, 5), Vec3.ZERO);
//        controller.getScene().add3DObject(camera);
//        controller.setCamera(view, camera);
        
        // Add player
//        this.controller.getScene().add3DObject(this.player);
        
    }
    
    
    private void buildWorld() {
        this.player = MeshUtilities.createCube();
        this.player.setPosition(new Vec3(0, 0, 0));
        
                
    }
    
    
    

}
