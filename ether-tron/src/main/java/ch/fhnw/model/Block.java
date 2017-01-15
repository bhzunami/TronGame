package ch.fhnw.model;

import java.io.IOException;
import java.io.Serializable;

import ch.fhnw.util.math.Vec3;

public class Block extends Model implements Serializable {

    private static final long serialVersionUID = 3003306163179867285L;

    
    public Block(float x, float y) throws IOException {
        this.meshes = loadBlenderObject("cube.obj");
        this.setPosition(new Vec3(x, y, 0));
    }
   

}
