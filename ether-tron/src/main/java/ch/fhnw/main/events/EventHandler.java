package ch.fhnw.main.events;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.tool.AbstractTool;
import ch.fhnw.ether.controller.tool.ITool;
import ch.fhnw.ether.view.IView;
import ch.fhnw.model.GameWorld;
import ch.fhnw.util.color.RGBA;

// Default camera control

public class EventHandler extends AbstractTool {

    private ITool fallbackTool;

    public static final RGBA GRID_COLOR = RGBA.GRAY;

    public static final int MOVE_FORWARD = 265;
    public static final int MOVE_BACKWARD = 264;
    public static final int MOVE_LEFT = 263;
    public static final int MOVE_RIGHT = 262;
    public static final int SPACE = 32;

    IController controller;
    GameWorld gw;

    public EventHandler(IController controller, GameWorld gameWorld) {
        super(controller);
        this.controller = controller;
        this.gw = gameWorld;
    }

    public EventHandler(IController controller, ITool fallbackTool, GameWorld gameWorld) {
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
        fallbackTool.deactivate();
    }

    @Override
    public void refresh(IView view) {
        fallbackTool.refresh(view);
    }

    @Override
    public void keyPressed(IKeyEvent e) {
        GameWorld.STATE = GameWorld.USER_INPUT;
        
        if (UserInput.isInEnum(e.getKey())) {
                gw.setMovement(e.getKey(), true);
        }

        fallbackTool.keyPressed(e);
    }

    @Override
    public void keyReleased(IKeyEvent e) {
        GameWorld.STATE = GameWorld.IDLE;
        if (UserInput.isInEnum(e.getKey())) {
                gw.setMovement(e.getKey(), false);
            }
        fallbackTool.keyReleased(e);
        
    }

}
