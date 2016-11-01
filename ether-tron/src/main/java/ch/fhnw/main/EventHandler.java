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
import ch.fhnw.model.GameWorld;
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
    GameWorld gw;

    protected EventHandler(IController controller, GameWorld gameWorld) {
        super(controller);
        this.controller = controller;
        this.gw = gameWorld;
    }

    protected EventHandler(IController controller, ITool fallbackTool, GameWorld gameWorld) {
        this(controller, gameWorld);
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
        GameWorld.STATE = GameWorld.USER_INPUT;

        if (e.getKey() == MOVE_FORWARD) {
            gw.moveForward();
        }

        if (e.getKey() == MOVE_BACKWARD) {
            gw.moveBackward();
        }

        if (e.getKey() == MOVE_RIGHT) {
            gw.moveRight();
            
        }

        if (e.getKey() == MOVE_LEFT) {
            gw.moveLeft();
        }

        
        fallbackTool.keyPressed(e);
    }

    @Override
    public void keyReleased(IKeyEvent e) {
        GameWorld.STATE = GameWorld.IDLE;
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
