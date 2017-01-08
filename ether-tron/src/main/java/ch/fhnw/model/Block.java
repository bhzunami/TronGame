package ch.fhnw.model;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.fhnw.ether.formats.IModelReader.Options;
import ch.fhnw.ether.formats.obj.ObjReader;
import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMapMaterial;
import ch.fhnw.ether.scene.mesh.material.IMaterial;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.GeodesicSphere;

public class Block  implements Serializable {

    private static final long serialVersionUID = 3003306163179867285L;

    @JsonIgnore
    private IMesh mesh;
    private String id;
    
    
    public Block(float x, float y) throws IOException {
        this.id = UUID.randomUUID().toString();
        final URL obj = getClass().getResource("/models/block.obj");
        mesh = new ObjReader(obj, Options.CONVERT_TO_Z_UP).getMeshes().get(0);
        this.setPosition(new Vec3(x, y, 0));
        
//        GeodesicSphere sphere = new GeodesicSphere(15);
//        IGPUImage t = IGPUImage.read(GameWorld.class.getResource("/textures/tron.jpg"));
//        mesh = new DefaultMesh(Primitive.TRIANGLES, new ColorMapMaterial(t), DefaultGeometry.createVM(sphere.getTriangles(), sphere.getTexCoords()), Queue.DEPTH);

        
    }
    
    
    public void setPosition(Vec3 pos) {
        this.mesh.setPosition(pos);
    }
    
    
    public IMesh getMesh() {
        return this.mesh;
    }
    
    public String getId() {
        return this.id;
    }

}
