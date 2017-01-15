package ch.fhnw.main.events;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.tool.AbstractTool;
import ch.fhnw.ether.controller.tool.ITool;
import ch.fhnw.ether.view.IView;
import ch.fhnw.model.GameWorld;

// Default camera control

public class EventHandler extends AbstractTool {

    private ITool fallbackTool;
    private GameWorld gw;

    public EventHandler(IController controller, ITool fallbackTool, GameWorld gameWorld) {
        super(controller);
        this.fallbackTool = fallbackTool;
        this.gw = gameWorld;
    }

    public ITool getFallbackTool() {
        return fallbackTool;
    }

    public void setFallbackTool(ITool fallbackTool) {
        this.fallbackTool = fallbackTool;
    }

    @Override
    public void activate() {
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
