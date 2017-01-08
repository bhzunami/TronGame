package ch.fhnw.helper;

import ch.fhnw.ether.scene.IScene;
import ch.fhnw.model.Point;
import ch.fhnw.util.math.Vec3;

public class Trace {
    
    private Point[] points;
    private int traceIterator;
    
    
    public Trace(int numPoints) {
        this.points = new Point[numPoints];
        
        for(int i = 0; i < this.points.length; i++) {
            Point point = new Point();
            points[i] = point;
        }
        
    }
    
    public Point[] getPoints() {
        return this.points;
    }
    
    
    public void notify(Vec3 position) {
        this.points[this.traceIterator].setPosition(position);
        this.traceIterator++;
        if(this.traceIterator >= this.points.length) {
            this.traceIterator = 0;
        }
    }

}
