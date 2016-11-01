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

public final class DoubleList extends SimpleArrayList<double[], Double> {

	public DoubleList() {
	}

	public DoubleList(int initialCapacity) {
		super(initialCapacity);
	}

	public DoubleList(DoubleList src) {
		super(src);
	}

	@Override
	protected double[] alloc(int count) {
		return new double[count];
	}

	@Override
	protected double[] copyOf(Double[] array, int newSize) {
		double[] result = new double[newSize];
		int idx = 0;
		for(Double i : array)
			result[idx++] = i.doubleValue();
		return result;
	}

	@Override
	protected double[] copyOf(double[] original, int newLength) {
		return Arrays.copyOf(original, newLength);
	}

	public double get(int i) {
		if(i > size || i < 0) throw new NoSuchElementException(Integer.toString(i));
		return elementData[i];
	}

	@Override
	public void clear() {
		modCount++;
		size = 0;
	}

	@Override
	public boolean addAll(double[] src) {
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

	public void add(final double e) {
		ensureCapacity(size + 1);  // Increments modCount!!
		elementData[size++] = e;
	}

	public void set(int i, double v) {
		elementData[i] = v;
	}

	public boolean contains(double v) {
		for(int i = 0; i < size; i++)
			if(elementData[i] == v)
				return true;
		return false;
	}

	public double getFirst() {
		return get(0);
	}

	public double getLast() {
		return get(size - 1);
	}


	public void add(final int idx, double e) {
		ensureCapacity(size + 1);  // Increments modCount!!		
		System.arraycopy(elementData, idx, elementData, idx + 1, size - idx);
		size++;
		elementData[idx] = e;
	}
	
	public void addFirst(double e) {
		add(0, e);
	}

	@Override
	protected int getComponentSize() {
		return 8;
	}
	
	@Override
	protected void load(DataInputStream in) throws IOException {
		try {
			for(int i = 0; i < elementData.length; i++)
				elementData[i] = in.readDouble();
		} catch(EOFException e) {}
	}

	@Override
	protected void store(DataOutputStream out) throws IOException {
		for(int i = 0; i < size; i++)
			out.writeDouble(elementData[i]);
	}
}
