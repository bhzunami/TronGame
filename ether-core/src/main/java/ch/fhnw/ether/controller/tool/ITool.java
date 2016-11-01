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

import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent;
import ch.fhnw.ether.view.IView;

public interface ITool {
	ITool NULL_TOOL = new ITool() {
		@Override
		public void activate() {
		}

		@Override
		public void deactivate() {
		}

		@Override
		public void refresh(IView view) {
		}

		@Override
		public void keyPressed(IKeyEvent e) {
		}

		@Override
		public void keyReleased(IKeyEvent e) {
		}

		@Override
		public void pointerPressed(IPointerEvent e) {
		}

		@Override
		public void pointerReleased(IPointerEvent e) {
		}
		
		@Override
		public void pointerClicked(IPointerEvent e) {	
		}

		@Override
		public void pointerMoved(IPointerEvent e) {
		}

		@Override
		public void pointerDragged(IPointerEvent e) {
		}

		@Override
		public void pointerScrolled(IPointerEvent e) {
		}		
	};
	
	/**
	 * Called when tool is activated (e.g. a new tool is selected). Note that
	 * this call is always followed by
	 * {@link #refresh(ch.fhnw.ether.view.IView)}.
	 */
	void activate();

	/**
	 * Called when tool is deactivated.
	 */
	void deactivate();

	/**
	 * Called when current view has changed (e.g. its camera). Tools should use this to
	 * update view-dependent geometry.
	 *
	 * @param view
	 *            the current view
	 */
	void refresh(IView view);

	// key listener
	void keyPressed(IKeyEvent e);
	void keyReleased(IKeyEvent e);

	// pointer listener
	void pointerPressed(IPointerEvent e);
	void pointerReleased(IPointerEvent e);
	void pointerClicked(IPointerEvent e);

	// pointer motion listener
	void pointerMoved(IPointerEvent e);
	void pointerDragged(IPointerEvent e);

	// pointer scroll listener
	void pointerScrolled(IPointerEvent e);
}
