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
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IEventScheduler;
import ch.fhnw.ether.formats.IModelReader.Options;
import ch.fhnw.ether.formats.obj.ObjReader;
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
//    private HashMap<String, Player> players = new HashMap<>();
    private List<Player> players = new ArrayList<>();
    private ICamera cam;
    private List<IMesh> ramps = new ArrayList<>();
    private List<Block> blocks = new ArrayList<>();
    private List<IMesh> powerUps = new ArrayList<>();
    private IController controller;
    private static final RGB AMBIENT = RGB.BLACK;
    private static final RGB COLOR = RGB.YELLOW;
    private ILight light = new DirectionalLight(Vec3.Z, AMBIENT, COLOR);
    private IMesh lightMesh;
    private IMesh ground = MeshUtilities.createGroundPlane(1000);
    
    
    private Vec3 direction = new Vec3(1, 0, 0);

    private final int offset = 40;

    DatagramSocket udpSocket;
    private float angle = 0;
    
    private float abs_rot_angle = 0f;
    private InetAddress server;

    private byte[] buffer = new byte[1024];
    private DatagramPacket packet = new DatagramPacket( buffer, buffer.length) ;

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
    

    public GameWorld(IController controller, Player p, DatagramSocket socket, String server) throws UnknownHostException, SocketException {
        this.controller = controller;
        this.mplayer = p;
        this.udpSocket =  socket;
        this.udpSocket.setReceiveBufferSize(1);
        this.server = InetAddress.getByName(server);

    }


//    public void playerMoved(Vec3 pos) {
//        ICamera camera = this.controller.getScene().getCameras().get(0);
//        camera.setPosition(pos);
//    }

    public void createWorld(IScene scene, IView view) throws IOException {
        System.out.println("Create Game world");

        
        this.controller.getScene().add3DObject(mplayer.getMesh());

        // Add player
//        for(Player p : this.players) {
//            final URL obj = getClass().getResource("/models/cube.obj");
//            final List<IMesh> meshes = new ArrayList<>();
//            new ObjReader(obj, Options.CONVERT_TO_Z_UP).getMeshes().forEach(mesh -> meshes.add(mesh));
//            final List<IMesh> merged = MeshUtilities.mergeMeshes(meshes);
////            p.setMesh(new ObjReader(obj, Options.CONVERT_TO_Z_UP).getMeshes().get(0));
//            this.controller.getScene().add3DObject(p.getMesh());
//        }

        cam = new Camera();
        controller.getScene().add3DObject(cam);
        controller.setCamera(view, cam);
        
        // Ground
        
        scene.add3DObject(ground);
        
        
        // Lights
     // Add first light and light geometry
        GeodesicSphere s = new GeodesicSphere(4);
        lightMesh = new DefaultMesh(Primitive.TRIANGLES, new ColorMaterial(RGBA.YELLOW), DefaultGeometry.createV(s.getTriangles()), Flag.DONT_CAST_SHADOW);
        lightMesh.setTransform(Mat4.trs(0, 0, 0, 0, 0, 0, 0.1f, 0.1f, 0.1f));
        lightMesh.setPosition(new Vec3(0, 0, 2));
        light.setPosition(lightMesh.getPosition());
        controller.getScene().add3DObjects(light);
        controller.getScene().add3DObjects(lightMesh);
        
        
        // Add blocks

        for(int i = 0; i < 100; i++) {
            Block block = new Block();
            Random r = new Random();
            int Low = -1000;
            int High = 1000;
            int x = r.nextInt(High-Low) + Low;
            int y = r.nextInt(High-Low) + Low;
            System.out.println("SET Object position:" +x +" "+y);
            block.setPosition(new Vec3(x, y, 0));
            this.blocks.add(block);
            this.controller.getScene().add3DObject(block.getMesh());
        }
        
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
//                if((time - old_time) % 1000 < 5 ) {
//                    System.out.println("Draw line");
//                    
//                    System.out.println("line drawed");
//                }
//                old_time = time;
                controller.viewChanged(controller.getCurrentView());

            }
        });
    }

    public void addPlayer(Player p) {
//        this.players.put(p.getId(), p);
        this.players.add(p);
        this.mplayer = p;
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
//            players.get(0).getMesh().setTransform(Mat4.multiply(Mat4.rotate(angle, Vec3.Y), Mat4.translate(0, 0, 0)));
            break;
        case MOVE_BACKWARD:
            break;
        case MOVE_LEFT:
            if(angle - 2f < -35) break;
            angle -= 2f;
//            players.get(0).getMesh().setTransform(Mat4.rotate(angle, Vec3.Y));
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

    
    private void updateCamera(Vec3 playerPosition, Vec3 direction) {
        Vec3 camara_position = new Vec3(playerPosition.x, playerPosition.y -6, playerPosition.z +2);
        cam.setPosition(camara_position);
        cam.setTarget(playerPosition);
        
    }
    private void getUDPData() throws IOException {
        this.udpSocket.receive(this.packet);
        this.packet.getData();
        byte[] data = this.packet.getData();
                
        // 1 byte = num playes
        // 16 Bytes = uuid
        // 12 Bytes position
        // 12 Bytes rotation
        byte num_players = ByteBuffer.wrap(data, 0, 1).order(ByteOrder.LITTLE_ENDIAN).get();
        
        System.out.println("NUM Players: " +num_players);
        
        byte[] uuid = Arrays.copyOfRange(data, 1, 17);
        ByteBuffer byteBuffer = ByteBuffer.wrap(uuid);
        Long high = byteBuffer.getLong();
        Long low = byteBuffer.getLong();
        
        Vec3 player_pos = new Vec3(ByteBuffer.wrap(data, 17,4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                ByteBuffer.wrap(data, 21,4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                ByteBuffer.wrap(data, 25,4).order(ByteOrder.LITTLE_ENDIAN).getFloat());
        
        players.get(0).updatePosition(player_pos);
        
        
        Vec3 rotation = new Vec3(ByteBuffer.wrap(data,29,4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                ByteBuffer.wrap(data,33,4).order(ByteOrder.LITTLE_ENDIAN).getFloat(),
                ByteBuffer.wrap(data,37,4).order(ByteOrder.LITTLE_ENDIAN).getFloat());
        
    
        this.abs_rot_angle +=   MathUtilities.RADIANS_TO_DEGREES * (rotation.z - this.direction.z);
        
        Mat4 rot_z = Mat4.rotate(this.abs_rot_angle, Vec3.Z);
        Mat4 rot_x = Mat4.rotate(MathUtilities.RADIANS_TO_DEGREES *rotation.x, Vec3.X);
        players.get(0).getMesh().setTransform(Mat4.multiply(rot_z,rot_x));
         
        Vec3 playerPos = players.get(0).getPosition();
        Vec3 cam_pos = playerPos.subtract(rot_z.transform(new Vec3(5, 0, -1)));
             
        cam.setPosition(cam_pos);
        cam.setTarget(player_pos);
        
//        Line line = new Line(MathUtilities.RADIANS_TO_DEGREES *rotation.x);
//        Vec3 line_pos = playerPos.subtract(rot_z.transform(new Vec3(2, 0, 0)));
//        line.getMesh().setPosition(line_pos);
//        controller.getScene().add3DObject(line.getMesh());
        this.direction = rotation;

    }
    


}
