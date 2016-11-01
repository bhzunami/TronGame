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

package ch.fhnw.ether.render.variable.base;

import java.nio.FloatBuffer;

import ch.fhnw.ether.render.IRenderer.RendererAttribute;
import ch.fhnw.ether.render.gl.Program;
import ch.fhnw.util.math.Mat3;
import ch.fhnw.util.math.Mat4;

public class UniformBlock extends AbstractUniform<Integer> {
	private boolean canBind = true;
	private boolean isBound = false;
	
	private int bindingPoint;

	public UniformBlock(RendererAttribute<Integer> attribute, String shaderName) {
		super(attribute, shaderName);
	}

	@Override
	public final void update(Object[] data) {
		bindingPoint = fetch(data);
	}

	@Override
	public final void enable(Program program) {
		if (canBind && !isBound) {
			int index = program.getUniformBlockIndex(getShaderName());
			if (index == -1) {
				canBind = false;
			} else {
				program.bindUniformBlock(index, bindingPoint);
				isBound = true;
			}
		}
	}

	@Override
	public String toString() {
		return super.toString() + "[" + isBound + "]";
	}
	
	public static void addMat4(FloatBuffer buffer, Mat4 mat) {
		buffer.put(mat.toArray());
	}
	
	public static void addMat3(FloatBuffer buffer, Mat3 mat) {
		buffer.put(mat.m00);
		buffer.put(mat.m10);
		buffer.put(mat.m20);
		buffer.put(0);

		buffer.put(mat.m01);
		buffer.put(mat.m11);
		buffer.put(mat.m21);
		buffer.put(0);

		buffer.put(mat.m02);
		buffer.put(mat.m12);
		buffer.put(mat.m22);
		buffer.put(0);
	}
}
