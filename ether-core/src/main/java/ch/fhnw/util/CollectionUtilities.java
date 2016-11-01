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
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public final class CollectionUtilities {
	public static <E extends Enum<E>> EnumSet<E> cat(EnumSet<E> collection, E element) {
		EnumSet<E> result = EnumSet.copyOf(collection);
		result.add(element);
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <E, T extends Collection<E>> T cat(T collection, E element) {
		try {
			T result = (T) collection.getClass().newInstance();
			CollectionUtilities.addAll(result, collection);
			result.add(element);
			return result;
		} catch(Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	@SuppressWarnings("unchecked")
	public static <E, T extends Collection<E>> T cat(T c0, T c1) {
		try {
			T result = (T)c0.getClass().newInstance();
			CollectionUtilities.addAll(result, c0);
			CollectionUtilities.addAll(result, c1);
			return result;
		} catch(Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	public static <E> E last(List<E> list) {
		return list.get(list.size() - 1);
	}

	/**
	 * Calculates the added and removed difference between s1 and s2.
	 * 
	 * @param s1 The first collection.
	 * @param s2 The second collection.
	 * @param added The elements that were added in s2 in comparison to s1.
	 * @param removed The elements that were removed from s1 in comparison to s2.
	 * @return The sum of added and removed elements.
	 */
	public static <T> int differences(Collection<? extends T> s1, Collection<? extends T> s2, Collection<T> added, Collection<T> removed) {
		CollectionUtilities.addAll(added, s2);
		CollectionUtilities.removeAll(added, s1);
		CollectionUtilities.addAll(removed, s1);
		CollectionUtilities.removeAll(removed, s2);
		return added.size() + removed.size();
	}
	
	/**
	 * Returns true if both collections contain the same elements and the elements are in the same order.
	 * Note that the start elements do not have to be the equal!
	 * @param s1 The first collection
	 * @param s2 The second collection
	 * @return true if s1 and s2 are same in the sense explained above. 
	 */
	public static boolean sameElementsSameOrder(Collection<?> s1, Collection<?> s2) {
		if (s1.size() != s2.size()) return false;				
		
		Iterator<?> it1 = s1.iterator();
		Object first = it1.next();
		for (Iterator<?> it2 = s2.iterator(); it2.hasNext(); ) {
			if (it2.next().equals(first)) {
				while (it1.hasNext()) {
					if (!it2.hasNext()) it2 = s2.iterator();
					if (!it1.next().equals(it2.next())) return false;										
				}				
				return true;
			}
		}			
		return false;		
	}

	public static <K, V> void putAt(LinkedHashMap<K, V> map, K key, V value, int pos) {
		Iterator<Entry<K,V>> ei   = map.entrySet().iterator();
		LinkedHashMap<K, V>  pre  = new LinkedHashMap<>();
		LinkedHashMap<K, V>  post = new LinkedHashMap<>();

		for(int i = 0; i < pos; i++) {
			if(!ei.hasNext()) break;
			Entry<K,V> tmpE = ei.next();
			pre.put(tmpE.getKey(), tmpE.getValue());
		}
		// skip element at pos
		if(ei.hasNext()) ei.next();
		while(ei.hasNext()) {
			Entry<K,V> tmpE = ei.next();
			post.put(tmpE.getKey(), tmpE.getValue());			
		}

		map.clear();
		map.putAll(pre);
		map.put(key, value);
		map.putAll(post);
	}

	public static <T> int indexOfEquals(Collection<T> collection, T element) {
		int pos = 0;
		for(T e : collection) {
			if(element.equals(e))
				return pos;
			pos++;
		}
		return -1;
	}

	public static <T> boolean removeAll(Collection<T> collection, T toRemove) {
		boolean result = false;
		for(Iterator<T> i = collection.iterator(); i.hasNext();)
			if(i.next() == toRemove) {
				i.remove();
				result = true;
			}
		return result;
	}

	public static <T> boolean removeAll(Collection<T> collection, Collection<?> toRemove) {
		// optimize == case
		if(collection == toRemove) {
			boolean result = !collection.isEmpty();
			collection.clear();
			return result;
		}

		if(toRemove.size() > 128 && !(toRemove instanceof Set<?>))
			toRemove = toRemove.getClass().getName().contains("Identity") ? new IdentityHashSet<>(toRemove) : new HashSet<>(toRemove);
		
		if(collection instanceof AbstractSet<?> && collection.size() <= toRemove.size()) 
			return removeAllFromSet((AbstractSet<?>) collection, toRemove);
		else if(collection instanceof ArrayList<?>)
			return removeAllFromArrayList((ArrayList<T>)collection, toRemove);
	
		return collection.removeAll(toRemove);
	}

	// optimize ArrayList(ArrayList) case O(n*n*m) -> O(n)
	private static <T> boolean removeAllFromArrayList(ArrayList<T> collection, Collection<?> toRemove) {
		boolean result = false;
		for(int i = collection.size(); --i >= 0;)
			if(toRemove.contains(collection.get(i))) {
				collection.remove(i);
				result = true;
			}
		return result;
	}
	
	private static boolean removeAllFromSet(AbstractSet<?> collection, Collection<?> toRemove) {
		boolean result = false;
		for (Object o : toRemove)
			result |= collection.remove(o);
		return result;
	}

	public static <T> boolean addAll(Collection<T> collection, Collection<? extends T> toAdd) {
		int size = toAdd.size();
		boolean result = false;
		if(size > 0) {
			if(size < 10)
				for(T element : toAdd)
					result |= collection.add(element);
			else
				result = collection.addAll(toAdd);
		}
		return result;
	}

	@SafeVarargs
	public static <T> boolean addAll(Collection<T> collection, T ... toAdd) {
		int size = toAdd.length;
		boolean result = false;
		if(size > 0) {
			for(T element : toAdd)
				result |= collection.add(element);
		}
		return result;
	}

	public static <T> boolean containsIdentity(Collection<? extends T> collection, T element) {
		for(T e : collection)
			if(e == element)
				return true;
		return false;
	}

	public static <T extends Comparable<T>> List<T> toSortedList(Collection<T> collection) {
		List<T> result = new ArrayList<>(collection);
		Collections.sort(result);
		return result;
	}
	

	public static <T> void addIfUnique(List<T> list, T element) {
		for(T e : list)
			if(e == element)
				return;
		list.add(element);
	}

	public static Object toArray(List<?> list, Class<?> componentType) {
		Object result = Array.newInstance(componentType, list.size());
		System.arraycopy(list.toArray(), 0, result, 0, list.size());
		return result;
	}

	public static <T> List<T> append(List<T> list, T[] array) {
		Collections.addAll(list, array);
		return list;
	}

	public static <T> List<T> asList(T e0, T e1) {
		ArrayList<T> result = new ArrayList<>(2);
		result.add(e0);
		result.add(e1);
		return result;
	}
	
	public static <T> List<T> asList(T e0, T e1, T e2) {
		ArrayList<T> result = new ArrayList<>(3);
		result.add(e0);
		result.add(e1);
		result.add(e2);
		return result;
	}

	@SafeVarargs
	public static <T> List<T> asList(T ... elements) {
		ArrayList<T> result = new ArrayList<>(elements.length);
		Collections.addAll(result, elements);
		return result;
	}

	public static <T> boolean removeIdentity(Collection<T> c, T e) {
		for(Iterator<T> i = c.iterator(); i.hasNext();) 
			if(i.next() == e) {
				i.remove();
				return true;
			}
		return false;
	}
	
	public static int sizeOfIntersection(Collection<?> a, Collection<?> b) {
		int count = 0;
		for (Object o : a)
			if (b.contains( o ))
				count++;
		return count;
	}
	
	public static <T> List<T> filterType(Class<T> cls, Collection<?> c) {
		return c.stream().filter(cls::isInstance).map(cls::cast).collect(Collectors.toList());
	}	
}
