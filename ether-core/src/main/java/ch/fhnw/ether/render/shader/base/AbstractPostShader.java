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

package ch.fhnw.ether.render.shader.base;

import org.lwjgl.opengl.GL11;

import ch.fhnw.ether.render.gl.Texture;
import ch.fhnw.ether.render.variable.base.SamplerUniform;
import ch.fhnw.ether.render.variable.builtin.ColorMapArray;
import ch.fhnw.ether.render.variable.builtin.PositionArray;
import ch.fhnw.ether.render.variable.builtin.ViewUniformBlock;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;

public abstract class AbstractPostShader extends AbstractShader {
	
	private final SamplerUniform colorMapUniform;
	private final SamplerUniform depthMapUniform;
	
	protected AbstractPostShader(Class<?> root, String name, String source, Primitive type) {
		super(root, name, source, type);
		colorMapUniform = new SamplerUniform("post.color_map", "colorMap", 0, GL11.GL_TEXTURE_2D);
		depthMapUniform = new SamplerUniform("post.depth_map", "depthMap", 1, GL11.GL_TEXTURE_2D);
		addUniforms();
	}

	protected AbstractPostShader(Class<?> root, String name, String vert, String frag, String geom, Primitive type) {
		super(root, name, vert, frag, geom, type);
		colorMapUniform = new SamplerUniform("post.color_map", "colorMap", 0, GL11.GL_TEXTURE_2D);
		depthMapUniform = new SamplerUniform("post.depth_map", "depthMap", 1, GL11.GL_TEXTURE_2D);
		addUniforms();
	}

	public void setMaps(Texture colorMap, Texture depthMap) {
		// XXX kind of hacky that we need to explicitly call update here, there should be a better way...
		colorMapUniform.setSupplier(() -> colorMap);
		colorMapUniform.update(null);
		depthMapUniform.setSupplier(() -> depthMap);
		depthMapUniform.update(null);
	}
	
	private void addUniforms() {
		addArray(new PositionArray());
		addArray(new ColorMapArray());
		
		addUniform(colorMapUniform);
		addUniform(depthMapUniform);

		addUniform(new ViewUniformBlock());
		
		setMaps(null, null);
	}
}
