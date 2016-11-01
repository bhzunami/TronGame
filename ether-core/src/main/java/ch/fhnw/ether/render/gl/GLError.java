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

package ch.fhnw.ether.render.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import ch.fhnw.util.Log;

/**
 * Basic OpenGL error checking.
 *
 * @author radar
 */
public final class GLError {
	private static final Log LOG = Log.create();

	private GLError() {
	}
	
	public static boolean check() {
		return GL11.glGetError() != GL11.GL_NO_ERROR;
	}

	public static boolean checkWithMessage(String message) {
		int error = GL11.glGetError();
		if (error == GL11.GL_NO_ERROR)
			return false;
		
		LOG.severe("gl error: " + message + ": " + getErrorString(error));
		return true;
	}
	
	public static void checkWithException(String message) {
		int error = GL11.glGetError();
		if (error == GL11.GL_NO_ERROR)
			return;
		throw new RuntimeException("gl error: " + message + ": " + getErrorString(error));
	}
	
	public static String getErrorString(int error) {
		switch (error) {
		case GL11.GL_NO_ERROR:
			return "GL_NO_ERROR";
		case GL11.GL_INVALID_ENUM:
			return "GL_INVALID_ENUM";
		case GL11.GL_INVALID_VALUE:
			return "GL_INVALID_VALUE";
		case GL11.GL_INVALID_OPERATION:
			return "GL_INVALID_OPERATION";
		case GL30.GL_INVALID_FRAMEBUFFER_OPERATION:
			return "GL_INVALID_FRAMEBUFFER_OPERATION";
		case GL11.GL_OUT_OF_MEMORY:
			return "GL_OUT_OF_MEMORY";
		case GL11.GL_STACK_UNDERFLOW:
			return "GL_STACK_UNDERFLOW";
		case GL11.GL_STACK_OVERFLOW:
			return ": GL_STACK_OVERFLOW";
		default:
			return "UNKNOWN ERROR 0x" + Integer.toHexString(error);
		}
	}
}
