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

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

public final class ArrayUtilities {

	@SuppressWarnings("unchecked")
	static <T> T[] alloc1D(T[] template, int size) {
		return (T[])Array.newInstance(template.getClass().getComponentType(), size);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] alloc1D(T template, int size) {
		return (T[])Array.newInstance(template.getClass(), size);
	}

	public static <T> T[] append(T[] array, T elem) {
		array = Arrays.copyOf(array, array.length + 1);
		array[array.length - 1] = elem;
		return array;
	}

	public static int[] prepend(int elem, int[] array) {
		int[] result = new int[array.length + 1];
		System.arraycopy(array, 0, result, 1, array.length);
		result[0] = elem;
		return result;
	}

	public static double[] prepend(double elem, double[] array) {
		double[] result = new double[array.length + 1];
		System.arraycopy(array, 0, result, 1, array.length);
		result[0] = elem;
		return result;
	}

	public static <T> T[] prepend(T elem, T[] array) {
		T[] result = alloc1D(array, array.length + 1);
		System.arraycopy(array, 0, result, 1, array.length);
		result[0] = elem;
		return result;
	}

	public static double[] append(double[] array, double elem) {
		array = Arrays.copyOf(array, array.length + 1);
		array[array.length - 1] = elem;
		return array;
	}

	public static float[] append(float[] array, float elem) {
		array = Arrays.copyOf(array, array.length + 1);
		array[array.length - 1] = elem;
		return array;
	}

	public static float[] append(float[] array, float[] array2) {
		array = Arrays.copyOf(array, array.length + array2.length);
		System.arraycopy(array2, 0, array, array.length - array2.length, array2.length);
		return array;
	}

	public static int[] append(int[] array, int[] array2) {
		array = Arrays.copyOf(array, array.length + array2.length);
		System.arraycopy(array2, 0, array, array.length - array2.length, array2.length);
		return array;
	}

	public static int[] append(int[] array, int elem) {
		array = Arrays.copyOf(array, array.length + 1);
		array[array.length - 1] = elem;
		return array;
	}

	public static byte[] append(byte[] array, byte elem) {
		array = Arrays.copyOf(array, array.length + 1);
		array[array.length - 1] = elem;
		return array;
	}

	public static boolean[] append(boolean[] array, boolean elem) {
		array = Arrays.copyOf(array, array.length + 1);
		array[array.length - 1] = elem;
		return array;
	}

	public static <T> T[] insert(T[] array, T elem, int idx) {
		T[] tmp = array;
		array = alloc1D(array, tmp.length + 1);
		System.arraycopy(tmp, 0, array, 0, idx);
		array[idx] = elem;
		System.arraycopy(tmp, idx, array, idx + 1, tmp.length - idx);
		return array;
	}

	public static int[] insert(int[] array, int elem, int idx) {
		int[] tmp = array;
		array = new int[tmp.length + 1];
		System.arraycopy(tmp, 0, array, 0, idx);
		array[idx] = elem;
		System.arraycopy(tmp, idx, array, idx + 1, tmp.length - idx);
		return array;
	}

	public static <T> T[] insertIfUniqueOrSwap(T[] array, T elem, int idx) {
		int found = -1;
		for(int i = 0; i < array.length; i++)
			if(array[i] == elem)
				found = i;
		if(found == -1)
			return insert(array, elem, idx);
		T tmp        = array[idx];
		array[idx]   = elem;
		array[found] = tmp;
		return array;
	}

	public static <E, T extends E> E[] remove(E[] array, T elem) {
		int count = 0;
		for(E e : array)
			if(e == elem)
				count++;

		if(count == 0) return array;

		E[] result = alloc1D(array, array.length - count);

		int i = 0;
		for(E e : array)
			if(e != elem)
				result[i++] = e;

		return result;
	}

