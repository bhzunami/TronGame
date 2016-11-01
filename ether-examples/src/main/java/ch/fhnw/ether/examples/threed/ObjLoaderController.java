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

package ch.fhnw.ether.examples.threed;

import org.lwjgl.glfw.GLFW;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.util.math.Vec3;

public class ObjLoaderController extends DefaultController {
	private static final String[] HELP = { 
		//@formatter:off
		"Simple Obj Loader Example", 
		"", 
		"[1-6] Side Views", 
		"", 
		"Use Mouse Buttons + Shift or Mouse Wheel to Navigate" 
		//@formatter:on
	};
	
	private static final Vec3[][] CAM_PARAMS = {
		//@formatter:off
		{ new Vec3(5, 0, 0), Vec3.Z }, 
		{ new Vec3(-5, 0, 0), Vec3.Z },
		{ new Vec3(0, 5, 0), Vec3.Z }, 
		{ new Vec3(0, -5, 0), Vec3.Z }, 
		{ new Vec3(0, 0, 5), Vec3.Y }, 
		{ new Vec3(0, 0, -5), Vec3.Y_NEG }
		//@formatter:on
	};

	public ObjLoaderController() {
	}

	@Override
	public void keyPressed(IKeyEvent e) {
		switch (e.getKey()) {
		case GLFW.GLFW_KEY_1:
		case GLFW.GLFW_KEY_2:
		case GLFW.GLFW_KEY_3:
		case GLFW.GLFW_KEY_4:
		case GLFW.GLFW_KEY_5:
		case GLFW.GLFW_KEY_6:
			Vec3[] params = CAM_PARAMS[e.getKey() - GLFW.GLFW_KEY_1];
			ICamera camera = getCamera(getCurrentView());
			camera.setPosition(params[0]);
			camera.setUp(params[1]);
			break;
		case GLFW.GLFW_KEY_H:
			printHelp(HELP);
			break;
		default:
			super.keyPressed(e);
		}
	}
}
