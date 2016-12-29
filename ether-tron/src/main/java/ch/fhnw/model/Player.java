package ch.fhnw.model;

import java.io.IOException;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class Player implements Serializable {

    private static final long serialVersionUID = 3003306163179867285L;

    @JsonIgnore
    private IMesh mesh;
    private String name;
    private String id;
    private float abs_rot_angle = 0f;
    private Vec3 direction;
    
    
    public Player() {}
    
    public Player(String name) throws IOException {
        this.name = name;
        this.mesh = MeshUtilities.createCube();
        this.direction = new Vec3(1, 0, 0);

    }
    
    @JsonIgnore
    public Vec3 getPosition() {
        return this.mesh.getPosition();
    }
    
    public void setPosition(Vec3 pos) {
        this.mesh.setPosition(pos);
    }
    
    public String getId() {
        return this.id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    @JsonIgnore
    public IMesh getMesh() {
        return this.mesh;
    }
    
    @Override
    public String toString() {
        return id;
    }
    
    public float getRotAngle() {
        return this.abs_rot_angle;
    }
    
    public void setRotAngle(float angle) {
        this.abs_rot_angle += angle;
    }
    
    public Vec3 getDirection() {
        return this.direction;
    }

    public void setDirection(Vec3 direction) {
        this.direction = direction;
    }
    
    public void updatePosition(Vec3 position) {
        if(position.x > 1000 || position.y > 1000) {
            return;
        }
        this.setPosition(position);
    }
    
    
    public void rotate(Mat4 transformation) {
        this.mesh.setTransform(transformation);
    }

}
