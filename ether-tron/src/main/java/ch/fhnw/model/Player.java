package ch.fhnw.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.fhnw.ether.formats.IModelReader.Options;
import ch.fhnw.ether.formats.obj.ObjReader;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class Player implements Serializable {

    private static final long serialVersionUID = 3003306163179867285L;

    @JsonIgnore
    private List<IMesh> mesh;
    private String name;
    private String id;
    private float abs_rot_angle = 0f;
    private Vec3 direction;
    
    
    public Player() {}
    
    public Player(String name) throws IOException {
        this.name = name;
        this.mesh = Player.getPlayerMesh("blender.obj");
        this.direction = new Vec3(1, 0, 0);


    }
    
    public static List<IMesh> getPlayerMesh(String filename) throws IOException {
        //final URL obj = new File(filename).toURI().toURL();
        final URL obj = Player.class.getResource("/models/" + filename);
    	final List<IMesh> meshes = new ArrayList<>();
    	new ObjReader(obj, Options.CONVERT_TO_Z_UP).getMeshes().forEach(mesh -> meshes.add(mesh));
    	
    	return MeshUtilities.mergeMeshes(meshes);
    }
    
    @JsonIgnore
    public Vec3 getPosition() {
        return this.mesh.get(0).getPosition();
    }
    
    public Vec3 getPositionCopy() {
        return new Vec3(this.mesh.get(0).getPosition().x, this.mesh.get(0).getPosition().y, this.mesh.get(0).getPosition().z);
    }
    
    public void setPosition(Vec3 pos) {
        this.mesh.get(0).setPosition(pos);
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
    
    @JsonIgnore
    public List<IMesh> getMesh() {
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
    	for(IMesh mesh : this.mesh) {
    		mesh.setTransform(transformation);	
    	}
    }

}
