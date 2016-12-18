package ch.fhnw.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import ch.fhnw.main.events.UserInput;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class GameWorld {

    // The player which plays on this view
    private Player mplayer;
    private List<Player> players = new ArrayList<>();
    private ICamera cam;
    private List<IMesh> ramps = new ArrayList<>();
    private List<IMesh> blocks = new ArrayList<>();
    private List<IMesh> powerUps = new ArrayList<>();
    private IController controller;
    DatagramSocket udpSocket;
    private float angle = 0;
    private InetAddress server;

    private byte[] buffer = new byte[1024];
    private DatagramPacket packet = new DatagramPacket( buffer, buffer.length) ;

    public static final int IDLE = 0;
    public static final int USER_INPUT = 1;
    public static final int READY = 2;
   
    private static ObjectMapper mapper = new ObjectMapper();


    public static int STATE = -1;
    // 0 = Forward
    // 1 = Right
    // 2 = Backward
    // 3 = Left
    // 4 = space
    private Set<UserInput> userInputs = new HashSet<UserInput>();
    

    public GameWorld(IController controller, Player p, DatagramSocket socket, String server) throws UnknownHostException, SocketException {
        this.controller = controller;
        this.mplayer = p;
        this.udpSocket =  socket;
        this.server = InetAddress.getByName(server);
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
//            p.setMesh(new ObjReader(obj, Options.CONVERT_TO_Z_UP).getMeshes().get(0));
            this.controller.getScene().add3DObject(p.getMesh());
        }

        // Create and add camera to the main player
        Vec3 pos = this.mplayer.getPosition();
//        ICamera camera = 
        cam = new Camera(new Vec3(pos.x, pos.y -6, pos.z + 2), Vec3.ZERO);
        controller.getScene().add3DObject(cam);
        controller.setCamera(view, cam);
    }

    /**
     * Main gain loop
     */
    public void run() {
        controller.animate(new IEventScheduler.IAnimationAction() {
            @Override
            public void run(double time, double interval) {
                // Read server input
                try {
                    sendUDPData();
                    getUDPData();
                } catch (SocketTimeoutException e) {
                    // Ignore timeouts
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
                // Update game world
                System.out.println("Update world");
                controller.viewChanged(controller.getCurrentView());
            }
        });
    }

    public void addPlayer(Player p) {
        this.players.add(p);
    }

    
    public void setMovemnet(int movement, boolean pressed) {
        UserInput mov = UserInput.getEnumByKey(movement);
        // Remove
        if( pressed ) {
            this.userInputs.add(mov);
        } else {
            this.userInputs.remove(mov);
        }
        
        switch(mov) {
        case MOVE_FORWARD:
            break;
        case MOVE_RIGHT:
            if(angle + 2f > 35) break;
            angle += 2f;
            players.get(0).getMesh().setTransform(Mat4.multiply(Mat4.rotate(angle, Vec3.Y), Mat4.translate(0, 0, 0)));
            break;
        case MOVE_BACKWARD:
            break;
        case MOVE_LEFT:
            if(angle - 2f < -35) break;
            angle -= 2f;
            players.get(0).getMesh().setTransform(Mat4.rotate(angle, Vec3.Y));
            break;
        }
    }
    
    
    private void sendUDPData() throws IOException {
        HashMap<String, Object> data = new HashMap<>();
        data.put("uuid", mplayer.getId());
        data.put("keys", UserInput.toByte(userInputs));
        
        byte[] buf = mapper.writeValueAsString(data).getBytes();
        DatagramPacket p = new DatagramPacket(buf, buf.length, this.server, 7607) ;
        this.udpSocket.send(p);
    }

    
    private void updateCamera(Vec3 playerPosition) {
        Vec3 camara_position = new Vec3(playerPosition.x, playerPosition.y -6, playerPosition.z +2);
        cam.setPosition(camara_position);
        cam.setTarget(playerPosition);
        
    }
    private void getUDPData() throws IOException {
        System.out.println("GET UDP");
        this.udpSocket.receive(this.packet);
        this.packet.getData();
        byte[] data = this.packet.getData();
        float posx = ByteBuffer.wrap(data, 0,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        float posy = ByteBuffer.wrap(data, 4,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        float posz = ByteBuffer.wrap(data, 8,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        float dirx = ByteBuffer.wrap(data,12,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        float diry = ByteBuffer.wrap(data,16,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        float dirz = ByteBuffer.wrap(data,20,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();

        Vec3 ppos = new Vec3(posx, posy, posz);
        System.out.println(ppos.toString());
        players.get(0).setPosition(ppos);
        updateCamera(players.get(0).getPosition());
        System.out.println("Fully processed");

    }



}
