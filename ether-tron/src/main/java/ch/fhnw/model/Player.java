package ch.fhnw.model;

import java.io.Serializable;
import java.util.UUID;

import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Vec3;

public class Player implements Serializable {

    private static final long serialVersionUID = 3003306163179867285L;

    private IMesh mesh;
    private String name;
    private String id;
    
    
    public Player(String name) {
        this.name = name;
        this.id = UUID.randomUUID().toString();
        this.mesh = MeshUtilities.createCube();
    }
    
    public Vec3 getPosition() {
        return this.mesh.getPosition();
    }
    
    public void setPosition(Vec3 pos) {
        this.mesh.setPosition(pos);
    }
    
    public String getId() {
        return this.id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setMesh(IMesh mesh) {
        this.mesh = mesh;
    }
    
    public IMesh getMesh() {
        return this.mesh;
    }
    
    @Override
    public String toString() {
        return id;
    }


}
