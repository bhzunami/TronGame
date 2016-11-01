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

public final class SortedLongMap<T> extends SimpleSortedMap<long[], T> {

	public boolean containsKey(int key) {
		return Arrays.binarySearch(keys, 0, size, key) >= 0;
	}

	public T get(long key) {
		return _get(Arrays.binarySearch(keys, 0, size, key));
	}

	public T put(long key, T value) {
		int idx = Arrays.binarySearch(keys, 0, size, key);
		T result = _put(idx, value);
		if(idx < 0)
			keys[-idx - 1] = key;
		return result;
	}

	public T remove(long key) {
		return _remove(Arrays.binarySearch(keys, 0, size, key));
	}

	@Override
	protected long[] allocKeys(int size) {
		return new long[size];
	}

	@Override
	protected void clearKeys() {
		Arrays.fill(keys, 0);
	}

	@Override
	protected long[] copyKeys(long[] keys, int size) {
		return Arrays.copyOf(keys, size);
	}
	
	@Override
	protected String keyValueToString(char defChar, char seperator) {
		StringBuilder result = new StringBuilder();
		for(long k : keySet())
			result.append(Long.toString(k)).append(defChar).append(get(k)).append(seperator);
		return result.toString();
	}
}
