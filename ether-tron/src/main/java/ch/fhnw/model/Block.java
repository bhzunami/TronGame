package ch.fhnw.model;

import java.io.IOException;

import ch.fhnw.util.math.Vec3;

public class Block extends Model {
    public Block(float x, float y) throws IOException {
        super("cube.obj");
        this.setPosition(new Vec3(x, y, 0));
    }
}
