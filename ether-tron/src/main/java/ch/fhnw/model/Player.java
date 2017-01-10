package ch.fhnw.model;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.fhnw.ether.formats.IModelReader.Options;
import ch.fhnw.ether.formats.obj.ObjReader;
import ch.fhnw.ether.scene.I3DObject;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.light.ILight;
import ch.fhnw.ether.scene.light.SpotLight;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.helper.Trace;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class Player implements Serializable {

    private static final long serialVersionUID = 3003306163179867285L;
    private static final int NUMPOINTS = 600;

    @JsonIgnore
    private List<IMesh> mesh;
    private String name;
    private String id;
    private float abs_rot_angle = 0f;
    private Vec3 direction;
    private Trace trace;
    private ILight light;
    private ICamera backCam;
    private ICamera frontCam;
    private ICamera activeCam;
    
    
    public Player() {}
    
    public Player(String name) throws IOException {
        this.name = name;
        this.mesh = Player.getPlayerMesh("blender.obj");
        this.direction = new Vec3(1, 0, 0);
        this.trace = new Trace(NUMPOINTS);
        this.light = new SpotLight(Vec3.Z, GameWorld.AMBIENT, GameWorld.COLOR, Vec3.Z, 0, 0);
        this.backCam = new Camera();
        this.frontCam = new Camera();
        this.activeCam = this.frontCam;


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
        for(IMesh mesh : this.mesh) {
            mesh.setPosition(pos);
        }
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
        this.setPosition(position);
    }
    
    
    public void rotate(Mat4 transformation) {
    	for(IMesh mesh : this.mesh) {
    		mesh.setTransform(transformation);	
    	}
    }
    
    public Trace getTrace() {
        return this.trace;
    }
    
    public void setLight(Vec3 direction) {
        light = new SpotLight(this.getPosition(), GameWorld.AMBIENT, GameWorld.COLOR, direction, 10, 0);
    }
    
    public ILight getLight() {
        return this.light;
    }
    
    public void setFrontCamera(Vec3 position, float rot) {
        Vec3 pos;
        if(rot > 0) {
            pos = new Vec3(position.x+Math.abs(rot)*2, position.y, position.z);
        } else if(rot < 0) {
            pos = new Vec3(position.x+Math.abs(rot)*2, position.y, position.z);
        } else {
            pos = new Vec3(position.x, position.y, position.z);
        }
                
        this.frontCam.setPosition(pos);
        this.frontCam.setTarget(this.getPosition());
    }
    
    
    public void setBackCamera(Vec3 position, float rot) {
        Vec3 pos;
        if(rot > 0) {
            pos = new Vec3(position.x+Math.abs(rot)*2, position.y, position.z);
        } else if(rot < 0) {
            pos = new Vec3(position.x+Math.abs(rot)*2, position.y, position.z);
        } else {
            pos = new Vec3(position.x, position.y, position.z);
        }
                
        this.backCam.setPosition(pos);
        this.backCam.setTarget(this.getPosition());
    }
    
    
    public ICamera switchCamera() {
        if(this.activeCam == this.frontCam) {
            return this.activeCam = this.backCam;
        } else {
            return this.activeCam = this.frontCam;
        }
    }
    
    
    public ICamera getCamera() {
        return this.activeCam;
    }

    public List<ICamera> getCameras() {
        List<ICamera> cameras = new ArrayList<>();
        cameras.add(this.frontCam);
        cameras.add(this.backCam);
        return cameras;
    }

}
