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
import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.light.DirectionalLight;
import ch.fhnw.ether.scene.light.ILight;
import ch.fhnw.ether.scene.light.PointLight;
import ch.fhnw.ether.scene.light.SpotLight;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMapMaterial;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.ether.scene.mesh.material.ShadedMaterial;
import ch.fhnw.ether.view.IView;
import ch.fhnw.main.events.UserInput;
import ch.fhnw.model.PowerUpShader.PowerUpMaterial;
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
//    private ICamera cam;
    private List<Block> blocks = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();
    private Point[] trace = new Point[600];
    
    private int traceIterator = 0;
    private IController controller;
    public static final RGB AMBIENT = RGB.BLACK;
    public static final RGB COLOR = RGB.YELLOW;
    
//    private ILight light = new PointLight(new Vec3(0, 0, 20), AMBIENT, COLOR, 10);
    
    private ILight light2 = new PointLight(new Vec3(10, 0, 20), AMBIENT, COLOR, 1000);
    private ILight light3 = new PointLight(new Vec3(50, 0, 20), AMBIENT, COLOR, 10);
    private ILight mainLight = new DirectionalLight(new Vec3(0, 0, 10), AMBIENT, COLOR);
//    private ILight light = new SpotLight(Vec3.Z, AMBIENT, COLOR, Vec3.Z, 0, 0);

    private IMesh lightMesh;
    
    IGPUImage t = IGPUImage.read(GameWorld.class.getResource("/textures/tron.jpg"));
    private IMesh ground = MeshUtilities.createGroundPlane(new ColorMapMaterial(t), 1000);
    
    private final int offset = 40;
    
    private long udpOrder = 0;

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


    public void createWorld(IScene scene, IView view) throws IOException {
        System.out.println("Create Game world");

        // Set main player camera
        List<ICamera> cameras = mplayer.getCameras();
        controller.setCamera(view, cameras.get(0));

        // Ground
        scene.add3DObject(ground);
       
        // Lights
        // Add first light and light geometry
        GeodesicSphere s = new GeodesicSphere(4);
        lightMesh = new DefaultMesh(Primitive.TRIANGLES, new ColorMaterial(RGBA.YELLOW),
                DefaultGeometry.createV(s.getTriangles()), Flag.DONT_CAST_SHADOW);
        lightMesh.setTransform(Mat4.trs(0, 0, 0, 0, 0, 0, 10f, 10f, 10f));
        lightMesh.setPosition(new Vec3(0, 0, 100));
        
        //light.setPosition(lightMesh.getPosition());
        scene.add3DObjects(mainLight);
//        scene.add3DObjects(light3);
        
        scene.add3DObjects(lightMesh);

    }
    
    List<List<IMesh>> shaders = new ArrayList<>();
    
    /**
     * Main gain loop
     */
    public void run() {
        controller.animate((time, interval) -> {
            // Read server input
            try {
                sendUDPData();
                getUDPData(time);
            } catch (SocketTimeoutException e) {
                // Ignore timeouts
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
            for(List<IMesh> meshes : shaders) {
            	for(IMesh mesh : meshes) {
            		((PowerUpMaterial) mesh.getMaterial()).setRedGain((float) Math.sin(time) + 1);	
            	}
            }
            
            controller.viewChanged(controller.getCurrentView());
        });
    }
    

    public void addPlayer(Player p, boolean main) {
        this.players.put(p.getId(), p);
        List<IMesh> x = p.getTrace().getPoints();
        System.out.println(x.size());
        this.controller.getScene().add3DObjects(x);
        this.controller.getScene().add3DObjects(p.getLight());

        
//        for(Point point : p.getTrace().getPoints()) {
//            this.controller.getScene().add3DObject(point.getMesh());
//        }
        

        if (main) {
            this.mplayer = p;
            this.controller.getScene().add3DObjects(mplayer.getMesh());
            this.controller.getScene().add3DObjects(mplayer.getCameras());

        }
    }

    public void setMovemnet(int movement, boolean pressed) {
        UserInput mov = UserInput.getEnumByKey(movement);
        // Remove
        if(UserInput.getEnumByKey(movement) == UserInput.SPACE && pressed) {
            System.out.println("SPACE");
            controller.setCamera(controller.getCurrentView(), mplayer.switchCamera());

            ;
        }
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
        
//        cam.setPosition(pos);
//        cam.setTarget(direction);

    }
    
//    private void removePlayer(Player p) {
//        for(Point point : p.getTrace().getPoints()) {
//            this.controller.getScene().remove3DObject(point.getMesh());
//        }
//        this.controller.getScene().remove3DObjects(p.getMesh());
//    }

    private void getUDPData(double time) throws IOException {
        this.udpSocket.receive(this.packet);
        this.packet.getData();
        byte[] data = this.packet.getData();

        // 8 Bytes = order
        // 1 byte = num players
        // 16 Bytes = uuid
        // 12 Bytes position
        // 12 Bytes rotation
        
        Long order = ByteBuffer.wrap(data, 0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        
        if(order < this.udpOrder) {
            return;
        }
        
        this.udpOrder = order;
        
        byte num_players = ByteBuffer.wrap(data, 8, 1).order(ByteOrder.LITTLE_ENDIAN).get();
//        System.out.println("Active Players: " +num_players);
        Set<String> activePlayers = new ArraySet<>();

        for (int i = 0; i < num_players; i++) {
            int index = offset * i;
            byte[] uuid = Arrays.copyOfRange(data, index + 9, index + 25);
            ByteBuffer byteBuffer = ByteBuffer.wrap(uuid);
            Long high = byteBuffer.getLong();
            Long low = byteBuffer.getLong();

            Vec3 player_pos = new Vec3(ByteBuffer.wrap(data, index + 25, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                    ByteBuffer.wrap(data, index + 29, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                    ByteBuffer.wrap(data, index + 33, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat());

            Vec3 rotation = new Vec3(ByteBuffer.wrap(data, index + 37, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                    ByteBuffer.wrap(data, index + 41, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                    ByteBuffer.wrap(data, index + 45, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat());

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
            
            controller.getScene().remove3DObjects(p.getTrace().getPoints());
            p.getTrace().notify(p.getPositionCopy().subtract(rot_z.transform(new Vec3(3, 0, 0))));
            controller.getScene().add3DObjects(p.getTrace().getPoints());
            
            p.setDirection(rotation);
            
            if (p == mplayer) {
                Vec3 ppos = mplayer.getPosition();
                p.setFrontCamera(ppos.subtract(rot_z.transform(new Vec3(25, 0, -5))), rotation.x);
                p.setBackCamera(ppos.add(rot_z.transform(new Vec3(25, 0, 5))),  rotation.x);

//                updateCamera();
                
            }
            controller.getScene().remove3DObject(p.getLight());
            Vec3 direction =  p.getPosition().subtract(p.getCamera().getPosition());
            
//            direction. = 0f;
            direction = new Vec3(direction.x, direction.y, 0f);
            p.setLight(direction);
            controller.getScene().add3DObject(p.getLight());
            
            
            

        }
        
        // Check if player is deleted
        
//        if(this.players.size() > activePlayers.size()) {
//            // Remove a player
//            for(Object id : this.players.keySet().toArray()) {
//                if(!activePlayers.contains((String)id))
//                    this.removePlayer(this.players.get((String)id));
//                    this.players.remove((String)id);
//
//            }
//        }

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
            this.controller.getScene().add3DObjects(pu.getMesh());
            shaders.add(pu.getMesh());
        }
        
    }

}