	public static <T> T[] removeEquals(T[] array, T elem) {
		int count = 0;
		for(T e : array)
			if(e.equals(elem))
				count++;

		if(count == 0) return array;

		T[] result = alloc1D(array, array.length - count);

		int i = 0;
		for(T e : array)
			if(!e.equals(elem))
				result[i++] = e;

		return result;
	}

	public static <T> T[] remove(T[] array, int index) {
		T[] result = alloc1D(array, array.length - 1);
		System.arraycopy(array, 0,         result, 0,                     index);
		System.arraycopy(array, index + 1, result, index, result.length - index);
		return result;
	}

	public static boolean[] remove(boolean[] array, int index) {
		boolean[] result = new boolean[array.length - 1];
		System.arraycopy(array, 0,         result, 0,                     index);
		System.arraycopy(array, index + 1, result, index, result.length - index);
		return result;
	}

	public static int[] remove(int[] array, int index) {
		int[] result = new int[array.length - 1];
		System.arraycopy(array, 0,         result, 0,                     index);
		System.arraycopy(array, index + 1, result, index, result.length - index);
		return result;
	}

	public static int[] remove(int[] array, int index, int count) {
		int[] result = new int[array.length - count];
		System.arraycopy(array, 0, result, 0, index);
		System.arraycopy(array, index + count, result, index, result.length - index);
		return result;
	}

	public static float[] remove(float[] array, int index, int count) {
		float[] result = new float[array.length - count];
		System.arraycopy(array, 0,         result, 0,                     index);
		System.arraycopy(array, index + count, result, index, result.length - index);
		return result;
	}

	public static float[] remove(float[] array, int index) {
		float[] result = new float[array.length - 1];
		System.arraycopy(array, 0,         result, 0,                     index);
		System.arraycopy(array, index + 1, result, index, result.length - index);
		return result;
	}

	public static <T> T[] keep(T[] array, int count) {
		return Arrays.copyOf(array, Math.min(count, array.length));
	}

	public static <T> T[] splice(T[] array, int start, int deleteCount, T[] elements) {
		if(start < 0) start += array.length;
		T[] result = alloc1D(array, array.length - deleteCount + elements.length);
		System.arraycopy(array, 0, result, 0, start);
		System.arraycopy(elements, 0, result, start, elements.length);
		System.arraycopy(array, start + deleteCount, result, start + elements.length, result.length - (start + elements.length));
		return result;
	}

	public static byte[] splice(byte[] array, int start, int deleteCount, byte[] elements) {
		if(start < 0) start += array.length;
		byte[] result = new byte[array.length - deleteCount + elements.length];
		System.arraycopy(array, 0, result, 0, start);
		System.arraycopy(elements, 0, result, start, elements.length);
		System.arraycopy(array, start + deleteCount, result, start + elements.length, result.length - (start + elements.length));
		return result;
	}

	public static int[] splice(int[] array, int start, int deleteCount, int[] elements) {
		if(start < 0) start += array.length;
		int[] result = new int[array.length - deleteCount + elements.length];
		System.arraycopy(array, 0, result, 0, start);
		System.arraycopy(elements, 0, result, start, elements.length);
		System.arraycopy(array, start + deleteCount, result, start + elements.length, result.length - (start + elements.length));
		return result;
	}

	public static int[] cat(int[] a1, int[] a2) {
		if(a1.length == 0) return a2;
		if(a2.length == 0) return a1;
		int[] result = Arrays.copyOf(a1, a1.length + a2.length);
		System.arraycopy(a2, 0, result, a1.length, a2.length);        
		return result;
	}

	public static float[] cat(float[] a1, float[] a2) {
		if(a1.length == 0) return a2;
		if(a2.length == 0) return a1;
		float[] result = Arrays.copyOf(a1, a1.length + a2.length);
		System.arraycopy(a2, 0, result, a1.length, a2.length);        
		return result;
	}

