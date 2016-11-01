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
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.RandomAccess;

public abstract class SimpleArrayList<AT, WT> implements RandomAccess, Cloneable, Iterable<WT> {
	/**
	 * The array buffer into which the elements of the ArrayList are stored. The
	 * capacity of the ArrayList is the length of this array buffer.
	 */
	protected AT elementData;

	/**
	 * The size of the ArrayList (the number of elements it contains).
	 */
	protected int size;
	protected int modCount = 0;

	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param initialCapacity
	 *            the initial capacity of the list
	 * @throws IllegalArgumentException
	 *             if the specified initial capacity is negative
	 */
	public SimpleArrayList(int initialCapacity) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		this.elementData = alloc(initialCapacity);
	}

	protected abstract AT alloc(int count);

	/**
	 * Constructs an empty list with an initial capacity of ten.
	 */
	public SimpleArrayList() {
		this(10);
	}

	/**
	 * Constructs a non-initialized version for sub-class initialization.
	 */
	public SimpleArrayList(boolean unused) {
	}

	/**
	 * Constructs a list containing the elements of the specified collection, in
	 * the order they are returned by the collection's iterator.
	 *
	 * @param c
	 *            the collection whose elements are to be placed into this list
	 * @throws NullPointerException
	 *             if the specified collection is null
	 */
	@SuppressWarnings("unchecked")
	public SimpleArrayList(Collection<? extends WT> c) {
		elementData = copyOf((WT[]) c.toArray(), c.size());
		size = c.size();
	}

	public SimpleArrayList(SimpleArrayList<AT, WT> al) {
		elementData = copyOf(al.elementData, al.size);
		size = al.size();
	}

	protected abstract AT copyOf(WT[] original, int newLength);

	protected abstract AT copyOf(AT original, int newLength);

	/**
	 * Trims the capacity of this <tt>ArrayList</tt> instance to be the list's
	 * current size. An application can use this operation to minimize the
	 * storage of an <tt>ArrayList</tt> instance.
	 */
	public final void trimToSize() {
		modCount++;
		int oldCapacity = Array.getLength(elementData);
		if (size < oldCapacity)
			grow(size);
	}

	/**
	 * Increases the capacity of this <tt>ArrayList</tt> instance, if necessary,
	 * to ensure that it can hold at least the number of elements specified by
	 * the minimum capacity argument.
	 *
	 * @param minCapacity
	 *            the desired minimum capacity
	 */
	public void ensureCapacity(int minCapacity) {
		modCount++;
		int oldCapacity = Array.getLength(elementData);
		if (minCapacity > oldCapacity) {
			int newCapacity = (oldCapacity * 3) / 2 + 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			grow(newCapacity);
		}
	}

	private void grow(int newSize) {
		elementData = copyOf(elementData, newSize);
	}

	protected abstract int getComponentSize();

	protected abstract void load(DataInputStream in) throws IOException;

	protected abstract void store(DataOutputStream out) throws IOException;

	public void ensureSize(int minCapacity) {
		ensureCapacity(minCapacity);
		size = minCapacity;
	}

	/**
	 * Returns the number of elements in this list.
	 *
	 * @return the number of elements in this list
	 */
	public final int size() {
		return size;
	}

	/**
	 * Returns <tt>true</tt> if this list contains no elements.
	 *
	 * @return <tt>true</tt> if this list contains no elements
	 */
	public final boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns a shallow copy of this <tt>ArrayList</tt> instance. (The elements
	 * themselves are not copied.)
	 *
	 * @return a clone of this <tt>ArrayList</tt> instance
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		try {
			SimpleArrayList<AT, WT> v = (SimpleArrayList<AT, WT>) super.clone();
			v.elementData = copyOf(elementData, size);
			v.modCount = 0;
			return v;
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}
	}

	/**
	 * Public method to remove an element.
	 * 
	 * @param i
	 *            Index of element to remove.
	 */
	public final void remove(int i) {
		if (i < 0 || i >= size)
			throw new Error("Invalid range (" + i + "/" + size + "); cannot remove");
		fastRemove(i);
		--size;
	}

	public void removeLast() {
		if (size == 0)
			throw new Error("List is empty; cannot removeLast");
		size--;
	}

	/*
	 * Private remove method that skips bounds checking and does not return the
	 * value removed.
	 */
	protected final void fastRemove(int index) {
		modCount++;
		int numMoved = size - index - 1;
		if (numMoved > 0)
			System.arraycopy(elementData, index + 1, elementData, index, numMoved);
	}

	/**
	 * Removes all of the elements from this list. The list will be empty after
	 * this call returns.
	 */
	public void clear() {
		elementData = alloc(Array.getLength(elementData));
		modCount++;
		size = 0;
	}

	/**
	 * Appends all of the elements in the specified collection to the end of
	 * this list, in the order that they are returned by the specified
	 * collection's Iterator. The behavior of this operation is undefined if the
	 * specified collection is modified while the operation is in progress.
	 * (This implies that the behavior of this call is undefined if the
	 * specified collection is this list, and this list is nonempty.)
	 *
	 * @param c
	 *            collection containing elements to be added to this list
	 * @return <tt>true</tt> if this list changed as a result of the call
	 * @throws NullPointerException
	 *             if the specified collection is null
	 */
	public final boolean addAll(Collection<? extends WT> c) {
		Object[] a = c.toArray();
		int numNew = a.length;
		ensureCapacity(size + numNew); // Increments modCount
		System.arraycopy(a, 0, elementData, size, numNew);
		size += numNew;
		return numNew != 0;
	}

	public final boolean addAll(SimpleArrayList<AT, WT> src) {
		ensureCapacity(size + src.size());
		int numNew = src.size();
		System.arraycopy(src.elementData, 0, elementData, size, numNew);
		size += numNew;
		return numNew != 0;
	}

	public boolean addAll(AT src) {
		int numNew = Array.getLength(src);
		ensureCapacity(size + numNew);
		System.arraycopy(src, 0, elementData, size, numNew);
		size += numNew;
		return numNew != 0;
	}

	public boolean addAll(AT src, int offset, int len) {
		ensureCapacity(size + len);
		System.arraycopy(src, offset, elementData, size, len);
		size += len;
		return len != 0;
	}

	/**
	 * Inserts all of the elements in the specified collection into this list,
	 * starting at the specified position. Shifts the element currently at that
	 * position (if any) and any subsequent elements to the right (increases
	 * their indices). The new elements will appear in the list in the order
	 * that they are returned by the specified collection's iterator.
	 *
	 * @param index
	 *            index at which to insert the first element from the specified
	 *            collection
	 * @param c
	 *            collection containing elements to be added to this list
	 * @return <tt>true</tt> if this list changed as a result of the call
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified collection is null
	 */
	@SuppressWarnings("unchecked")
	public final boolean addAll(int index, Collection<? extends WT> c) {
		if (index > size || index < 0)
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);

		AT a = copyOf((WT[]) c.toArray(), c.size());
		int numNew = c.size();
		ensureCapacity(size + numNew); // Increments modCount

		int numMoved = size - index;
		if (numMoved > 0)
			System.arraycopy(elementData, index, elementData, index + numNew, numMoved);

		System.arraycopy(a, 0, elementData, index, numNew);
		size += numNew;
		return numNew != 0;
	}

	public final AT toArray() {
		return copyOf(elementData, size);
	}

	/**
	 * Get direct access to the underlying array. Good for fast access but
	 * somewhat hacky...
	 * 
	 * @returns the reference to the underlying array.
	 */
	public final AT _getArray() {
		return elementData;
	}

	/**
	 * Get direct access to the underlying size. Good for fast access but
	 * somewhat hacky...
	 */
	public final void _setSize(int size) {
		this.size = size;
	}

	/**
	 * Removes from this list all of the elements whose index is between
	 * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive. Shifts
	 * any succeeding elements to the left (reduces their index). This call
	 * shortens the list by <tt>(toIndex - fromIndex)</tt> elements. (If
	 * <tt>toIndex==fromIndex</tt>, this operation has no effect.)
	 *
	 * @param fromIndex
	 *            index of first element to be removed
	 * @param toIndex
	 *            index after last element to be removed
	 * @throws IndexOutOfBoundsException
	 *             if fromIndex or toIndex out of range (fromIndex &lt; 0 ||
	 *             fromIndex &gt;= size() || toIndex &gt; size() || toIndex &lt;
	 *             fromIndex)
	 */
	protected final void removeRange(int fromIndex, int toIndex) {
		modCount++;
		int numMoved = size - toIndex;
		System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);
	}

	@Override
	public String toString() {
		return TextUtilities.toString("[", ", ", "]", this, TextUtilities.QUOTE_STRINGS, 0, size);
	}

	public final int getCapacity() {
		return Array.getLength(elementData);
	}

	@Override
	public Iterator<WT> iterator() {
		return new Iterator<WT>() {
			private int pos;

			@Override
			public boolean hasNext() {
				return pos < size;
			}

			@SuppressWarnings("unchecked")
			@Override
			public WT next() {
				return (WT) Array.get(elementData, pos++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public int getSize() {
		return size;
	}
}
