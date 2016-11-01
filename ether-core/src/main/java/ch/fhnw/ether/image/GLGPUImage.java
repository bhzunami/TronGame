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

package ch.fhnw.ether.image;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import ch.fhnw.ether.render.gl.GLContextManager;
import ch.fhnw.ether.render.gl.GLObject;
import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.ether.view.IWindow.IContext;

// TODO: add explicit dispose
public final class GLGPUImage extends AbstractImage implements IGPUImage {

	// NOTE: keep these in sync with ComponentType / ComponentFormat enum
	private static final int TYPE_MAP[] = {
		GL11.GL_UNSIGNED_BYTE,
		GL11.GL_FLOAT,
	};
	
	private static final int INTERNAL_FORMAT_MAP[][] = {
		{
			GL11.GL_RED,
			GL30.GL_RG,
			GL11.GL_RGB,
			GL11.GL_RGBA,
		},
		{
			GL30.GL_R16F,
			GL30.GL_RG16F,
			GL30.GL_RGB16F,
			GL30.GL_RGBA16F,
		}
	};
	
	private static final int FORMAT_MAP[] = {
			GL11.GL_RED,
			GL30.GL_RG,
			GL11.GL_RGB,
			GL11.GL_RGBA,			
	};
	
	private GLObject texture;
	
	public GLGPUImage(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode, ByteBuffer pixels) {
		super(width, height, componentType, componentFormat, alphaMode);
		try (IContext ctx = GLContextManager.acquireContext()) {
			texture = new GLObject(Type.TEXTURE);
			int target = GL11.GL_TEXTURE_2D;
			GL11.glBindTexture(target, texture.getId());
			if (pixels != null)
				pixels.rewind();
			load(pixels);
			GL30.glGenerateMipmap(target);
			GL11.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
			GL11.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
			GL11.glTexParameteri(target, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(target, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
			GL11.glBindTexture(target, 0);
			GL11.glFinish();
		} catch (Throwable t) {
			LOG.severe(t);
		}
	}
	
	@Override
	public long getGPUHandle() {
		return texture.getId();
	}
	
	@Override
	public IHostImage createHostImage() {
		try (IContext ctx = GLContextManager.acquireContext()) {
			int target = GL11.GL_TEXTURE_2D;
			int type = TYPE_MAP[getComponentType().ordinal()];
			int format = FORMAT_MAP[getComponentFormat().ordinal()];
			GL11.glBindTexture(target, texture.getId());
			ByteBuffer pixels = BufferUtils.createByteBuffer(getWidth() * getHeight() * getNumBytesPerPixel());
			GL11.glGetTexImage(target, 0, format, type, pixels);
			GL11.glBindTexture(target, 0);
			return IHostImage.create(getWidth(), getHeight(), getComponentType(), getComponentFormat(), getAlphaMode(), pixels);
		} catch (Throwable t) {
			LOG.severe(t);
			return null;
		}
	}

	private void load(ByteBuffer pixels) {
		int target = GL11.GL_TEXTURE_2D;
		int type = TYPE_MAP[getComponentType().ordinal()];
		int internalFormat = INTERNAL_FORMAT_MAP[getComponentType().ordinal()][getComponentFormat().ordinal()];
		int format = FORMAT_MAP[getComponentFormat().ordinal()];

		switch (getComponentFormat()) {
		case G:
			GL11.glTexParameteri(target, GL33.GL_TEXTURE_SWIZZLE_R, GL11.GL_RED);
			GL11.glTexParameteri(target, GL33.GL_TEXTURE_SWIZZLE_G, GL11.GL_RED);
			GL11.glTexParameteri(target, GL33.GL_TEXTURE_SWIZZLE_B, GL11.GL_RED);
			break;
		case GA:
			GL11.glTexParameteri(target, GL33.GL_TEXTURE_SWIZZLE_R, GL11.GL_RED);
			GL11.glTexParameteri(target, GL33.GL_TEXTURE_SWIZZLE_G, GL11.GL_RED);
			GL11.glTexParameteri(target, GL33.GL_TEXTURE_SWIZZLE_B, GL11.GL_RED);
			GL11.glTexParameteri(target, GL33.GL_TEXTURE_SWIZZLE_A, GL11.GL_GREEN);
			break;
		default:
		}
		GL11.glTexImage2D(target, 0, internalFormat, getWidth(), getHeight(), 0, format, type, pixels);
	}	
}