	public static byte[] cat(byte[] a1, byte[] a2) {
		if(a1.length == 0) return a2;
		if(a2.length == 0) return a1;
		byte[] result = Arrays.copyOf(a1, a1.length + a2.length);
		System.arraycopy(a2, 0, result, a1.length, a2.length);        
		return result;
	}

	public static <T> T[] cat(T[] a1, T[] a2) {
		if(a1.length == 0) return a2;
		if(a2.length == 0) return a1;
		T[] result = Arrays.copyOf(a1, a1.length + a2.length);
		System.arraycopy(a2, 0, result, a1.length, a2.length);        
		return result;
	}

	@SafeVarargs
	public static <T> T[] cat(T[] a0, T[] ... as) {
		int count = a0.length;
		for(T[] a : as)
			count += a.length;
		T[] result = Arrays.copyOf(a0, count);
		count = a0.length;
		for(T[] a : as) {
			System.arraycopy(a, 0, result, count, a.length);
			count += a.length;
		}
		return result;
	}

	public static byte[] getLow8BitAsBytes(int ... data) {
		byte[] result = new byte[data.length];
		for(int i = 0; i < data.length; i++)
			result[i] = (byte)data[i];
		return result;
	}

	public static <T> T[] dropFirst(int c, T[] a) {
		return Arrays.copyOfRange(a, c, a.length);
	}

	public static byte[] dropFirst(int c, byte[] a) {
		return Arrays.copyOfRange(a, c, a.length);
	}

	public static float[] dropFirst(int c,  float[] a) {
		return Arrays.copyOfRange(a, c, a.length);
	}

	public static <T> T[] dropLast(int c, T[] a) {
		return Arrays.copyOf(a, a.length - c);
	}

	public static boolean containsEquals(Object[] a, Object element) {
		if(element == null) {
			for(Object o : a)
				if(o == null)
					return true;
			return false;
		}
		for(Object o : a)
			if(element.equals(o))
				return true;
		return false;
	}

	public static boolean contains(Object[] a, Object element) {
		for(Object o : a)
			if(element == o)
				return true;
		return false;
	}
	
	public static int firstIndexOf(Object[] a, Object element) {
		for(int i = 0; i < a.length; i++)
			if(element == a[i])
				return i;
		return -1;
	}

	public static boolean contains(int[] a, int element) {
		for(int o : a)
			if(element == o)
				return true;
		return false;
	}

	public static boolean hasDuplicate(int[] a, int refObjIndex) {
		int element = a[refObjIndex];
		for (int i=0; i<a.length; i++)
		{
			if (i==refObjIndex)
				continue;
			if (a[i]==element)
				return true;
		}
		return false;
	}

	public static boolean sameElements(Object[] a1, Object[] a2) {
		if(a1 == a2)               return true;
		if(a1.length != a2.length) return false;
		o1loop:
			for(Object o1 : a1) {
				for(Object o2 : a2)
					if(o1 == o2)
						continue o1loop;
				return false;
			}
		return true;
	}

	public static int[] asIntArray(byte[] bytes) {
		int[] result = new int[(bytes.length + 3) / 4];
		int src = 0;
		for(int i = 0; i < result.length - 1; i++) {
			result[i] = ((bytes[src] & 0xFF) << 24) | ((bytes[src + 1] & 0xFF) << 16) | ((bytes[src + 2] & 0xFF) << 8) | ((bytes[src + 3] & 0xFF));
			src += 4;
		}
		switch(bytes.length - src) {
		case 1:
			result[result.length - 1] = ((bytes[src] & 0xFF) << 24);
			break;
		case 2:
			result[result.length - 1] = ((bytes[src] & 0xFF) << 24) | ((bytes[src + 1] & 0xFF) << 16);
			break;
		case 3:
			result[result.length - 1] = ((bytes[src] & 0xFF) << 24) | ((bytes[src + 1] & 0xFF) << 16) | ((bytes[src + 2] & 0xFF) << 8);
			break;
		case 4:
			result[result.length - 1] = ((bytes[src] & 0xFF) << 24) | ((bytes[src + 1] & 0xFF) << 16) | ((bytes[src + 2] & 0xFF) << 8) | ((bytes[src + 3] & 0xFF));
			break;
		}
		return result;
	}

