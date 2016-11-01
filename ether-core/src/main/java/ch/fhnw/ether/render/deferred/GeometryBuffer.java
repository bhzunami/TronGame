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

package ch.fhnw.ether.render.deferred;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import ch.fhnw.ether.render.IRenderer.IRenderTargetState;
import ch.fhnw.ether.render.gl.FrameBuffer;
import ch.fhnw.ether.render.gl.GLObject;
import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.ether.render.gl.Texture;
import ch.fhnw.util.Log;
import ch.fhnw.util.Viewport;

public final class GeometryBuffer {
	private static final Log log = Log.create();

	private FrameBuffer frameBuffer;
	@SuppressWarnings("unused")
	private Texture positionTexture;
	@SuppressWarnings("unused")
	private Texture normalTexture;
	private Texture colorTexture;
	private Texture depthTexture;
	private int width;
	private int height;

	public GeometryBuffer() {

	}
	
	public void update(IRenderTargetState state) {
		if (frameBuffer == null)
			frameBuffer = new FrameBuffer();
		
		Viewport viewport = state.getViewCameraState().getViewport();
		if (viewport.w != width || viewport.h != height) {
			width = (int)viewport.w;
			height = (int)viewport.h;
			
			frameBuffer.bind();
			
			colorTexture = new Texture(new GLObject(Type.TEXTURE), width, height);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, (int)colorTexture.getGPUHandle());
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			frameBuffer.attach(GL30.GL_COLOR_ATTACHMENT0, colorTexture);
			
			depthTexture = new Texture(new GLObject(Type.TEXTURE), width, height);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, (int)depthTexture.getGPUHandle());
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT, (ByteBuffer)null);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			frameBuffer.attach(GL30.GL_DEPTH_ATTACHMENT, depthTexture);
			
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			
			GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);

			if(!frameBuffer.isComplete())
				log.severe("Status: " + FrameBuffer.toString(frameBuffer.getStatus()));
			
			FrameBuffer.unbind();
		}
	}

	public void enable() {
		frameBuffer.bind();
	}
	
	public void disable() {
		FrameBuffer.unbind();
	}
	
	public void blit() {
		int object = frameBuffer.getGLObject().getId();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, object);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}
}
