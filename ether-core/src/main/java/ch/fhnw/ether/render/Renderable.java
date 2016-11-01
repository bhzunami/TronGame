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

package ch.fhnw.ether.render;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.lwjgl.opengl.GL11;

import ch.fhnw.ether.render.shader.IShader;
import ch.fhnw.ether.scene.attribute.IAttribute;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;

public final class Renderable {
	private final IShader shader;
	private final VertexBuffer buffer;
	private final IMesh.Queue queue;
	private final Set<IMesh.Flag> flags;

	public Renderable(IMesh mesh, Map<IAttribute, Supplier<?>> globals) {
		this(null, mesh, globals);
	}

	public Renderable(IShader shader, IMesh mesh, Map<IAttribute, Supplier<?>> globals) {
		this.shader = ShaderBuilder.create(shader, mesh, globals);
		this.buffer = new VertexBuffer(this.shader, mesh.getGeometry().getAttributes());
		this.queue = mesh.getQueue();
		this.flags = mesh.getFlags();
	}

	public void update(Object[] materialData, float[][] geometryData) {
		if (materialData != null)
			shader.update(materialData);
		if (geometryData != null)
			buffer.update(geometryData);
	}

	public void render() {
		boolean disableCulling = hasFlag(Flag.DONT_CULL_FACE);
		if (disableCulling)
			GL11.glDisable(GL11.GL_CULL_FACE);
		shader.enable();
		shader.render(buffer);
		shader.disable();
		if (disableCulling)
			GL11.glEnable(GL11.GL_CULL_FACE);
	}
	
	public IShader getShader() {
		return shader;
	}
	
	public IVertexBuffer getBuffer() {
		return buffer;
	}

	public IMesh.Queue getQueue() {
		return queue;
	}

	public boolean hasFlag(IMesh.Flag flag) {
		return flags.contains(flag);
	}

	@Override
	public String toString() {
		return "renderable[queue=" + getQueue() + " shader=" + shader + " buffer=" + buffer + "]";
	}
}