	public static <T> T[] addIfUnique(T[] array, T element) {
		for(T e : array)
			if(e == element)
				return array;
		return append(array, element);
	}

	public static <T> T[] addIfUniqueEquals(T[] array, T element) {
		for(T e : array)
			if(e.equals(element))
				return array;
		return append(array, element);
	}

	public static void reverseArrayRange(int[] array, int start, int end) {
		end--;
		while (start < end) {
			int tmp      = array[start];
			array[start] = array[end];
			array[end]   = tmp;
			start++;
			end--;
		}
	}

	public static void reverseArrayRange(float[] array, int start, int end) {
		end--;
		while (start < end) {
			float tmp      = array[start];
			array[start] = array[end];
			array[end]   = tmp;
			start++;
			end--;
		}
	}

	public static <T> void reverseArrayRange(T[] array, int start, int end) {
		end--;
		while (start < end) {
			T tmp        = array[start];
			array[start] = array[end];
			array[end]   = tmp;
			start++;
			end--;
		}
	}

	public static void rotateArrayRange(int[] array, int from, int to, int n) {
		rotateArrayRange(array, from, to, n, Arrays.copyOfRange(array, from, to));
	}

	public static void rotateArrayRange(int[] array, int from, int to, int n, int[] copyOfRange) {
		final int d = to - from;
		if (n < 0) n = to - from + n;
		for (int i = 0; i < d; ++i)
			array[from + i] = copyOfRange[(i + n) % d];
	}

	public static <T> void rotateArrayRange(T[] array, int from, int to, int n) {
		rotateArrayRange(array, from, to, n, Arrays.copyOfRange(array, from, to));
	}

	public static <T> void rotateArrayRange(T[] array, int from, int to, int n, T[] copyOfRange) {
		final int d = to - from;
		if (n < 0) n = to - from + n;
		for (int i = 0; i < d; ++i)
			array[from + i] = copyOfRange[(i + n) % d];		
	}



