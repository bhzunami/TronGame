package ch.fhnw.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.material.ColorMapMaterial;
import ch.fhnw.util.math.Vec3;

public class Ground extends Model {
    public Ground() throws IOException {
        super(createGround());
    }
    
    private static List<IMesh> createGround() throws IOException {
        IGPUImage t = IGPUImage.read(GameWorld.class.getResource("/textures/ground.jpg"));
        List<IMesh> parts = new ArrayList<>();

        int stepSize = 200;
        int partSize = stepSize / 2;

        for (int x = -1000; x < 1000; x += stepSize) {
            for (int y = -1000; y < 1000; y += stepSize) {
                IMesh ground = MeshUtilities.createGroundPlane(new ColorMapMaterial(t), partSize);
                ground.setPosition(new Vec3(x + partSize, y + partSize, 0));
                parts.add(ground);
            }
        }

        return MeshUtilities.mergeMeshes(parts);
    }
}
