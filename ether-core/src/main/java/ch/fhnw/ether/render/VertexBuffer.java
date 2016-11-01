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

import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import ch.fhnw.ether.render.gl.FloatArrayBuffer;
import ch.fhnw.ether.render.gl.IArrayBuffer;
import ch.fhnw.ether.render.shader.IShader;
import ch.fhnw.ether.render.variable.IShaderArray;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry.IGeometryAttribute;

// TODO: deal with max vbo size & multiple vbos, memory optimization, handle non-float arrays, indexed buffers

public final class VertexBuffer implements IVertexBuffer {
	private static final ThreadLocal<FloatBuffer> TARGET = 
			ThreadLocal.withInitial(() -> BufferUtils.createFloatBuffer(1024 * 1024));

	private final FloatArrayBuffer buffer = new FloatArrayBuffer();

	private final int stride;
	private final int[] sizes;
	private final int[] offsets;
	private final int[] attributeIndices;

	public VertexBuffer(IShader shader, IGeometryAttribute[] attributes) {
		List<IShaderArray<?>> arrays = shader.getArrays();
		if (arrays.isEmpty())
			throw new IllegalArgumentException("shader " + shader + " does not define any vertex arrays");

		sizes = new int[arrays.size()];
		offsets = new int[arrays.size()];
		attributeIndices = new int[arrays.size()];

		int stride = 0;
		int bufferIndex = 0;
		for (IShaderArray<?> array : arrays) {
			int attributeIndex = 0;
			for (IGeometryAttribute attribute : attributes) {
				if (array.id().equals(attribute.id())) {
					int size = attribute.getNumComponents();
					sizes[bufferIndex] = size;
					offsets[bufferIndex] = stride;
					attributeIndices[bufferIndex] = attributeIndex;
					array.setBufferIndex(bufferIndex);
					bufferIndex++;
					stride += size;
					break;
				}
				attributeIndex++;
			}
			if (attributeIndex == attributes.length)
				throw new IllegalArgumentException("shader " + shader + " requires attribute " + array.id());
		}
		this.stride = stride;
	}

	public void update(float[][] data) {
		float[][] sources = new float[attributeIndices.length][];

		int size = 0;
		for (int attributeIndex = 0; attributeIndex < attributeIndices.length; ++attributeIndex) {
			float[] source = data[attributeIndices[attributeIndex]];
			sources[attributeIndex] = source;
			size += source.length;
		}
		FloatBuffer buffer = TARGET.get();
		if (buffer.capacity() < size) {
			buffer = BufferUtils.createFloatBuffer(2 * size);
			TARGET.set(buffer);
		}
		buffer.clear();
		buffer.limit(size);
		interleave(buffer, sources, sizes);
		this.buffer.load(buffer);
	}
	
	@Override
	public int getNumVertices() {
		return buffer.size() / stride;
	}

	@Override
	public void bind() {
		buffer.bind();
	}

	@Override
	public void unbind() {
		IArrayBuffer.unbind();
	}

	@Override
	public void enableAttribute(int bufferIndex, int shaderIndex) {
		if (!buffer.isEmpty()) {
			GL20.glEnableVertexAttribArray(shaderIndex);
			GL20.glVertexAttribPointer(shaderIndex, sizes[bufferIndex], GL11.GL_FLOAT, false, stride * 4, offsets[bufferIndex] * 4);
		}
	}

	@Override
	public void disableAttribute(int bufferIndex, int shaderIndex) {
		if (!buffer.isEmpty())
			GL20.glDisableVertexAttribArray(shaderIndex);
	}

	@Override
	public String toString() {
		return buffer.size() + " " + stride;
	}

	private static void interleave(FloatBuffer target, float[][] data, int[] sizes) {
		for (int i = 0; i < data[0].length / sizes[0]; ++i) {
			for (int j = 0; j < data.length; ++j) {
				int k = (i * sizes[j]) % data[j].length;
				target.put(data[j], k, sizes[j]);
			}
		}
	}
}
