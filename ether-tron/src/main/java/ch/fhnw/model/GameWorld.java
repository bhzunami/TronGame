package ch.fhnw.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IEventScheduler;
import ch.fhnw.ether.formats.IModelReader.Options;
import ch.fhnw.ether.formats.obj.ObjReader;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.view.IView;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class GameWorld {

    // The player which plays on this view
    private Player mplayer;
    private List<Player> players = new ArrayList<>();
    private List<IMesh> ramps = new ArrayList<>();
    private List<IMesh> blocks = new ArrayList<>();
    private List<IMesh> powerUps = new ArrayList<>();
    private IController controller;
    DatagramSocket udpSocket;

    private byte[] buffer = new byte[1024];
    private DatagramPacket packet = new DatagramPacket( buffer, buffer.length) ;

    public static final int IDLE = 0;
    public static final int USER_INPUT = 1;
    public static int STATE;
    private String movement = null;
    float c = 0;

    public GameWorld(IController controller, Player p, DatagramSocket udpSocket) {
        this.controller = controller;
        this.mplayer = p;
        this.udpSocket = udpSocket;
    }


    public void playerMoved(Vec3 pos) {
        ICamera camera = this.controller.getScene().getCameras().get(0);
        camera.setPosition(pos);
    }

    public void createWorld(IScene scene, IView view) throws IOException {
        System.out.println("Create Game world");

        // Add player
        for(Player p : this.players) {
            final URL obj = getClass().getResource("/models/cube.obj");
            final List<IMesh> meshes = new ArrayList<>();
            new ObjReader(obj, Options.CONVERT_TO_Z_UP).getMeshes().forEach(mesh -> meshes.add(mesh));
            final List<IMesh> merged = MeshUtilities.mergeMeshes(meshes);
            //p.setMesh(merged.get(0));
            this.controller.getScene().add3DObject(p.getMesh());
        }

        // Create and add camera to the main player
        Vec3 pos = this.mplayer.getPosition();
        ICamera camera = new Camera(new Vec3(pos.x, pos.y -3, pos.z + 2), Vec3.ZERO);
        controller.getScene().add3DObject(camera);
        controller.setCamera(view, camera);
    }

    public void run() {

        //while( true ) {
        controller.animate(new IEventScheduler.IAnimationAction() {
            @Override
            public void run(double time, double interval) {
                // Read server input
                // Update players
                // render World
                try {
                    String data = getUDPData();
                } catch (SocketTimeoutException e) {
                    System.out.println();
                } catch(Exception ex) {
                    System.out.println("ERROR");
                }
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
        Mat4 transfrom = null;
        Vec3 ctarget = null;
        Vec3 target = null;
        switch(movement) {

        case "FORWARD":
            target = new Vec3(pos.x, pos.y+c, pos.z);
            ctarget = new Vec3(cam_pos.x, cam_pos.y +c , cam_pos.z);
            transfrom = Mat4.translate(target);
            players.get(0).setPosition(target);
            cam.setPosition(ctarget);
            cam.setTarget(target);
            //controller.setCamera(controller.getCurrentView(), cam);
            //controller.getCamera(controller.getCurrentView()).setPosition(ctarget);
            break;
        case "BACKWARD":
            target = new Vec3(pos.x, pos.y-c, pos.z);
            transfrom = Mat4.translate(target);
            players.get(0).setPosition(new Vec3(pos.x, pos.y-c, pos.z));
            ctarget = new Vec3(cam_pos.x, cam_pos.y - c , cam_pos.z);
            cam.setPosition(ctarget);
            cam.setTarget(target);
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

//        players.get(0).getMesh().setTransform(transfrom);
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


    private String getUDPData() throws IOException {
        this.udpSocket.receive(this.packet);
        this.packet.getData();
        byte[] data = this.packet.getData();
        pos1 := ByteBuffer.wrap(bytes, 0,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        pos2 := ByteBuffer.wrap(bytes, 4,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        pos3 := ByteBuffer.wrap(bytes, 8,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        dir1 := ByteBuffer.wrap(bytes,12,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        dir2 := ByteBuffer.wrap(bytes,16,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        dir3 := ByteBuffer.wrap(bytes,20,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        // log dirs and poss
        return this.packet.getData().toString();

    }



}
