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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.tritonus.share.ArraySet;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.light.ILight;
import ch.fhnw.ether.scene.light.PointLight;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMapMaterial;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
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
    private Map<String, Player> players = new HashMap<>();
    private List<Block> blocks = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();
    
    private IController controller;
    public static final RGB AMBIENT = RGB.BLACK;
    public static final RGB COLOR = RGB.YELLOW;
    
    private IMesh lightMesh;
    
    private final int offset = 40;
    
    private long udpOrder = 0;

    DatagramSocket udpSocket;
    
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
    
    public static List<IMesh> createGround() throws IOException {
        IGPUImage t = IGPUImage.read(GameWorld.class.getResource("/textures/ground.jpg"));
        List<IMesh> parts = new ArrayList<>();

        int stepSize = 200;
        int partSize = stepSize / 2;

        for (int x = -1000; x < 1000; x += stepSize) {
            for (int y = -1000; y < 1000; y += stepSize) {
                IMesh ground = MeshUtilities.createGroundPlane(new ColorMapMaterial(t), partSize);
                ground.setPosition(new Vec3(x+partSize, y+partSize, 0));
                parts.add(ground);
            }
        }
        
        return MeshUtilities.mergeMeshes(parts);
    }


    public void createWorld(IScene scene, IView view) throws IOException {
        System.out.println("Create Game world");

        // Set main player camera
        List<ICamera> cameras = mplayer.getCameras();
        controller.setCamera(view, cameras.get(0));

        // Ground
        scene.add3DObjects(createGround());
       
        // Lights
        // Add first light and light geometry
        GeodesicSphere s = new GeodesicSphere(4);
        lightMesh = new DefaultMesh(Primitive.TRIANGLES, new ColorMaterial(RGBA.YELLOW),
                DefaultGeometry.createV(s.getTriangles()), Flag.DONT_CAST_SHADOW);
        lightMesh.setTransform(Mat4.trs(0, 0, 0, 0, 0, 0, 10f, 10f, 10f));
        lightMesh.setPosition(new Vec3(0, 0, 100));
        
        
        int pos = 0;
        for(int i = 0; i < 6; i++) {
            ILight light = new PointLight(new Vec3(pos, 0, 20), AMBIENT, RGB.WHITE, 250 );
            
            LightSource l = new LightSource();
            l.setPosition(new Vec3(pos, 0, 0));
            scene.add3DObjects(l.getMeshes());
            
            lightMesh = new DefaultMesh(Primitive.TRIANGLES, new ColorMaterial(RGBA.YELLOW),
                    DefaultGeometry.createV(s.getTriangles()), Flag.DONT_CAST_SHADOW);
            lightMesh.setTransform(Mat4.trs(0, 0, 0, 0, 0, 0, 10f, 10f, 10f));
            lightMesh.setPosition(new Vec3(pos, 0, 20));
            scene.add3DObjects(lightMesh);
            light.setPosition(lightMesh.getPosition());
            scene.add3DObject(light);
            System.out.println("X COR: " +i);

            pos += 20;
        }
        
    
        //light.setPosition(lightMesh.getPosition());
//        scene.add3DObjects(mainLight);
//        scene.add3DObjects(light3);
        
//        scene.add3DObjects(lightMesh);

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
            this.controller.getScene().add3DObjects(mplayer.getMeshes());
            this.controller.getScene().add3DObjects(mplayer.getCameras());

        }
    }

    public void setMovement(int movement, boolean pressed) {
        UserInput mov = UserInput.getEnumByKey(movement);
        // Remove
        if(UserInput.getEnumByKey(movement) == UserInput.SPACE && pressed) {
            System.out.println("SPACE");
            controller.setCamera(controller.getCurrentView(), mplayer.switchCamera());
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
                p = new Player("test", false);
                p.setId(puuid);
                this.addPlayer(p, false);
                this.controller.getScene().add3DObjects(p.getMeshes());
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

		// remove inactive players
		Map<Boolean, List<Player>> result = players.values().stream().collect(Collectors.groupingBy(player -> activePlayers.contains(player.getId())));
		
		players = result.get(true).stream().collect(Collectors.toMap(p -> p.getId(), p -> p));
		
		if(result.containsKey(false)) {
			result.get(false).stream().forEach(player -> {
				System.out.println("Remove Player: " + player.getId());
				controller.getScene().remove3DObjects(player.getMeshes());
				controller.getScene().remove3DObjects(player.getTrace().getPoints());
			});	
		}
    }
    

    public void addBlocks(float[][] blocks) throws IOException {
        for(int i = 0; i < blocks.length; i++) {
            Block b = new Block(blocks[i][0], blocks[i][1]);
            this.blocks.add(b);
            this.controller.getScene().add3DObjects(b.getMeshes());
        }        
    }

    public void addPowerUps(float[][] powerUps) {
        for(int i = 0; i < powerUps.length; i++) {
            PowerUp pu = new PowerUp(powerUps[i][0], powerUps[i][1]);
            this.powerUps.add(pu);
            this.controller.getScene().add3DObjects(pu.getMeshes());
            shaders.add(pu.getMeshes());
        }
        
    }

}
