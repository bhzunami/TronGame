package ch.fhnw.model;

import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class Point {
    
    private IMesh mesh;
    private Vec3 position;
    
    public Point() {
        this.position = new Vec3(2000f, 0f, -20f);
        this.mesh = MeshUtilities.createCube();
        this.mesh.setPosition(this.position);
    }
    
    
    public void setPosition(Vec3 position) {
        this.position = position;
        this.mesh.setPosition(this.position);
    }
    
    public IMesh getMesh() {
        return this.mesh;
    }

}
