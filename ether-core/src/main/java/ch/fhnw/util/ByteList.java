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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.jcodec.common.IOUtils;



public final class ByteList extends SimpleArrayList<byte[], Byte> {

	public ByteList() {
	}

	public ByteList(int initialCapacity) {
		super(initialCapacity);
	}

	public ByteList(ByteList source) {
		elementData = source._getArray().clone();
		size        = source.size;
		modCount++;
	}

	public ByteList(byte[] data) {
		elementData = data.clone();
		size        = data.length;
		modCount++;
	}

	@Override
	protected byte[] alloc(int count) {
		return new byte[count];
	}

	@Override
	protected byte[] copyOf(Byte[] array, int newSize) {
		byte[] result = new byte[newSize];
		int idx = 0;
		for(Byte i : array)
			result[idx++] = i.byteValue();
		return result;
	}

	@Override
	protected byte[] copyOf(byte[] original, int newLength) {
		return Arrays.copyOf(original, newLength);
	}

	public final void addAllIfUnique(ByteList l) {
		for (int i=0; i<l.size(); i++)
		{
			addIfUnique(l.get(i));
		}
	}

	public void add(final byte e) {
		ensureCapacity(size + 1);  // Increments modCount!!
		elementData[size++] = e;
	}

	public void add(final byte[] es) {
		ensureCapacity(size + es.length);  // Increments modCount!!
		System.arraycopy(es, 0, elementData, size, es.length);
		size += es.length;
	}

	public void addIfUnique(final byte e) {
		if (contains(e))
			return;
		ensureCapacity(size + 1);  // Increments modCount!!
		elementData[size++] = e;
	}

	public void add(final int idx, final byte e) {
		ensureCapacity(size + 1);  // Increments modCount!!		
		System.arraycopy(elementData, idx, elementData, idx + 1, size - idx);
		size++;
		elementData[idx] = e;
	}

	public byte get(int i) {
		if(i > size || i < 0) throw new NoSuchElementException(Integer.toString(i));
		return elementData[i];
	}

	@Override
	public ByteList clone() {
		return (ByteList)super.clone();
	}

	@Override
	public void clear() {
		modCount++;
		size = 0;
	}

	public void set(int i, byte v) {
		elementData[i] = v;
	}

	public boolean contains(byte v) {
		for(int i = 0; i < size; i++)
			if(elementData[i] == v)
				return true;
		return false;
	}

	public int indexOf( byte val )
	{
		for( int i = 0; i < size; i++ )
			if( elementData[ i ] == val )
				return i;
		return -1;
	}

	public byte getFirst() {
		return get(0);
	}

	public byte getLast() {
		return get(size - 1);
	}

	public void addFirst(byte e) {
		add(0, e);
	}

	public void sort() {
		Arrays.sort(elementData, 0, size);
	}

	@Override
	protected int getComponentSize() {
		return 1;
	}

	@Override
	protected void load(DataInputStream in) throws IOException {
		for(int i = 0; i < elementData.length; i++) {
			int val = in.read();
			if(val < 0) return;
			elementData[i] = (byte) val;
		}
	}

	@Override
	protected void store(DataOutputStream out) throws IOException {
		out.write(elementData);
	}

	public void readFully(InputStream in) throws IOException {
		addAll(IOUtils.toByteArray(in));
		in.close();
	}
}
