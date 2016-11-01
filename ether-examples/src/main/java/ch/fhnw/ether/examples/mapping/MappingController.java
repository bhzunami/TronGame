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

import org.lwjgl.glfw.GLFW;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.tool.AbstractTool;
import ch.fhnw.ether.controller.tool.ITool;
import ch.fhnw.ether.controller.tool.NavigationTool;
import ch.fhnw.ether.mapping.BoxCalibrationModel;
import ch.fhnw.ether.mapping.tool.CalibrationTool;
import ch.fhnw.ether.mapping.tool.FillTool;
import ch.fhnw.util.math.Vec3;

public class MappingController extends DefaultController {
	private static final String[] HELP = { "Simple Mapping Example (Without Content)", "", "[1] Default Tool / View", "[2] Mapping Calibration",
			"[3] Projector Adjustment", "", "Use Mouse Buttons + Shift or Mouse Wheel to Navigate" };

	private final ITool defaultTool = new AbstractTool(this) {
	};

	private final CalibrationTool calibrationTool = new CalibrationTool(this, new BoxCalibrationModel(0.5f, 0.5f, 0.5f, 0.8f, 0.8f));
	private final FillTool fillTool = new FillTool(this);

	public MappingController() {
	}

	@Override
	public void keyPressed(IKeyEvent e) {
		switch (e.getKey()) {
		case GLFW.GLFW_KEY_0:
		case GLFW.GLFW_KEY_1:
			setTool(new NavigationTool(this, defaultTool));
			break;
		case GLFW.GLFW_KEY_2:
			setTool(new NavigationTool(this, calibrationTool));
			break;
		case GLFW.GLFW_KEY_3:
			setTool(new NavigationTool(this, fillTool));
			break;
		case GLFW.GLFW_KEY_H:
			printHelp(HELP);
			break;
		default:
			super.keyPressed(e);
		}
	}

	public Vec3 getLightPosition() {
		return Vec3.Z;
	}

	public void setLightPosition(Vec3 position) {
		// unimplemented
	}
}
