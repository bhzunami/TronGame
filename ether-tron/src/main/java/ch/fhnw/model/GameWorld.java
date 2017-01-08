package ch.fhnw.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.tritonus.share.ArraySet;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IEventScheduler;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.light.DirectionalLight;
import ch.fhnw.ether.scene.light.ILight;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.ether.view.IView;
import ch.fhnw.main.events.UserInput;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.GeodesicSphere;

public class GameWorld {

    // The player which plays on this view
    private Player mplayer;
    private HashMap<String, Player> players = new HashMap<>();
    private ICamera cam;
    private List<Block> blocks = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();
    private Point[] trace = new Point[600];
    
    private int traceIterator = 0;
    private IController controller;
    private static final RGB AMBIENT = RGB.BLACK;
    private static final RGB COLOR = RGB.YELLOW;
    private ILight light = new DirectionalLight(Vec3.Z, AMBIENT, COLOR);
    private IMesh lightMesh;
    private IMesh ground = MeshUtilities.createGroundPlane(1000);

    private final int offset = 40;

    DatagramSocket udpSocket;
    private double time = 0f;

    private InetAddress server;

    private byte[] buffer = new byte[1024];
    private DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    public static final int IDLE = 0;
    public static final int USER_INPUT = 1;
    public static final int READY = 2;
    public double old_time;

    private static ObjectMapper mapper = new ObjectMapper();

    public static int STATE = -1;
    // 0 = Forward
    // 1 = Right
    // 2 = Backward
    // 3 = Left
    // 4 = space
    private Set<UserInput> userInputs = new HashSet<UserInput>();

    public GameWorld(IController controller, Player p, DatagramSocket socket, String server)
            throws IOException {
        this.controller = controller;
        this.mplayer = p;
        this.udpSocket = socket;
        this.udpSocket.setReceiveBufferSize(1);
        this.server = InetAddress.getByName(server);

    }

    // public void playerMoved(Vec3 pos) {
    // ICamera camera = this.controller.getScene().getCameras().get(0);
    // camera.setPosition(pos);
    // }

