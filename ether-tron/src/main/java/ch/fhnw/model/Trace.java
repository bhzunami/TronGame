package ch.fhnw.model;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class Trace extends Model {
    private static Mat4 CUBE_FORM = Mat4.scale(new Vec3(10, 1, 1));
            
    private List<IMesh> points = new ArrayList<>();

    private List<IMesh> merged;

    private int traceIterator = 0;
    private int numPoints;

    public Trace(int numPoints, boolean isPrimary) {
        this.numPoints = numPoints;

        for (int i = 0; i < numPoints; i++) {
            points.add(createPoint(isPrimary));
        }

        merged = MeshUtilities.mergeMeshes(this.points);
    }

    public static IMesh createPoint(boolean isPrimary) {
        IMesh mesh = MeshUtilities.createCube(new ColorMaterial(isPrimary ? RGBA.CYAN : RGBA.GREEN));
        mesh.setPosition(new Vec3(0, 0, -200));
        return mesh;
    }

    @Override
    public List<IMesh> getMeshes() {
        return merged;
    }

    public void notify(Vec3 position, Mat4 rotation) {
        // last block gets moved to the front
        this.points.get(this.traceIterator).setPosition(position);
        this.points.get(this.traceIterator).setTransform(Mat4.multiply(rotation, CUBE_FORM));
        this.traceIterator++;
        if (this.traceIterator >= numPoints) {
            this.traceIterator = 0;
        }

        merged = MeshUtilities.mergeMeshes(this.points);
    }

}
