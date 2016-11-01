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



public final class CharList extends SimpleArrayList<char[], Character> {

	public CharList() {
	}

	public CharList(int initialCapacity) {
		super(initialCapacity);
	}

	public CharList(CharList source) {
		elementData = source._getArray().clone();
		size        = source.size;
		modCount++;
	}

	@Override
	protected char[] alloc(int count) {
		return new char[count];
	}

	@Override
	protected char[] copyOf(Character[] array, int newSize) {
		char[] result = new char[newSize];
		int idx = 0;
		for(Character c : array)
			result[idx++] = c.charValue();
		return result;
	}

	@Override
	protected char[] copyOf(char[] original, int newLength) {
		return Arrays.copyOf(original, newLength);
	}

	public final void addAllIfUnique(CharList l) {
		for (int i=0; i<l.size(); i++)
		{
			addIfUnique(l.get(i));
		}
	}

	public void add(final char e) {
		ensureCapacity(size + 1);  // Increments modCount!!
		elementData[size++] = e;
	}

	public void add(final String s) {
		ensureCapacity(size + s.length());  // Increments modCount!!
		s.getChars(0, s.length(), elementData, size);
		size += s.length();
	}

	public void addIfUnique(final char e) {
		if (contains(e))
			return;
		ensureCapacity(size + 1);  // Increments modCount!!
		elementData[size++] = e;
	}

	public void add(final int idx, final char e) {
		ensureCapacity(size + 1);  // Increments modCount!!		
		System.arraycopy(elementData, idx, elementData, idx + 1, size - idx);
		size++;
		elementData[idx] = e;
	}

	public char get(int i) {
		if(i > size || i < 0) throw new NoSuchElementException(Integer.toString(i));
		return elementData[i];
	}

	@Override
	public CharList clone() {
		return (CharList)super.clone();
	}

	@Override
	public void clear() {
		modCount++;
		size = 0;
	}

	public void set(int i, char v) {
		elementData[i] = v;
	}

	public boolean contains(char v) {
		for(int i = 0; i < size; i++)
			if(elementData[i] == v)
				return true;
		return false;
	}

	public int indexOf(char val) {
		for( int i = 0; i < size; i++ )
			if( elementData[ i ] == val )
				return i;
		return -1;
	}

	public char getFirst() {
		return get(0);
	}

	public char getLast() {
		return get(size - 1);
	}


	public void addFirst(char e) {
		add(0, e);
	}

	public void sort() {
		Arrays.sort(elementData, 0, size);
	}

	@Override
	public String toString() {
		return new String(elementData, 0, size);
	}

	@Override
	protected int getComponentSize() {
		return 2;
	}

	@Override
	protected void load(DataInputStream in) throws IOException {
		try {
			for(int i = 0; i < elementData.length; i++)
				elementData[i] = in.readChar();
		} catch(EOFException e) {}
	}

	@Override
	protected void store(DataOutputStream out) throws IOException {
		for(int i = 0; i < size; i++)
			out.writeChar(elementData[i]);
	}

}
