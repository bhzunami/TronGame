package ch.fhnw.model;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.fhnw.ether.formats.IModelReader.Options;
import ch.fhnw.ether.formats.obj.ObjReader;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.util.math.Vec3;

public abstract class Model {
    
    List<IMesh> meshes;
    
    
    public Model() {}
    
    public Model(String filename) throws IOException {
        this.meshes = loadBlenderObject(filename);
        
    }
    
    public Model(String filename, Vec3 position) throws IOException {
        this.meshes = loadBlenderObject(filename);
        this.setPosition(position);
        
    }
        
    
    public List<IMesh> loadBlenderObject(String filename) throws IOException {
        URL obj = Player.class.getResource("/models/" + filename);
        List<IMesh> meshes = new ArrayList<>();
        new ObjReader(obj, Options.CONVERT_TO_Z_UP).getMeshes().forEach(mesh -> meshes.add(mesh));
        
        return MeshUtilities.mergeMeshes(meshes);
        
    }
    
    @JsonIgnore
    public List<IMesh> getMeshes() {
        return this.meshes;
    }
    
    
    
    public void setPosition(Vec3 position) {
        for(IMesh m : this.meshes) {
            m.setPosition(position);
        }
    }
    
    
    

}
