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



public final class LongList extends SimpleArrayList<long[], Long> {
	
	public LongList() {}
	
	public LongList(int size) {
		super(size);
	}

	@Override
	protected long[] alloc(int count) {
		return new long[count];
	}

	@Override
	protected long[] copyOf(Long[] array, int newSize) {
		long[] result = new long[newSize];
		int idx = 0;
		for(Long i : array)
			result[idx++] = i.longValue();
		return result;
	}

	@Override
	protected long[] copyOf(long[] original, int newLength) {
		return Arrays.copyOf(original, newLength);
	}
	
	@Override
	public void clear() {
		modCount++;
		size = 0;
	}

	public long get(int i) {
		if(i > size || i < 0) throw new NoSuchElementException(Integer.toString(i));
		return elementData[i];
	}
	
	public void add(final long e) {
		ensureCapacity(size + 1);  // Increments modCount!!
		elementData[size++] = e;
	}
	
	public boolean contains(long v) {
		for(int i = 0; i < size; i++)
			if(elementData[i] == v)
				return true;
		return false;
	}
	
	public long getFirst() {
		return get(0);
	}

	public long getLast() {
		return get(size - 1);
	}


	public void add(final int idx, long e) {
		ensureCapacity(size + 1);  // Increments modCount!!		
		System.arraycopy(elementData, idx, elementData, idx + 1, size - idx);
		size++;
		elementData[idx] = e;
	}

	public void addFirst(long e) {
		add(0, e);
	}
	
	
	public void set(int i, long v) {
		elementData[i] = v;
	}

	@Override
	protected int getComponentSize() {
		return 8;
	}
	
	@Override
	protected void load(DataInputStream in) throws IOException {
		try {
			for(int i = 0; i < elementData.length; i++)
				elementData[i] = in.readLong();
		} catch(EOFException e) {}
	}

	@Override
	protected void store(DataOutputStream out) throws IOException {
		for(int i = 0; i < size; i++)
			out.writeLong(elementData[i]);
	}

}
