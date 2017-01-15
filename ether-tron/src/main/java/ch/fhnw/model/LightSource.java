package ch.fhnw.model;

import java.io.IOException;

import ch.fhnw.ether.scene.light.ILight;
import ch.fhnw.ether.scene.light.PointLight;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Vec3;

public class LightSource extends Model{
    
    ILight light;
   
    
    public LightSource(Vec3 position) throws IOException {
        this.meshes = loadBlenderObject("lamp.obj");
        Vec3 lightSourcePosition = new Vec3(position.x, position.y, position.z +15);
        light = new PointLight(lightSourcePosition, RGB.BLACK, new RGB(131f/255f, 244f/255f, 142f/255f), 1000);
        this.setPosition(position);
    }
    
    
    public ILight getLight() {
        return this.light;
    }

   
    
}
