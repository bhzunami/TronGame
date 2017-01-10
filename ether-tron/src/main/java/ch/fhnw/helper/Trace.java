package ch.fhnw.helper;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.model.Point;
import ch.fhnw.util.math.Vec3;

public class Trace {
    
    private List<IMesh> points;
    
    private List<IMesh> merged;
    
    private int traceIterator = 0;
    private int numPoints;
    
    
    public Trace(int numPoints) {
        this.numPoints = numPoints;
        this.points = new ArrayList<>();
        
        for(int i = 0; i < numPoints; i++) {
            Point point = new Point();
            points.add(point.getMesh());
        }
        
        merged = MeshUtilities.mergeMeshes(this.points);
        
    }
    
    public List<IMesh> getPoints() {
        return merged;
    }
    
    
    public void notify(Vec3 position) {
        this.points.get(this.traceIterator).setPosition(position);
        this.traceIterator++;
        if(this.traceIterator >= numPoints) {
            this.traceIterator = 0;
        }
        
        merged = MeshUtilities.mergeMeshes(this.points);
    }

}