    public void createWorld(IScene scene, IView view) throws IOException {
        System.out.println("Create Game world");

        cam = new Camera();
        scene.add3DObject(cam);
        controller.setCamera(view, cam);

        // Ground
        scene.add3DObject(ground);

        // Lights
        // Add first light and light geometry
        GeodesicSphere s = new GeodesicSphere(4);
        lightMesh = new DefaultMesh(Primitive.TRIANGLES, new ColorMaterial(RGBA.YELLOW),
                DefaultGeometry.createV(s.getTriangles()), Flag.DONT_CAST_SHADOW);
        lightMesh.setTransform(Mat4.trs(0, 0, 0, 0, 0, 0, 0.1f, 0.1f, 0.1f));
        lightMesh.setPosition(new Vec3(0, 0, 2));
        light.setPosition(lightMesh.getPosition());
        scene.add3DObjects(light);
        scene.add3DObjects(lightMesh);

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
                    getUDPData(time);
                } catch (SocketTimeoutException e) {
                    // Ignore timeouts
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                controller.viewChanged(controller.getCurrentView());

            }
        });
    }
    

    public void addPlayer(Player p, boolean main) {
        this.players.put(p.getId(), p);
        
        for(Point point : p.getTrace().getPoints()) {
            this.controller.getScene().add3DObject(point.getMesh());
        }
        

        if (main) {
            this.mplayer = p;
            this.controller.getScene().add3DObjects(mplayer.getMesh());
        }
    }

    public void setMovemnet(int movement, boolean pressed) {
        UserInput mov = UserInput.getEnumByKey(movement);
        // Remove
        if (pressed) {
            this.userInputs.add(mov);
        } else {
            this.userInputs.remove(mov);
        }
    }

    private void sendUDPData() throws IOException {
        HashMap<String, Object> data = new HashMap<>();
        data.put("uuid", mplayer.getId());
        data.put("keys", UserInput.toByte(userInputs));

        byte[] buf = mapper.writeValueAsString(data).getBytes();
        DatagramPacket p = new DatagramPacket(buf, buf.length, this.server, 7607);
        this.udpSocket.send(p);
    }

    private void updateCamera(Vec3 position, Vec3 direction, float rot) {
        // Vec3 camara_position = new Vec3(playerPosition.x, playerPosition.y - 6, playerPosition.z
        // + 2);
        Vec3 pos;
        if(rot > 0) {
            pos = new Vec3(position.x+Math.abs(rot)*2, position.y, position.z);
        } else if(rot < 0) {
            pos = new Vec3(position.x+Math.abs(rot)*2, position.y, position.z);
        } else {
            pos = new Vec3(position.x, position.y, position.z);
        }
        
//        pos = pos.add(new Vec3(0, 0, 5));
        
        cam.setPosition(pos);
        cam.setTarget(direction);

    }
    
    private void removePlayer(Player p) {
        for(Point point : p.getTrace().getPoints()) {
            this.controller.getScene().remove3DObject(point.getMesh());
        }
    }

    private void getUDPData(double time) throws IOException {
        this.udpSocket.receive(this.packet);
        this.packet.getData();
        byte[] data = this.packet.getData();

        // 1 byte = num players
        // 16 Bytes = uuid
        // 12 Bytes position
        // 12 Bytes rotation
        byte num_players = ByteBuffer.wrap(data, 0, 1).order(ByteOrder.LITTLE_ENDIAN).get();
        Set<String> activePlayers = new ArraySet<>();

        for (int i = 0; i < num_players; i++) {
            int index = offset * i;
            byte[] uuid = Arrays.copyOfRange(data, index + 1, index + 17);
            ByteBuffer byteBuffer = ByteBuffer.wrap(uuid);
            Long high = byteBuffer.getLong();
            Long low = byteBuffer.getLong();

            Vec3 player_pos = new Vec3(ByteBuffer.wrap(data, index + 17, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                    ByteBuffer.wrap(data, index + 21, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                    ByteBuffer.wrap(data, index + 25, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat());

            Vec3 rotation = new Vec3(ByteBuffer.wrap(data, index + 29, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                    ByteBuffer.wrap(data, index + 33, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                    ByteBuffer.wrap(data, index + 37, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat());

            String puuid = new UUID(high, low).toString();
            Player p = players.get(puuid);

            if (p == null) {
                System.out.println("NEW PLAYER");
                // new player in game
                p = new Player("test");
                p.setId(puuid);
                this.addPlayer(p, false);
                this.controller.getScene().add3DObjects(p.getMesh());
            }
            
            activePlayers.add(p.getId());

            // Calculate rotation of player
            p.setRotAngle(MathUtilities.RADIANS_TO_DEGREES * (rotation.z - p.getDirection().z));
            Mat4 rot_z = Mat4.rotate(p.getRotAngle(), Vec3.Z);
            Mat4 rot_x = Mat4.rotate(MathUtilities.RADIANS_TO_DEGREES * rotation.x, Vec3.X);

            p.updatePosition((player_pos));
            p.rotate(Mat4.multiply(rot_z, rot_x));
            
            p.getTrace().notify(p.getPositionCopy());
//            this.trace[this.traceIterator].setPosition(p.getPositionCopy());
//            this.traceIterator++;
//            if(this.traceIterator >= this.trace.length) {
//                this.traceIterator = 0;
//            }

            
            if (p == mplayer) {
                Vec3 ppos = mplayer.getPosition();
                updateCamera(ppos.subtract(rot_z.transform(new Vec3(25, 0, -5))), ppos, rotation.x);
            }

            p.setDirection(rotation);

        }
        
        // Check if player is deleted
        
        if(this.players.size() > activePlayers.size()) {
            // Remove a player
            for(String id : this.players.keySet()) {
                if(!activePlayers.contains(id))
                    this.removePlayer(this.players.get(id));
            }
        }

    }
    
    

    public void addBlocks(float[][] blocks) throws IOException {
        for(int i = 0; i < blocks.length; i++) {
            Block b = new Block(blocks[i][0], blocks[i][1]);
            this.blocks.add(b);
            this.controller.getScene().add3DObject(b.getMesh());
        }        
    }

    public void addPowerUps(float[][] powerUps) {
        for(int i = 0; i < powerUps.length; i++) {
            PowerUp pu = new PowerUp(powerUps[i][0], powerUps[i][1]);
            this.powerUps.add(pu);
            this.controller.getScene().add3DObject(pu.getMesh());
        }
        
    }

}
