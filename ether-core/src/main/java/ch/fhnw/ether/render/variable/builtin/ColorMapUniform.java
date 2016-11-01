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

import java.util.function.Supplier;

import org.lwjgl.opengl.GL11;

import ch.fhnw.ether.image.IGPUTexture;
import ch.fhnw.ether.render.variable.base.SamplerUniform;
import ch.fhnw.ether.scene.mesh.material.IMaterial;

public final class ColorMapUniform extends SamplerUniform {
	private static final String DEFAULT_SHADER_NAME = "colorMap";

	public ColorMapUniform() {
		this(DEFAULT_SHADER_NAME);
	}

	public ColorMapUniform(String shaderName) {
		this(shaderName, null);
	}

	public ColorMapUniform(String shaderName, int unit) {
		this(shaderName, unit, null);
	}

	public ColorMapUniform(Supplier<IGPUTexture> supplier) {
		this(DEFAULT_SHADER_NAME, supplier);
	}

	public ColorMapUniform(String shaderName, Supplier<IGPUTexture> supplier) {
		super(IMaterial.COLOR_MAP, shaderName, 0, GL11.GL_TEXTURE_2D, supplier);
	}
	
	public ColorMapUniform(String shaderName, int unit, Supplier<IGPUTexture> supplier) {
		super(IMaterial.COLOR_MAP, shaderName, unit, GL11.GL_TEXTURE_2D, supplier);
	}
}
