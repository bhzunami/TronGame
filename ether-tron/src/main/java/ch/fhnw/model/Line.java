package ch.fhnw.model;

import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

public class Line {
    
    private IMesh mesh;
    
    public Line(float angle) {
      this.mesh = MeshUtilities.createDisk(4);
      
      Mat4 rotx = Mat4.rotate(45, Vec3.X);
      Mat4 roty = Mat4.rotate(90, Vec3.Y);
      Mat4 rotz = Mat4.rotate(angle, Vec3.Z);
      Mat4 rotxy = Mat4.multiply(rotx, roty);
      Mat4 rot = Mat4.multiply(rotxy, rotz);
      Mat4 scale = Mat4.scale(0.5f);
      this.mesh.setTransform(Mat4.multiply(rot, scale));

    }
    
    
    public IMesh getMesh() {
        return this.mesh;
    }

}
