package ch.fhnw.util;

import java.util.Arrays;
import java.util.Iterator;

public class LRUList<E> implements Iterable<E> {
	private int      maxSize = Integer.MAX_VALUE;
	private int      size;
	private Object[] list    = new Object[16];

	public LRUList(int lruSize) {
		this.maxSize = lruSize;
	}

	public LRUList() {}

	public int size() {
		return size;
	}

	@SuppressWarnings("unchecked")
	public E get(int i) {
		return (E)list[i];
	}

	public int indexOf(E entry) {
		if(entry.getClass().isArray()) {
			for(int i = 0; i < size; i++)
				if(Arrays.deepEquals((Object[])entry, (Object[])list[i]))
					return i;
		} else {
			for(int i = 0; i < size; i++)
				if(entry.equals(list[i]))
					return i;
		}
		return -1;
	}

	public void used(E entry) {
		int idx = indexOf(entry);
		if(idx < 0) {
			size++;
			if(size > maxSize)
				size = maxSize;
			ensureSize(size);
			idx = Math.min(size, list.length - 1);
		}
		System.arraycopy(list, 0, list, 1, idx);
		list[0] = entry;
	}

	private void ensureSize(int size) {
		if(list.length < size)
			list = Arrays.copyOf(list, size + 16);
	}

	@Override
	public String toString() {
		return TextUtilities.toString("[", ",", "]", list, TextUtilities.NONE, 0, size);
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			int idx;
			
			@Override
			public boolean hasNext() {
				return idx < size;
			}

			@SuppressWarnings("unchecked")
			@Override
			public E next() {
				return (E)list[idx++];
			}
		};
	}
}
