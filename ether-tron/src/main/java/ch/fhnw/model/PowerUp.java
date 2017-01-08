package ch.fhnw.model;

import java.util.List;

import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.util.math.Vec3;

public class PowerUp {//extends AbstractMaterial implements ICustomMaterial{
    
    private List<IMesh> meshes;
    private Vec3 position;
    
    
    public PowerUp(float x, float y) {
//        super(providedAttributes, geometryAttributes);
        this.position = new Vec3(x, y, 0);
        this.meshes = PowerUpShader.makeColoredTriangle();
        for(IMesh mesh : meshes) {
        	mesh.setPosition(this.position);
        }
    }

    public List<IMesh> getMesh() {
        return meshes;
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