	@SuppressWarnings("unchecked")
	public static <T> T[] reversedCopyOf(T[] array) {
		int size = array.length;

		if(size <= 1)
			return array;

		int size1 = size - 1;
		T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), size);
		for(int i = 0; i < size; i++)
			result[i] = array[size1 - i];
		return result;
	}	

	private static int[][] COUNT_UP = new int[0][]; 
	public synchronized static int[] countUp(int size) {
		if(size > 1024) {
			int[] result = new int[size];
			for(int i = 0; i < result.length; i++)
				result[i] = i;
			return result;
		}
		if(size >= COUNT_UP.length) {
			int low = COUNT_UP.length;
			COUNT_UP = Arrays.copyOf(COUNT_UP, size + 1);
			for(int j = low; j < COUNT_UP.length; j++) {
				int[] result = new int[j];
				for(int i = 0; i < result.length; i++)
					result[i] = i;
				COUNT_UP[j] = result;
			}
		}
		return COUNT_UP[size];
	}

	public static <T> T[] copyAndSort(T[] builtinFunctions) {
		T[] result = builtinFunctions.clone();
		Arrays.sort(result);
		return result;
	}

	public static <T extends Comparable<?>> Object[]  toSortedArray(Collection<T> collection) {
		Object[] result = collection.toArray();
		Arrays.sort(result);
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> Object[]  toSortedArray(Collection<T> collection, Comparator<T> cmp) {
		Object[] result = collection.toArray();
		Arrays.sort(result, (Comparator<Object>)cmp);
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] alloc1D(Class<T> componentType, int size) {
		return (T[])Array.newInstance(componentType, size);
	}

	public static <T> Iterable<T[]> permutations(final T[][] sets) {
		int tmp = 1;
		for(int i = 0; i < sets.length; i++)
			tmp *= sets[i].length;
		final int count = tmp;

		return new Iterable<T[]>() {
			@SuppressWarnings("unchecked")
			@Override
			public Iterator<T[]> iterator() {
				return new Iterator<T[]>() {
					int           i;
					T[]           tmp  = (T[]) Array.newInstance(sets[0][0].getClass(), sets.length);
					int[]         idxs = new int[sets.length];

					{
						for(int i = 0; i < sets.length; i++)
							tmp[i] = sets[i][idxs[i]++];
					}

					@Override
					public boolean hasNext() {
						return i < count;
					}

					@Override
					public T[] next() {
						T[] result = this.tmp.clone();
						i++;
						for(int i = 0; i < sets.length; i++) {
							if(idxs[i] < sets[i].length) {
								tmp[i] = sets[i][idxs[i]++];
								break;
							}
							idxs[i] = 0;
							tmp[i]  = sets[i][idxs[i]++];
						}
						return result;
					}

					@Override
					public void remove() {}
				};
			}
		};
	}

	public static int[] toIntArray(Integer[] data) {
		int[] r = new int[data.length];
		for(int i = 0; i < data.length; i++)
			r[i] = data[i].intValue();		
		return r;
	}

	public static  <T> T get(T[] a, int idx, T defaultValue) {
		return idx > 0 && idx < a.length ? a[idx] : defaultValue;
	}

	public static Boolean[] toObjectArray(boolean[] primitiveArray) {
		final Boolean[] objects = new Boolean[primitiveArray.length];
		int index = 0;
		for (boolean b : primitiveArray)
			objects[index++] = Boolean.valueOf(b);
		return objects;
	}

	public static Integer[] toObjectArray(int[] primitiveArray) {
		final Integer[] objects = new Integer[primitiveArray.length];
		int index = 0;
		for (int v : primitiveArray)
			objects[index++] = Integer.valueOf(v);
		return objects;
	}

	public static Double[] toObjectArray(double[] primitiveArray) {
		final Double[] objects = new Double[primitiveArray.length];
		int index = 0;
		for (double b : primitiveArray)
			objects[index++] = Double.valueOf(b);
		return objects;
	}

	public static <T> SortedSet<T> toSortedSet(T[] a) {
		TreeSet<T> result = new TreeSet<>();
		CollectionUtilities.addAll(result, a);
		return result;
	}

	public static int[][] deepClone(int[][] source) {
		int[][] result = new int[source.length][];
		for (int i = 0; i < source.length; i++)
			result[i] = source[i].clone();
		return result;
	}

	public static float[][] deepClone(float[][] source) {
		float[][] result = new float[source.length][];
		for (int i = 0; i < source.length; i++)
			result[i] = source[i].clone();
		return result;
	}

	public static String[] toStringArray(Object[] values) {
		if(values == null) return null;
		String[] result = new String[values.length];
		for(int i = 0; i < values.length; i++) {
			if(values[i] == null) continue;
			result[i] = values[i].toString();
		}
		return result;
	}

	public static byte[] toByteArray(ByteBuffer buffer) {
		if(buffer.hasArray())
			return buffer.array();
		byte[] result = new byte[buffer.capacity()];
		buffer.rewind();
		buffer.get(result);
		return result;
	}

	public static int minIndex( float[] vals ) {
		float min = Float.MAX_VALUE;
		int minI = -1;
		
		for (int i = 0; i < vals.length; i++)
			if (vals[i] < min) {
				minI = i;
				min = vals[i];
			}
		
		return minI;
	}
	
	public static <T> T find(T[] array, Predicate<T> predicate) {
		for (T element : array) {
			if (predicate.test(element))
				return element;
		}
		return null;
	}
}
