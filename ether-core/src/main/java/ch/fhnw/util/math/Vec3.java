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

import java.util.Collection;

import ch.fhnw.util.HashUtilities;

/**
 * 3D vector for basic vector algebra. Instances are immutable.
 *
 * @author radar
 */
public final class Vec3 implements IVec3 {
	public static final Vec3 ZERO = new Vec3(0, 0, 0);
	public static final Vec3 ONE = new Vec3(1, 1, 1);
	public static final Vec3 X = new Vec3(1, 0, 0);
	public static final Vec3 Y = new Vec3(0, 1, 0);
	public static final Vec3 Z = new Vec3(0, 0, 1);
	public static final Vec3 X_NEG = new Vec3(-1, 0, 0);
	public static final Vec3 Y_NEG = new Vec3(0, -1, 0);
	public static final Vec3 Z_NEG = new Vec3(0, 0, -1);

	public final float x;
	public final float y;
	public final float z;

	public Vec3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vec3(double x, double y, double z) {
		this((float) x, (float) y, (float) z);
	}

	public Vec3(Vec4 v) {
		this(v.x, v.y, v.z);
	}

	@Override
	public float x() {
		return x;
	}
	
	@Override
	public float y() {
		return y;
	}

	@Override
	public float z() {
		return z;
	}

	public boolean isZero() {
		return MathUtilities.isZero(length());
	}

	public float length() {
		return MathUtilities.length(x, y, z);
	}

	public float distance(Vec3 v) {
		return (float) Math.sqrt((v.x - x) * (v.x - x) + (v.y - y) * (v.y - y) + (v.z - z) * (v.z - z));
	}

	public Vec3 add(Vec3 v) {
		return new Vec3(x + v.x, y + v.y, z + v.z);
	}

	public Vec3 subtract(Vec3 v) {
		return new Vec3(x - v.x, y - v.y, z - v.z);
	}

	public Vec3 scale(float s) {
		return new Vec3(x * s, y * s, z * s);
	}

	public Vec3 negate() {
		return scale(-1);
	}

	public Vec3 normalize() {
		float l = length();
		if (MathUtilities.isZero(l) || l == 1)
			return this;
		return new Vec3(x / l, y / l, z / l);
	}

	public float dot(Vec3 v) {
		return MathUtilities.dot(x, y, z, v.x, v.y, v.z);
	}
	
	public float angle(Vec3 v) {
		return MathUtilities.RADIANS_TO_DEGREES * (float)Math.acos(dot(v) / length() * v.length());
	}

	public Vec3 cross(Vec3 v) {
		return new Vec3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x);
	}

	@Override
	public Vec3 toVec3() {
		return this;
	}

	@Override
	public float[] toArray() {
		return new float[] { x, y, z };
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Vec3))
			return false;
		Vec3 v = (Vec3) obj;
		return x == v.x && y == v.y && z == v.z;
	}
	
	@Override
	public int hashCode() {
		return HashUtilities.hash(x, y, z);
	}

	@Override
	public String toString() {
		return String.format("[% .2f,% .2f,% .2f]", x, y, z);
	}
	
	public static Vec3 lerp(Vec3 v0, Vec3 v1, float t) {
		return new Vec3(MathUtilities.lerp(v0.x, v1.x, t), MathUtilities.lerp(v0.y, v1.y, t), MathUtilities.lerp(v0.z, v1.z, t));
	}

	public static float[] toArray(Collection<Vec3> vectors) {
		if (vectors == null)
			return null;

		float[] result = new float[vectors.size() * 3];
		int i = 0;
		for (Vec3 v : vectors) {
			result[i++] = v.x;
			result[i++] = v.y;
			result[i++] = v.z;
		}
		return result;
	}
}
