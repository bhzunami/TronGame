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

public final class HashUtilities {
	private static final int SEED = 17;

	// this is only very basic. for better recommendations see
	// http://developer.android.com/reference/java/lang/Object.html

	public static int hash(boolean v) {
		return SEED + (v ? 1 : 0);
	}

	public static int hash(int v) {
		return SEED + v;
	}

	public static int hash(float v0) {
		return SEED + Float.floatToIntBits(v0);
	}

	public static int hash(float v0, float v1) {
		return SEED + Float.floatToIntBits(v0 + v1);
	}

	public static int hash(float v0, float v1, float v2) {
		return SEED + Float.floatToIntBits(v0 + v1 + v2);
	}

	public static int hash(float v0, float v1, float v2, float v3) {
		return SEED + Float.floatToIntBits(v0 + v1 + v2 + v3);
	}

	public static int hash(float v0, float v1, float v2, float v3, float v4, float v5) {
		return SEED + Float.floatToIntBits(v0 + v1 + v2 + v3 + v4 + v5);
	}

	public static int hash(float v0, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8) {
		return SEED + Float.floatToIntBits(v0 + v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8);
	}

	public static int hash(float v0, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8,
			float v9, float v10, float v11, float v12, float v13, float v14, float v15) {
		return SEED + Float.floatToIntBits(v0 + v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8 + v9 + v10 + v11 + v12 + v13 + v14 + v15);
	}
}
