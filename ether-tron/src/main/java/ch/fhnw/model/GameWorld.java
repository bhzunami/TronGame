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
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.material.ColorMapMaterial;
import ch.fhnw.ether.view.IView;
import ch.fhnw.helper.JoinResponseDao;
import ch.fhnw.main.events.UserInput;
import ch.fhnw.model.PowerUpShader.PowerUpMaterial;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.math.Vec3;

public class GameWorld {

    // The player which plays on this view
    private Player mplayer;

    private Map<String, Player> players = new HashMap<>();

    private List<Block> blocks = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();

    private IController controller;
    public static final RGB AMBIENT = RGB.BLACK;
    public static final RGB COLOR = RGB.YELLOW;

    private final int messageSize = 40;

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

    public GameWorld(IController controller, Player p, DatagramSocket socket, String server) throws IOException {
        this.controller = controller;
        this.mplayer = p;
        this.udpSocket = socket;
        this.udpSocket.setReceiveBufferSize(1);
        this.server = InetAddress.getByName(server);

    }

    public void createWorld(IView view, JoinResponseDao gameData) throws IOException {
        mplayer.setId(gameData.getId());
        addPlayer(mplayer);
        addBlocks(gameData.getBlocks());
        addPowerUps(gameData.getPowerUps());

        // Set main player camera
        List<ICamera> cameras = mplayer.getCameras();
        controller.setCamera(view, cameras.get(0));

        controller.getScene().add3DObjects(mplayer.getCameras());
        controller.getScene().add3DObjects(new Ground().getMeshes());
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

            for (List<IMesh> meshes : shaders) {
                for (IMesh mesh : meshes) {
                    ((PowerUpMaterial) mesh.getMaterial()).setRedGain((float) Math.sin(time) + 1);
                }
            }

            controller.viewChanged(controller.getCurrentView());
        });
    }

    public void addPlayer(Player p) {
        this.players.put(p.getId(), p);
        this.controller.getScene().add3DObjects(p.getTrace().getMeshes());
        this.controller.getScene().add3DObjects(p.getLight());
        this.controller.getScene().add3DObjects(p.getMeshes());
    }

    public void setMovement(int movement, boolean pressed) {
        UserInput mov = UserInput.getEnumByKey(movement);
        // Remove
        if (UserInput.getEnumByKey(movement) == UserInput.SPACE && pressed) {
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

        if (order < this.udpOrder) {
            // skip out of order packets
            return;
        }

        this.udpOrder = order;

        byte num_players = ByteBuffer.wrap(data, 8, 1).order(ByteOrder.LITTLE_ENDIAN).get();
        // System.out.println("Active Players: " +num_players);
        Set<String> activePlayers = new ArraySet<>();

        for (int i = 0; i < num_players; i++) {
            // read message part
            int index = messageSize * i;
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
                // new player in game
                p = new Player(puuid, false);
                this.addPlayer(p);
            }

            activePlayers.add(p.getId());
            p.updatePosition(player_pos, rotation, controller.getScene());
        }

        removeInactivePlayers(activePlayers);
    }

    private void removeInactivePlayers(Set<String> activePlayers) {
        // check for inactive players
        Map<Boolean, List<Player>> result = players.values().stream()
                .collect(Collectors.groupingBy(player -> activePlayers.contains(player.getId())));

        // update player list with active players
        players = result.get(true).stream().collect(Collectors.toMap(p -> p.getId(), p -> p));

        // remove inactive players
        if (result.containsKey(false)) {
            result.get(false).stream().forEach(player -> {
                System.out.println("Remove Player: " + player.getId());
                controller.getScene().remove3DObjects(player.getMeshes());
                controller.getScene().remove3DObjects(player.getTrace().getMeshes());
                controller.getScene().remove3DObject(player.getLight());
            });
        }
    }

    public void addLight(Vec3 position) throws IOException {
        Vec3 lightPos = new Vec3(position.x, position.y, position.z + 10);

        LightSource l = new LightSource(lightPos);
        this.controller.getScene().add3DObjects(l.getMeshes());
        this.controller.getScene().add3DObject(l.getLight());
    }

    public void addBlocks(float[][] blocks) throws IOException {
        for (int i = 0; i < blocks.length; i++) {
            Vec3 position = new Vec3(blocks[i][0], blocks[i][1], 0);
            Block b = new Block(position.x, position.y);
            this.blocks.add(b);
            this.controller.getScene().add3DObjects(b.getMeshes());

            if (i % 17 == 0) {
                this.addLight(position);
            }
        }
    }

    public void addPowerUps(float[][] powerUps) {
        for (int i = 0; i < powerUps.length; i++) {
            PowerUp pu = new PowerUp(powerUps[i][0], powerUps[i][1]);
            this.powerUps.add(pu);
            this.controller.getScene().add3DObjects(pu.getMeshes());
            shaders.add(pu.getMeshes());
        }

    }

}
