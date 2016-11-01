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

import java.util.Arrays;
import java.util.BitSet;


/**
 * Fast set class for ints, loosely based java.util.HashTable, HashSet,
 * and the discussion of hashtables from "Data Structures & Algorithms
 * in Java" by Robert Lafore.
 *
 * @author Tom Ball
 * @author Simon Schubiger
 */
public final class CharSet {
	private static final int NUM_BITS = 4096;

	static class Entry {
		char value;
		int  hash;
		Entry next;

		Entry(char v, int h, Entry n) {
			value = v;
			hash = h;
			next = n;
		}
	}

	private Entry[] table;
	private int     size;
	private char[]  sorted;
	private BitSet  bitSet = new BitSet(NUM_BITS);

	private static final int LOAD_FACTOR   = 2;
	private static final int GROWTH_FACTOR = 2;

	public CharSet() {
		// 2048 is the max used by all of the JDK sources.
		// Use fewer only if the set is referenced longer than
		// the body of makeIndexes() above.
		this(NUM_BITS / 2);
	}

	public CharSet(int capacity) {
		table = new Entry[Math.max(128, capacity - NUM_BITS)];
	}

	public boolean add(char x) {
		if (contains(x))
			return false;

		sorted = null;
		if(x < NUM_BITS) {
			bitSet.set(x);
		} else {
			if (size > table.length / LOAD_FACTOR)
				resize();

			int h = hash(x);
			int idx = indexFor(h, table.length);
			Entry e = new Entry(x, h, table[idx]);
			table[idx] = e;
		}
		size++;
		return true;
	}

	public boolean contains(char x) {
		if(x < NUM_BITS)
			return bitSet.get(x);

		int h = hash(x);
		Entry e = table[indexFor(h, table.length)];
		while (e != null) {
			if (e.value == x)
				return true;
			e = e.next;
		}
		return false;
	}

	public char[] toArray() {
		char[] result = new char[size];
		int n = 0;
		for(int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1))
			result[n++] = (char)i;
		for (int i = 0; i < table.length; i++)
			for(Entry e = table[i]; e != null; e = e.next)
				result[n++] = e.value;
		assert n == size;
		return result;
	}

	private void resize() {
		Entry[] newt = new Entry[table.length * GROWTH_FACTOR];

		Entry eNext;
		for(int i = 0; i < table.length; i++)
			for(Entry e = table[i]; e != null; e = eNext) {
				int idx = indexFor(e.hash, newt.length);
				eNext = e.next;
				e.next = newt[idx];
				newt[idx] = e;
			}
		table = newt;
	}

	// hash(), forIndex() from java.util.HashMap

	/**
	 * Returns a hash value for the specified object.  In addition to 
	 * the object's own hashCode, this method applies a "supplemental
	 * hash function," which defends against poor quality hash functions.
	 * This is critical because HashMap uses power-of two length 
	 * hash tables.
	 * 
	 * The shift distances in this function were chosen as the result 
	 * of an automated search over the entire four-dimensional search space. */
	private static int hash(char h) {
		h += ~(h << 9);
		h ^= (h >>> 14);
		h += (h << 4);
		h ^= (h >>> 10);
		return h; 
	}
	/** 
	 * Returns index for hash code h. 
	 */ 
	private static int indexFor(int h, int length) { 
		return h & (length-1);
	}

	public int size() {
		return size;
	}

	public char[] sorted() {
		if(sorted == null) {
			sorted = toArray();
			Arrays.sort(toArray());
		}
		return sorted;
	}
} 
