package ch.fhnw.model;

import ch.fhnw.ether.render.shader.IShader;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.material.AbstractMaterial;
import ch.fhnw.ether.scene.mesh.material.ICustomMaterial;
import ch.fhnw.util.math.Vec3;

public class PowerUp {//extends AbstractMaterial implements ICustomMaterial{
    
    private IMesh mesh;
    private Vec3 position;
    
    
    public PowerUp(float x, float y) {
//        super(providedAttributes, geometryAttributes);
        this.position = new Vec3(x, y, 0);
        this.mesh = MeshUtilities.createCylinder(4, true);
        this.mesh.setPosition(this.position);
    }


    public IMesh getMesh() {
        return mesh;
    }


    public void setMesh(IMesh mesh) {
        this.mesh = mesh;
    }


    public Vec3 getPosition() {
        return position;
    }


    public void setPosition(Vec3 position) {
        this.position = position;
    }


//    @Override
//    public IShader getShader() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//
//    @Override
//    public Object[] getData() {
//        // TODO Auto-generated method stub
//        return null;
//    }   
    
    
    

}
