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

import java.util.IdentityHashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import ch.fhnw.ether.render.AbstractRenderer;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.view.IView;

/*
 * General flow:
 * - foreach viewport
 * -- only use geometry assigned to this viewport
 * 
 * - foreach pass/queue
 * -- setup opengl params specific to pass
 * 
 * - foreach material
 * -- enable shader
 * -- write uniforms
 * 
 * - foreach material instance (texture set + uniforms)
 * -- setup texture
 * -- write uniforms
 * -- refresh buffers
 * 
 * - foreach buffer (assembled objects)
 * -- setup buffer
 * -- draw
 */

/**
 * Simple and straightforward forward renderer.
 *
 * @author radar
 */
public final class ForwardRenderer extends AbstractRenderer {

	private final Map<IView, PostBuffer> postBuffers = new IdentityHashMap<>();
	
	public ForwardRenderer() {
	}

	public ForwardRenderer(boolean startRenderThread) {
		super(startRenderThread);
	}

	@SuppressWarnings("unused")
	@Override
	protected void render(IRenderTargetState state) {
		
		//---- PREPARE POST QUEUE IF NECESSARY
		PostBuffer postBuffer = null;
		if (state.hasPost()) {
			postBuffer = postBuffers.get(state.getView());
			if (postBuffer == null) {
				postBuffer = new PostBuffer();
				postBuffers.put(state.getView(), postBuffer);
			}
			postBuffer.update(state);
			postBuffer.bind();
			postBuffer.clear();
		}

		//---- DEPTH QUEUE (DEPTH WRITE&TEST ENABLED, BLEND OFF)
		globals.viewInfo.setCameraSpace();
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
		GL11.glPolygonOffset(1, 3);
		renderObjects(state, Queue.DEPTH);
		GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

		if (false)
			renderShadowVolumes(state, Queue.DEPTH);

		//---- TRANSPARENCY QUEUE (DEPTH WRITE DISABLED, DEPTH TEST ENABLED, BLEND ON)
		globals.viewInfo.setCameraSpace();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDepthMask(false);
		renderObjects(state, Queue.TRANSPARENCY);
		
		//---- POST QUEUE (IF ANY POST RENDERABLES)
		if (state.hasPost()) {
			globals.viewInfo.setOrthoDeviceSpace();
			postBuffer.unbind();
			renderPostObjects(state, Queue.POST, postBuffer.getColorMap(), postBuffer.getDepthMap());
		}

		//---- OVERLAY QUEUE (DEPTH WRITE&TEST DISABLED, BLEND ON)
		globals.viewInfo.setCameraSpace();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		renderObjects(state, Queue.OVERLAY);

		//---- DEVICE SPACE OVERLAY QUEUE (DEPTH WRITE&TEST DISABLED, BLEND ON)
		globals.viewInfo.setOrthoDeviceSpace();
		renderObjects(state, Queue.DEVICE_SPACE_OVERLAY);

		//---- SCREEN SPACE OVERLAY QUEUE(DEPTH WRITE&TEST DISABLED, BLEND ON)
		globals.viewInfo.setOrthoScreenSpace();
		renderObjects(state, Queue.SCREEN_SPACE_OVERLAY);

		//---- CLEANUP: RETURN TO DEFAULTS
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(true);
	}
}
