package ch.fhnw.model;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IEventScheduler;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.view.IView;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class GameWorld {
    
    private List<Player> players = new ArrayList<>();
    private List<IMesh> ramps = new ArrayList<>();
    private List<IMesh> blocks = new ArrayList<>();
    private List<IMesh> powerUps = new ArrayList<>();
    private IController controller;
    
    public static final int IDLE = 0;
    public static final int USER_INPUT = 1;
    public static int STATE;
    private String movement = null;
    float c = 0;
    
    private double animationStart = 0;
    
    
    public GameWorld(IController controller) {
        this.controller = controller;
    }
    
    
    public void playerMoved(Vec3 pos) {
        ICamera camera = this.controller.getScene().getCameras().get(0);
        camera.setPosition(pos);
    }
    
    public void createWorld(IScene scene, IView view) {
        System.out.println("Create Game world");
        Player player = null;
        
        // Add player
        for(Player p : this.players) {
            this.controller.getScene().add3DObject(p.getMesh());
            if(p.isShow())
                player = p;
        }
        
        // Create and add camera
        Vec3 pos = player.getPosition();
        ICamera camera = new Camera(new Vec3(pos.x, pos.y -3, pos.z + 2), Vec3.ZERO);
        controller.getScene().add3DObject(camera);
        controller.setCamera(view, camera);
    }
    
    public void run() {
       
        controller.animate(new IEventScheduler.IAnimationAction() {
            @Override
            public void run(double time, double interval) {
                // Read server input
                // Update players
                // render World
                switch(GameWorld.STATE) {
                case GameWorld.IDLE:
                    c = 0;
                    break;
                    
                case GameWorld.USER_INPUT:
                    handleUserInput(time);
                    controller.viewChanged(controller.getCurrentView());
                    break;
                default:
                        break;
                }
                
            }
        });
    }
    
    
    private void handleUserInput(double time) {
        
        c += 0.05;
        Vec3 pos = players.get(0).getMesh().getPosition();
        ICamera cam = this.controller.getCamera(controller.getCurrentView());
        Vec3 cam_pos = this.controller.getCamera(controller.getCurrentView()).getPosition();
        System.out.println("Start from " +pos.x +pos.y);
        Mat4 transfrom = null;
        switch(movement) {
        case "FORWARD":
            Vec3 target = new Vec3(pos.x, pos.y+c, pos.z);
            Vec3 ctarget = new Vec3(cam_pos.x, cam_pos.y, cam_pos.z);
            transfrom = Mat4.translate(target);
            players.get(0).setPosition(target);
            //cam.setPosition(ctarget);
//            cam.setTarget(ctarget);
            //controller.setCamera(controller.getCurrentView(), cam);
            //controller.getCamera(controller.getCurrentView()).setPosition(ctarget);
            break;
        case "BACKWARD":
            transfrom = Mat4.translate(pos.x, pos.y-c, pos.z);
            players.get(0).setPosition(new Vec3(pos.x, pos.y-c, pos.z));
            break;
        case "LEFT":
            transfrom = Mat4.translate(pos.x-(c/2), pos.y, pos.z);
            players.get(0).setPosition(new Vec3(pos.x-(c/2), pos.y, pos.z));
            break;
        case "RIGHT":
            transfrom = Mat4.translate(pos.x+(c/2), pos.y, pos.z);
            players.get(0).setPosition(new Vec3(pos.x+(c/2), pos.y, pos.z));
            break;
        default:
            System.out.println("NO WAY");
                
        }
       
        players.get(0).getMesh().setTransform(transfrom);
    }
    
    
    public void addPlayer(Player p) {
        this.players.add(p);
    }
    
    public void moveForward() {
        this.movement = "FORWARD";
    }
    
    public void moveBackward() {
        this.movement = "BACKWARD";
    }
    
    public void moveLeft() {
        this.movement = "LEFT";
    }
    
    public void moveRight() {
        this.movement = "RIGHT";
    }
    
    

}
