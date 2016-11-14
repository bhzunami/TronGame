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

package ch.fhnw.util.math;

public final class MathUtilities {
	public static final float EPSILON = 0.000001f; // 32 bits
	public static final float PI = (float) Math.PI;
	public static final float RADIANS_TO_DEGREES = 180f / PI;
	public static final float DEGREES_TO_RADIANS = PI / 180;

	public static float lerp(float v0, float v1, float t) {
		return (1 - t) * v0 + t * v1;
	}

	public static double lerp(double v0, double v1, double t) {
		return (1 - t) * v0 + t * v1;
	}
	
	public static float map(float value, float sourceMin, float sourceMax, float targetMin, float targetMax) {
		if (sourceMax - sourceMin == 0)
			return 0;
		return (value - sourceMin) * (targetMax - targetMin) / (sourceMax - sourceMin) + targetMin;
	}

	public static double map(double value, double sourceMin, double sourceMax, double targetMin, double targetMax) {
		if (sourceMax - sourceMin == 0)
			return 0;
		return (value - sourceMin) * (targetMax - targetMin) / (sourceMax - sourceMin) + targetMin;
	}

	public static int clamp(int value, int min, int max) {
		if(value < min)      return min;
		else if(value > max) return max;
		else                 return value;
	}

	public static float clamp(float value, float min, float max) {
		if(value < min || Float.isNaN(value)) return min;
		else if(value > max)                  return max;
		else                                  return value;
	}

	public static double clamp(double value, double min, double max) {
		if(value < min || Double.isNaN(value)) return min;
		else if(value > max)                   return max;
		else                                   return value;
	}
	
	public static float wrap(float v) {
		float result = v % 1f;
		return result < 0 ? result + 1 : result;
	}

	public static double wrap(double v) {
		double result = v % 1.0;
		return result < 0 ? result + 1 : result;
	}

	public static int random(int min, int max) {
		return min + (int)Math.round(Math.random() * (max - min));
	}

	public static float random(float min, float max) {
		return min + (float)(Math.random() * (max - min));
	}

	public static double random(double min, double max) {
		return min + Math.random() * (max - min);
	}

	/** Returns true if the value is zero (using the default tolerance as upper bound) */
	static public boolean isZero(float value) {
		return Math.abs(value) <= EPSILON;
	}

	/**
	 * Returns true if the value is zero.
	 * 
	 * @param tolerance
	 *            represent an upper bound below which the value is considered zero.
	 */
	static public boolean isZero(float value, float tolerance) {
		return Math.abs(value) <= tolerance;
	}

	/**
	 * Returns true if a is nearly equal to b. The function uses the default floating error tolerance.
	 * 
	 * @param a
	 *            the first value.
	 * @param b
	 *            the second value.
	 */
	static public boolean isEqual(float a, float b) {
		return Math.abs(a - b) <= EPSILON;
	}

	/**
	 * Returns true if a is nearly equal to b.
	 * 
	 * @param a
	 *            the first value.
	 * @param b
	 *            the second value.
	 * @param tolerance
	 *            represent an upper bound below which the two values are considered equal.
	 */
	static public boolean isEqual(float a, float b, float tolerance) {
		return Math.abs(a - b) <= tolerance;
	}

	/**
	 * Calculates the length of a vector [x, y].
	 * 
	 * @return the euclidean length of [x, y]
	 */
	public static float length(float x, float y) {
		return (float) Math.sqrt(x * x + y * y);
	}

	/**
	 * Calculates the length of a vector [x, y, z].
	 * 
	 * @return the euclidean length of [x, y, z]
	 */
	public static float length(float x, float y, float z) {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	/**
	 * Calculates the length of a vector [x, y, z, w].
	 * 
	 * @return the euclidean length of [x, y, z, w]
	 */
	public static float length(float x, float y, float z, float w) {
		return (float) Math.sqrt(x * x + y * y + z * z + w * w);
	}

	/**
	 * Calculates the dot product between two vectors [x, y].
	 * 
	 * @return The dot product between the two vectors
	 */
	public static float dot(float ax, float ay, float bx, float by) {
		return ax * bx + ay * by;
	}

	/**
	 * Calculates the dot product between two vectors [x, y, z].
	 * 
	 * @return The dot product between the two vectors
	 */
	public static float dot(float ax, float ay, float az, float bx, float by, float bz) {
		return ax * bx + ay * by + az * bz;
	}

	/**
	 * Calculates the dot product between two vectors [x, y, z, w].
	 * 
	 * @return The dot product between the two vectors
	 */
	public static float dot(float ax, float ay, float az, float aw, float bx, float by, float bz, float bw) {
		return ax * bx + ay * by + az * bz + aw * bw;
	}

	/**
	 * Checks if a number is a power of two.
	 * @param n the number to be evaluated
	 * @return true if n is a power of two, false otherwise
	 */
	public static boolean isPowerOfTwo(int n) {
		return (n > 0) && ((n & (n - 1)) == 0);
	}

	public static int nextPowerOfTwo(int num) {
		int result = 1;
		while (num != 0) {
			num >>= 1;
		result <<= 1;
		}
		return result;
	}

	public static float min(float[] values) {
		float result = values[0];
		for(int i = 1; i < values.length; i++)
			if(values[i] < result) result = values[i];
		return result;
	}

	public static float max(float[] values) {
		float result = values[0];
		for(int i = 1; i < values.length; i++)
			if(values[i] > result) result = values[i];
		return result;
	}

	public static float[] normalize(float[] values, float targetMin, float targetMax) {
		float min = min(values);
		float max = max(values);
		for(int i = 0; i < values.length; i++)
			values[i] = map(values[i], min, max, targetMin, targetMax);
		return values;
	}
}
