package ch.fhnw.main;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IEventScheduler;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent;
import ch.fhnw.ether.controller.tool.AbstractTool;
import ch.fhnw.ether.controller.tool.ITool;
import ch.fhnw.ether.scene.camera.DefaultCameraControl;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.view.IView;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

// Default camera control

public class EventHandler extends AbstractTool {

    private ITool fallbackTool;

    public static final RGBA GRID_COLOR = RGBA.GRAY;

    private final int MOVE_FORWARD = 265;
    private final int MOVE_BACKWARD = 264;
    private final int MOVE_LEFT = 263;
    private final int MOVE_RIGHT = 262;

    IController controller;
    IMesh player = MeshUtilities.createCube();

    protected EventHandler(IController controller) {
        super(controller);
        this.controller = controller;
    }

    protected EventHandler(IController controller, ITool fallbackTool) {
        this(controller);
        this.fallbackTool = fallbackTool;
        this.controller = controller;
    }

    public ITool getFallbackTool() {
        return fallbackTool;
    }

    public void setFallbackTool(ITool fallbackTool) {
        this.fallbackTool = fallbackTool;
    }

    @Override
    public void activate() {
        //IView view = getController().getCurrentView();
        fallbackTool.activate();
    }

    @Override
    public void deactivate() {
        IView view = getController().getCurrentView();
        fallbackTool.deactivate();
    }

    @Override
    public void refresh(IView view) {
        fallbackTool.refresh(view);
    }

    @Override
    public void keyPressed(IKeyEvent e) {

        DefaultCameraControl control = new DefaultCameraControl(getCamera(e.getView()));
        float moveFactor = 0.001f * control.getDistance();

        if (e.getKey() == MOVE_FORWARD) {
            //control.track(0, moveFactor * 20);
            // Start animation forward
            
            // Start time merken
            this.controller.animate(new IEventScheduler.IAnimationAction() {
                private float c = 0f;
                
                

                @Override
                public void run(double time, double interval) {
                    if( time > 2) {
                        controller.kill(this);
                    }
                    c += 0.2f;
                    if(c >= 5) {
                        return;
                    }
                    Vec3 pos = player.getPosition();
                    Mat4 transfrom = Mat4.translate(pos.x, pos.y+c, pos.z);
                    // apply changes to geometry
                    player.setTransform(transfrom);
                }
            });

        }

        if (e.getKey() == MOVE_BACKWARD) {
            //control.track(0, moveFactor * -20);
            this.controller.animate(new IEventScheduler.IAnimationAction() {
                private float c = 0f;

                @Override
                public void run(double time, double interval) {
                    if( time > 2) {
                        controller.kill(this);
                    }
                    c += 0.2f;
                    if(c >= 5) {
                        return;
                    }
                    System.out.println(time);
                    Vec3 pos = player.getPosition();
                    Mat4 transfrom = Mat4.translate(pos.x, pos.y - c, pos.z);
                    // apply changes to geometry
                    player.setTransform(transfrom);
                }
            });
        }

        if (e.getKey() == MOVE_RIGHT) {
            control.track(moveFactor * 20, 0);
        }

        if (e.getKey() == MOVE_LEFT) {
            control.track(moveFactor * -20, 0);
        }

        getController().viewChanged(e.getView());
        System.out.println("Key pressed: " + e.getKey());
        fallbackTool.keyPressed(e);
    }

    @Override
    public void keyReleased(IKeyEvent e) {
        System.out.println("Key released: " + e.getKey());
        fallbackTool.keyReleased(e);
    }

    @Override
    public void pointerPressed(IPointerEvent e) {
        // Ignore
        /*
         * if (e.isModifierDown()) { mouseX = e.getX(); mouseY = e.getY(); button = e.getButton(); }
         * else {
         */
        fallbackTool.pointerPressed(e);
        // }
    }

    @Override
    public void pointerReleased(IPointerEvent e) {
        if (e.isModifierDown()) {
            // nop
        } else {
            fallbackTool.pointerReleased(e);
        }
    }

    @Override
    public void pointerClicked(IPointerEvent e) {
        if (e.isModifierDown()) {
            // nop
        } else {
            fallbackTool.pointerClicked(e);
        }
    }

    @Override
    public void pointerMoved(IPointerEvent e) {
        fallbackTool.pointerMoved(e);
    }

    @Override
    public void pointerDragged(IPointerEvent e) {
        return;
    }

    // FIXME: find a solution for OS-dependent stuff like this
    @Override
    public void pointerScrolled(IPointerEvent e) {
        return;
    }
    

}
