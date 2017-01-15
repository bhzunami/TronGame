package ch.fhnw.model;

import java.io.IOException;

public class LightSource extends Model{
   
    
    public LightSource() throws IOException {
        this.meshes = loadBlenderObject("lampe.obj");
        
    }

   
    
}
