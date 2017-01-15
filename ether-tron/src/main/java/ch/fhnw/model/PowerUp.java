package ch.fhnw.model;

import ch.fhnw.util.math.Vec3;

public class PowerUp extends Model {

    public PowerUp(float x, float y) {
        super(PowerUpShader.makeColoredTriangle());
        this.setPosition(new Vec3(x, y, 0));
    }
}
