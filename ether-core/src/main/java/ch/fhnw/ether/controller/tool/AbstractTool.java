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
 */

package ch.fhnw.ether.controller.tool;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.view.IView;

public abstract class AbstractTool implements ITool {
	private static final int SNAP_SIZE = 4;

	private final IController controller;

	protected AbstractTool(IController controller) {
		this.controller = controller;
	}

	protected final IController getController() {
		return controller;
	}
	
	protected final ICamera getCamera(IView view) {
		return controller.getCamera(view);
	}
	
	@Override
	public void activate() {
	}

	@Override
	public void deactivate() {
	}

	@Override
	public void refresh(IView view) {
	}

	// key listener

	@Override
	public void keyPressed(IKeyEvent e) {
	}
	
	@Override
	public void keyReleased(IKeyEvent e) {
	}

	// mouse listener

	@Override
	public void pointerPressed(IPointerEvent e) {
	}

	@Override
	public void pointerReleased(IPointerEvent e) {
	}
	
	@Override
	public void pointerClicked(IPointerEvent e) {
	}

	// mouse motion listener

	@Override
	public void pointerMoved(IPointerEvent e) {
	}

	@Override
	public void pointerDragged(IPointerEvent e) {
	}

	// mouse wheel listener

	@Override
	public void pointerScrolled(IPointerEvent e) {
	}

	// FIXME: get rid of this or move to PickUtil
	public static boolean snap2D(float mx, float my, float x, float y) {
		return (mx >= x - SNAP_SIZE) && (mx <= x + SNAP_SIZE) && (my >= y - SNAP_SIZE) && (my < y + SNAP_SIZE);
	}
}
