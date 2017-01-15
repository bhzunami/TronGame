package ch.fhnw.helper;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Vec3;

public class Trace {

    private List<IMesh> points;

    private List<IMesh> merged;

    private int traceIterator = 0;
    private int numPoints;

    public Trace(int numPoints, boolean isPrimary) {
        this.numPoints = numPoints;
        this.points = new ArrayList<>();

        for (int i = 0; i < numPoints; i++) {
            points.add(createPoint(isPrimary));
        }

        merged = MeshUtilities.mergeMeshes(this.points);

    }

    public static IMesh createPoint(boolean isPrimary) {
        IMesh mesh = MeshUtilities.createCube(new ColorMaterial(isPrimary ? RGBA.CYAN : RGBA.GREEN));
        mesh.setPosition(new Vec3(2000f, 0f, -20f));
        return mesh;
    }

    public List<IMesh> getPoints() {
        return merged;
    }

    public void notify(Vec3 position) {
        this.points.get(this.traceIterator).setPosition(position);
        this.traceIterator++;
        if (this.traceIterator >= numPoints) {
            this.traceIterator = 0;
        }

        merged = MeshUtilities.mergeMeshes(this.points);
    }

}
