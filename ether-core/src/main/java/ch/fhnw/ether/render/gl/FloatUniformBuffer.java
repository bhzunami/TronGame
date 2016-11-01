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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import ch.fhnw.ether.render.gl.GLObject.Type;

/**
 * Basic uniform buffer object wrapper.
 *
 * @author radar
 */

// TODO: switch to arrays instead of using float buffer (or offer both methods)?

public final class FloatUniformBuffer {
	private static final AtomicInteger BINDING_POINT_COUNTER = new AtomicInteger();

	private static int blockAlignment;

	private final int blockSize;
	private final int blockCount;
	private final int bindingPoint;

	private FloatBuffer buffer;
	private FloatBuffer block;

	private GLObject ubo;

	public FloatUniformBuffer(int blockSize) {
		this(blockSize, 1);
	}

	public FloatUniformBuffer(int blockSize, int blockCount) {
		this(blockSize, blockCount, getNewBindingPoint());
	}

	public FloatUniformBuffer(int blockSize, int blockCount, int bindingPoint) {
		this.blockSize = blockSize;
		this.blockCount = blockCount;
		this.bindingPoint = bindingPoint;
	}

	public void load(BiConsumer<Integer, FloatBuffer> consumer) {
		int stride = stride();

		if (ubo == null)
			ubo = new GLObject(Type.BUFFER);
		
		if (buffer == null)
			buffer = BufferUtils.createFloatBuffer(size(stride));

		buffer.rewind();
		for (int i = 0; i < blockCount; ++i) {
			buffer.position(i * stride);
			consumer.accept(i, buffer);
		}
		buffer.rewind();
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, ubo.getId());
		GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, buffer, GL15.GL_STREAM_DRAW);
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
	}

	public void load(int blockIndex, Consumer<FloatBuffer> consumer) {
		int stride = stride();

		if (ubo == null) {
			ubo = new GLObject(Type.BUFFER);
			GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, ubo.getId());
			GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, size(stride) * 4, GL15.GL_STREAM_DRAW);
			GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
		}

		if (block == null)
			block = BufferUtils.createFloatBuffer(blockSize);
		
		block.rewind();
		consumer.accept(block);
		block.rewind();
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, ubo.getId());
		GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, blockIndex * stride * 4, block);
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
	}

	public void bind() {
		GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, bindingPoint, ubo.getId());
	}

	public void bind(int blockIndex) {
		GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, bindingPoint, ubo.getId(), blockIndex * stride() * 4, blockSize * 4);
	}

	public int getBindingPoint() {
		return bindingPoint;
	}

	public static int getNewBindingPoint() {
		return BINDING_POINT_COUNTER.getAndIncrement();
	}

	private int stride() {
		if (blockAlignment == 0) {
			blockAlignment = Math.max(1, GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT) / 4);
		}
		int stride = (blockSize + blockAlignment - 1) - (blockSize + blockAlignment - 1) % blockAlignment;
		//System.out.println("block size: " + blockSize + " blockAlignment: " + blockAlignment + " stride: " + stride);
		return stride;
	}

	private int size(int stride) {
		return (blockCount - 1) * stride + blockSize;
	}
}
