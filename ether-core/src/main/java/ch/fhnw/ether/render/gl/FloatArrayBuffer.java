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

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL15;

import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.util.BufferUtilities;

/**
 * Basic float buffer attribute wrapper.
 *
 * @author radar
 */

// TODO: switch to arrays instead of using float buffer (or offer both methods)?

public final class FloatArrayBuffer implements IArrayBuffer {
	private GLObject vbo;
	private int size;

	public FloatArrayBuffer() {
	}

	public void load(FloatBuffer data) {
		if (vbo == null)
			vbo = new GLObject(Type.BUFFER);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo.getId());
		if (data != null && data.limit() != 0) {
			size = data.limit();
			data.rewind();
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
		} else {
			size = 0;
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, BufferUtilities.EMPTY_FLOAT_BUFFER, GL15.GL_STATIC_DRAW);
		}
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}

	@Override
	public void clear() {
		load(null);
	}

	@Override
	public void bind() {
		if (size > 0)
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo.getId());
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}
}
