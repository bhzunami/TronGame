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

package ch.fhnw.ether.render.forward;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

import ch.fhnw.ether.render.Renderable;
import ch.fhnw.ether.render.ShaderBuilder;
import ch.fhnw.ether.render.shader.builtin.ShadowVolumeShader;
import ch.fhnw.ether.render.shader.builtin.TrivialDeviceSpaceShader;
import ch.fhnw.ether.scene.attribute.IAttribute;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.material.EmptyMaterial;
import ch.fhnw.util.color.RGBA;

public final class ShadowVolumes {
	private static final IMesh OVERLAY_MESH = new DefaultMesh(Primitive.TRIANGLES, new EmptyMaterial(), DefaultGeometry.createV(MeshUtilities.UNIT_QUAD_TRIANGLES));

	private ShadowVolumeShader volumeShader;
	private Renderable overlay;

	private int lightIndex;
	private float extrudeDistance = 1000;
	private RGBA volumeColor = new RGBA(1, 0, 0, 0.2f);
	private RGBA overlayColor = new RGBA(0, 0, 0, 0.9f);

	public ShadowVolumes(Map<IAttribute, Supplier<?>> globals) {
		volumeShader = ShaderBuilder.create(new ShadowVolumeShader(() -> lightIndex, () -> extrudeDistance, () -> volumeColor), null, globals);
		overlay = new Renderable(new TrivialDeviceSpaceShader(() -> overlayColor), OVERLAY_MESH, globals);
	}

	// http://ogldev.atspace.co.uk/www/tutorial40/tutorial40.html
	public void render(IMesh.Queue pass, List<Renderable> renderables, int numLights) {
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_ALPHA);
		GL11.glDepthMask(false);
		GL11.glEnable(GL32.GL_DEPTH_CLAMP);
		
		overlay.update(OVERLAY_MESH.getMaterial().getData(), OVERLAY_MESH.getTransformedGeometryData());

		for (lightIndex = 0; lightIndex < numLights; ++lightIndex) {
			GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

			GL11.glColorMask(false, false, false, false);

			GL11.glEnable(GL11.GL_STENCIL_TEST);

			GL20.glStencilFuncSeparate(GL11.GL_FRONT, GL11.GL_ALWAYS, 0, 0xffffffff);
			GL20.glStencilOpSeparate(GL11.GL_FRONT, GL11.GL_KEEP, GL14.GL_DECR_WRAP, GL11.GL_KEEP);

			GL20.glStencilFuncSeparate(GL11.GL_BACK, GL11.GL_ALWAYS, 0, 0xffffffff);
			GL20.glStencilOpSeparate(GL11.GL_BACK, GL11.GL_KEEP, GL14.GL_INCR_WRAP, GL11.GL_KEEP);

			volumeShader.update(null);
			volumeShader.enable();
			for (Renderable renderable : renderables) {
				if (renderable.hasFlag(Flag.DONT_CAST_SHADOW))
					continue;
				if (renderable.getQueue() != pass)
					continue;

				volumeShader.render(renderable.getBuffer());
			}
			volumeShader.disable();

			GL11.glColorMask(true, true, true, true);

			GL11.glStencilFunc(GL11.GL_NOTEQUAL, 0x0, 0xffffffff);
			GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

			overlay.render();

			GL11.glDisable(GL11.GL_STENCIL_TEST);
		}
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(true);
		GL11.glDisable(GL32.GL_DEPTH_CLAMP);
	}
}
