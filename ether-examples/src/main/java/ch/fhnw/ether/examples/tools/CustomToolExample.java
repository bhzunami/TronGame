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

package ch.fhnw.ether.examples.tools;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.view.DefaultView;
import ch.fhnw.ether.view.IView;
import ch.fhnw.util.math.Vec3;

public final class CustomToolExample {
	public static void main(String[] args) {
		new CustomToolExample();
	}

	public CustomToolExample() {
		// Create controller
		IController controller = new DefaultController();
		controller.run(time -> {
			// Create view
			IView view = new DefaultView(controller, 100, 100, 500, 500, IView.INTERACTIVE_VIEW, "Tool Example");

			// Create scene and add a camera and some objects
			IScene scene = new DefaultScene(controller);
			controller.setScene(scene);

			ICamera camera = new Camera();
			
			IMesh mesh0 = MeshUtilities.createCube();
			IMesh mesh1 = MeshUtilities.createCube();
			IMesh mesh2 = MeshUtilities.createCube();
			IMesh mesh3 = MeshUtilities.createCube();
			mesh0.setPosition(new Vec3(-6, -1, 6.5f));
			mesh1.setPosition(new Vec3(+1, -1, 0.5f));
			mesh2.setPosition(new Vec3(+4, +1, 0.5f));
			mesh3.setPosition(new Vec3(-1, +1, 0.5f));
			scene.add3DObjects(camera, mesh0, mesh1, mesh2, mesh3);
			
			controller.setCamera(view, camera);
			
			// Create UI
			//ITool areaTool = new AreaTool(controller);
//			UI ui = controller.getUI();
//			ui.addWidget(new Slider(0, 4, "SLIDER", "Slider", 0.3f, (slider, v) -> System.out.println("Slider " + slider.getValue())));
//			ui.addWidget(new Button(0, 3, "PICK", "Pick Tool (1)", GLFW.GLFW_KEY_1, (button, v) -> controller.setTool(new NavigationTool(controller, new PickTool(controller)))));
//			ui.addWidget(new Button(0, 2, "AREA", "AREA Tool (2)", GLFW.GLFW_KEY_2, (button, v) -> controller.setTool(new NavigationTool(controller, areaTool))));
//			ui.addWidget(new Button(0, 1, "F", "Frame Scene (F)", GLFW.GLFW_KEY_F, (button, v) -> {
//				new FrameCameraControl(camera, scene.getMeshes()).frame();
//			}));
//			ui.addWidget(new Button(0, 0, "Quit", "Quit", GLFW.GLFW_KEY_ESCAPE, (button, v) -> System.exit(0)));			
		});
	}
}
