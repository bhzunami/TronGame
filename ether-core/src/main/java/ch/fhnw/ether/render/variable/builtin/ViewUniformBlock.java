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

package ch.fhnw.ether.render.variable.builtin;

import ch.fhnw.ether.render.IRenderer.RendererAttribute;
import ch.fhnw.ether.render.gl.FloatUniformBuffer;
import ch.fhnw.ether.render.variable.base.UniformBlock;
import ch.fhnw.ether.scene.camera.IViewCameraState;
import ch.fhnw.util.math.Mat4;

public final class ViewUniformBlock extends UniformBlock {
	public static final RendererAttribute<Integer> ATTRIBUTE = new RendererAttribute<>("builtin.view_uniform_block");

	// 3 * mat4 + 1 * mat3 + 3 pad
	// layout: viewMatrix, viewProjMatrix, projMatrix, normalMatrix
	// 3x (3D, ortho device space, ortho screen space)
	public static final int BLOCK_SIZE = 3 * 16 + 9 + 3;

	private static final String DEFAULT_SHADER_NAME = "viewBlock";

	private static final float[] ID_4X4 = Mat4.ID.toArray();
	private static final float[] PAD_12 = new float[12];

	public ViewUniformBlock() {
		super(ATTRIBUTE, DEFAULT_SHADER_NAME);
	}

	public ViewUniformBlock(String shaderName) {
		super(ATTRIBUTE, shaderName);
	}

	public static void loadUniforms(FloatUniformBuffer uniforms, IViewCameraState vcs) {
		uniforms.load((blockIndex, buffer) -> {
			switch (blockIndex) {
			case 0:
				// 3d setup
				addMat4(buffer, vcs.getViewMatrix());
				addMat4(buffer, vcs.getViewProjMatrix());
				addMat4(buffer, vcs.getProjMatrix());
				addMat3(buffer, vcs.getNormalMatrix());
				break;
			case 1:
				// ortho device space
				buffer.put(ID_4X4);
				buffer.put(ID_4X4);
				buffer.put(ID_4X4);
				buffer.put(PAD_12);
				break;
			case 2:
				// ortho screen space
				float[] ortho = Mat4.ortho(0, vcs.getViewport().w, 0, vcs.getViewport().h, -1, 1).toArray();
				buffer.put(ID_4X4);
				buffer.put(ortho);
				buffer.put(ortho);
				buffer.put(PAD_12);
				break;
			}
		});
	}
}
