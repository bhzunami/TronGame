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

package ch.fhnw.ether.examples.mapping;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.view.IView;

public final class MappingExample {
	public static void main(String[] args) {
		new MappingExample();
		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Creates a sample setup with 1 control view and 4 projector views, each with 90 degrees rotation around the model.
	 * Uses the sample calibration model with a calibration rig of 0.5 units (~meters) edge lenght, and an additional
	 * square 4 points at 0.8 units.
	 */
	public MappingExample() {
		Platform.get().init();
		IController controller = new MappingController();
		controller.run(time -> {
			// FIXME: for every view use separate camera with dedicated angle
			new MappingView(controller, 0, 10, 512, 512, IView.INTERACTIVE_VIEW, "View 0");
			new MappingView(controller, 530, 0, 400, 400, IView.RENDER_VIEW, "View 1");
			// new MappingView(controller, 940, 0, 400, 400, ViewType.MAPPED_VIEW, "View 2", 90.0f);
			// new MappingView(controller, 530, 410, 400, 400, ViewType.MAPPED_VIEW, "View 3", 180.0f);
			// new MappingView(controller, 940, 410, 400, 400, ViewType.MAPPED_VIEW, "View 4", 270.0f);
	
			IScene scene = new MappingScene(controller);
			controller.setScene(scene);
		});
		Platform.get().run();
	}
}
