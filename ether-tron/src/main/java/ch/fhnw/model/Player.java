package ch.fhnw.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.light.ILight;
import ch.fhnw.ether.scene.light.SpotLight;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.helper.Trace;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class Player extends Model implements Serializable {

    private static final long serialVersionUID = 3003306163179867285L;
    private static final int NUMPOINTS = 600;

    @JsonIgnore
    private String name;
    private String id;
    private float abs_rot_angle = 0f;
    private Vec3 direction;
    private Trace trace;
    private ILight light;
    private ICamera backCam;
    private ICamera frontCam;
    private ICamera activeCam;

    public Player(String name, boolean isPrimary) throws IOException {
        super("tron.obj");

        this.name = name;
        this.direction = new Vec3(1, 0, 0);
        this.trace = new Trace(NUMPOINTS, isPrimary);
        this.light = new SpotLight(Vec3.Z, GameWorld.AMBIENT, GameWorld.COLOR, Vec3.Z, 0, 0);
        this.backCam = new Camera();
        this.frontCam = new Camera();
        this.activeCam = this.frontCam;

    }

    @JsonIgnore
    public Vec3 getPosition() {
        return getMeshes().get(0).getPosition();
    }

    public Vec3 getPositionCopy() {
        return new Vec3(getMeshes().get(0).getPosition().x, getMeshes().get(0).getPosition().y,
                getMeshes().get(0).getPosition().z);
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
        for (IMesh mesh : getMeshes()) {
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
        if (rot > 0) {
            pos = new Vec3(position.x + Math.abs(rot) * 2, position.y, position.z);
        } else if (rot < 0) {
            pos = new Vec3(position.x + Math.abs(rot) * 2, position.y, position.z);
        } else {
            pos = new Vec3(position.x, position.y, position.z);
        }

        this.frontCam.setPosition(pos);
        this.frontCam.setTarget(this.getPosition());
    }

    public void setBackCamera(Vec3 position, float rot) {
        Vec3 pos;
        if (rot > 0) {
            pos = new Vec3(position.x + Math.abs(rot) * 2, position.y, position.z);
        } else if (rot < 0) {
            pos = new Vec3(position.x + Math.abs(rot) * 2, position.y, position.z);
        } else {
            pos = new Vec3(position.x, position.y, position.z);
        }

        this.backCam.setPosition(pos);
        this.backCam.setTarget(this.getPosition());
    }

    public ICamera switchCamera() {
        if (this.activeCam == this.frontCam) {
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
