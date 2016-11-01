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

package ch.fhnw.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;

public final class FloatList extends SimpleArrayList<float[], Float> {
	
	public FloatList() {
	}

	public FloatList(int initialCapacity) {
		super(initialCapacity);
	}

	public FloatList(FloatList src) {
		super(src);
	}

	@Override
	protected float[] alloc(int count) {
		return new float[count];
	}

	@Override
	protected float[] copyOf(Float[] array, int newSize) {
		float[] result = new float[newSize];
		int idx = 0;
		for(Float i : array)
			result[idx++] = i.floatValue();
		return result;
	}

	@Override
	protected float[] copyOf(float[] original, int newLength) {
		return Arrays.copyOf(original, newLength);
	}

	public float get(int i) {
		if(i > size || i < 0) throw new NoSuchElementException(Integer.toString(i));
		return elementData[i];
	}

	@Override
	public void clear() {
		modCount++;
		size = 0;
	}

	@Override
	public boolean addAll(float[] src) {
		int numNew = src.length;
		ensureCapacity(size + numNew);
		if(numNew < 16) {
			for(int i = 0; i < numNew; i++)
				elementData[size++] = src[i];
		} else {
			System.arraycopy(src, 0, elementData, size, numNew);
			size += numNew;
		}
		return numNew != 0;		
	}

	@Override
	public boolean addAll(float[] src, int off, int count) {
		int numNew = count;
		ensureCapacity(size + numNew);
		if(numNew < 16) {
			for(int i = 0; i < numNew; i++)
				elementData[size++] = src[off+i];
		} else {
			System.arraycopy(src, off, elementData, size, numNew);
			size += numNew;
		}
		return numNew != 0;		
	}

	public void add(final float e) {
		ensureCapacity(size + 1);  // Increments modCount!!
		elementData[size++] = e;
	}
	
	public void set(int i, float v) {
		elementData[i] = v;
	}

	public boolean contains(float v) {
		for(int i = 0; i < size; i++)
			if(elementData[i] == v)
				return true;
		return false;
	}
	
	public boolean contains(float v, float eps) {
		for(int i = 0; i < size; i++)
			if(Math.abs(elementData[i]-v)<eps)
				return true;
		return false;
	}
	
	public int find(float v, float eps) {
		for(int i = 0; i < size; i++)
			if(Math.abs(elementData[i]-v)<eps)
				return i;
		return -1;
	}

	public float getFirst() {
		return get(0);
	}

	public float getLast() {
		return get(size - 1);
	}

	public void add(final int idx, float e) {
		ensureCapacity(size + 1);  // Increments modCount!!		
		System.arraycopy(elementData, idx, elementData, idx + 1, size - idx);
		size++;
		elementData[idx] = e;
	}

	public void addFirst(float e) {
		add(0, e);
	}

	public void sort() {
		Arrays.sort(elementData, 0, size);
	}

	@Override
	protected int getComponentSize() {
		return 4;
	}
	
	@Override
	protected void load(DataInputStream in) throws IOException {
		try {
			for(int i = 0; i < elementData.length; i++)
				elementData[i] = in.readFloat();
		} catch(EOFException e) {}
	}

	@Override
	protected void store(DataOutputStream out) throws IOException {
		for(int i = 0; i < size; i++)
			out.writeFloat(elementData[i]);
	}

}
