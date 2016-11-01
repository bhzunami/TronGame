/*
 * Copyright (c) 2013 - 2016 Stefan Muller Arisona, Simon Schubiger
 * Copyright (c) 2013 - 2016 FHNW & ETH Zurich
 * All rights reserved.
 *
 * Contributions by: Filip Schramka, Samuel von Stachelski
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *  Neither the name of FHNW / ETH Zurich nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */package ch.fhnw.ether.controller;

import java.util.Collection;
import java.util.List;

import ch.fhnw.ether.controller.event.IEventScheduler.IAction;
import ch.fhnw.ether.controller.event.IEventScheduler.IAnimationAction;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent;
import ch.fhnw.ether.controller.tool.ITool;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.render.IRenderManager;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.view.IView;

/**
 * A controller that coordinates both model and associated views. It also
 * handles the relevant events coming from individual views.
 * 
 * Thread-safety: Unless otherwise stated, methods are not thread-safe and
 * should be called only from scene thread.
 *
 * @author radar
 */
public interface IController {
	/**
	 * Get the controller's model.
	 *
	 * @return the controller's model
	 */
	IScene getScene();

	/**
	 * Set the controller's model. This effectively unhooks the current model
	 * from the controller and replaces it with the new one. If a controller
	 * implementation does not implement such behavior it will throw an
	 * {@link java.lang.UnsupportedOperationException}.
	 *
	 * @param model
	 *            to be set
	 */
	void setScene(IScene model);

	/**
	 * Get a list of all views.
	 *
	 * @return list of views
	 */
	List<IView> getViews();

	/**
	 * Get current view (i.e. the view that currently receives events).
	 *
	 * @return the current view
	 */
	IView getCurrentView();

	/**
	 * Enable a list of views for rendering.
	 *
	 * @param views
	 *            list of views to be enabled for rendering or NULL to enable
	 *            all views
	 */
	void enableViews(Collection<IView> views);

	/**
	 * Get active camera for given view.
	 */
	ICamera getCamera(IView view);

	/**
	 * Set active camera for given view.
	 */
	void setCamera(IView view, ICamera camera);

	/**
	 * Get the current tool.
	 *
	 * @return the current tool
	 */
	ITool getTool();

	/**
	 * Set the current tool.
	 *
	 * @param tool
	 *            the tool to be set as current tool
	 */
	void setTool(ITool tool);

	/**
	 * Get the renderer.
	 *
	 * @return the renderer
	 */
	IRenderManager getRenderManager();

	/**
	 * Add an action to the model animation loop until it removes itself via
	 * {@link #kill(IAnimationAction)}. This is a shorthand for
	 * getScheduler().animate(). Thread-safe.
	 * 
	 * @param action
	 *            Action to be run
	 */
	void animate(IAnimationAction action);

	/**
	 * Remove an action from model animation loop. This is a shorthand for
	 * getScheduler().animate(). Thread-safe.
	 * 
	 * @param action
	 */
	void kill(IAnimationAction action);

	/**
	 * Run an action on scene thread once. This is a shorthand for
	 * getScheduler().run(). Thread-safe.
	 * 
	 * @param action
	 *            Action to be run
	 */
	void run(IAction action);

	/**
	 * Run an action on scene thread once, with given delay. This is a shorthand
	 * for getScheduler().run(). Thread-safe.
	 * 
	 * @param delay
	 *            Delay before action is run, in seconds
	 * @param action
	 *            Action to be run
	 */
	void run(double delay, IAction action);

	/**
	 * Get the scheduler associated with this controller.
	 * @return The scheduler.
	 */
	IScheduler getScheduler();
	
	/**
	 * Request repaint of all views. This is a shorthand for
	 * getScheduler().repaint(). Thread-safe.
	 */
	void repaint();

	/**
	 * Returns true if caller calls from scene thread.
	 */
	boolean isSceneThread();

	/**
	 * Throws {@link java.lang.IllegalThreadStateException} if caller does not
	 * call from scene thread.
	 */
	void ensureSceneThread();

	// a long list of events, redirected from the view. the view will make sure
	// that these methods are all run on the scene thread.

	// view listener

	void viewCreated(IView view);

	void viewDisposed(IView view);

	void viewGainedFocus(IView view);

	void viewLostFocus(IView view);
	
	void viewResized(IView view);

	void viewChanged(IView view);

	// key listener

	void keyPressed(IKeyEvent e);

	void keyReleased(IKeyEvent e);

	// pointer listener

	void pointerEntered(IPointerEvent e);

	void pointerExited(IPointerEvent e);

	void pointerPressed(IPointerEvent e);

	void pointerReleased(IPointerEvent e);

	void pointerClicked(IPointerEvent e);

	// pointer motion listener

	void pointerMoved(IPointerEvent e);

	void pointerDragged(IPointerEvent e);

	// pointer scroll listener

	void pointerScrolled(IPointerEvent e);

}
