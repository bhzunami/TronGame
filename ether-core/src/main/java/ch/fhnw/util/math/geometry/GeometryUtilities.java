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

package ch.fhnw.util.math.geometry;

import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec3;

public final class GeometryUtilities {
	/**
	 * Intersects screen ray (i.e. ray in z direction at coordinate x-y) with a
	 * triangle. Returns Float.POSITIVE_INFINITY if no intersection, a positive
	 * value if intersection "in front" of screen and a negative value if
	 * intersection "behind" screen.
	 */
	public static float intersectScreenRayWithTriangle(float x, float y, float[] triangle, int index) {
		return intersectRayWithTriangle(new Vec3(x, y, 0), Vec3.Z, triangle, index);
	}

	/**
	 * Intersects ray (i.e. ray in z direction at coordinate x-y) with a
	 * triangle. Returns Float.POSITIVE_INFINITY if no intersection, a positive
	 * value if intersection is on positive side of ray (with respect to origin)
	 * and a negative value if intersection is on negative side of ray.
	 */
	public static float intersectRayWithTriangle(Vec3 rayOrigin, Vec3 rayDirection, float[] triangle, int index) {
		Vec3 o = rayOrigin;
		Vec3 d = rayDirection;

		// edge e1 = p2 - p1
		float e1x = triangle[index + 3] - triangle[index];
		float e1y = triangle[index + 4] - triangle[index + 1];
		float e1z = triangle[index + 5] - triangle[index + 2];

		// edge e2 = p3 - p1
		float e2x = triangle[index + 6] - triangle[index];
		float e2y = triangle[index + 7] - triangle[index + 1];
		float e2z = triangle[index + 8] - triangle[index + 2];

		// Vec3 p = d x e2
		float px = d.y * e2z - d.z * e2y;
		float py = d.z * e2x - d.x * e2z;
		float pz = d.x * e2y - d.y * e2x;

		// float det = e1 * p
		float det = e1x * px + e1y * py + e1z * pz;

		if (MathUtilities.isZero(det))
			return Float.POSITIVE_INFINITY;

		// Vec3 t = o - p1 (distance from p1 to ray origin)
		float tx = o.x - triangle[index];
		float ty = o.y - triangle[index + 1];
		float tz = o.z - triangle[index + 2];

		// float u = (t * p) / det
		float u = (tx * px + ty * py + tz * pz) / det;

		if (u < 0 || u > 1)
			return Float.POSITIVE_INFINITY;

		// Vec3 q = t x e1
		float qx = ty * e1z - tz * e1y;
		float qy = tz * e1x - tx * e1z;
		float qz = tx * e1y - ty * e1x;

		// float v = (d, q) / det
		float v = (d.x * qx + d.y * qy + d.z * qz) / det;

		if (v < 0 || u + v > 1)
			return Float.POSITIVE_INFINITY;

		// t = (e2 * q) / det
		float t = (e2x * qx + e2y * qy + e2z * qz) / det;

		//return (t > MathUtilities.EPSILON) ? t : Float.POSITIVE_INFINITY;
		return t;
	}

	/**
	 * Tests whether a point is within a 2D triangle (x-y plane, z coordinate is ignored).
	 */
	public static boolean testPointInTriangle2D(float x, float y, float[] triangle) {
		return testPointInTriangle2D(x, y, triangle, 0);
	}

	/**
	 * Tests whether a point is within a 2D triangle (x-y plane, z coordinate is ignored).
	 */
	public static boolean testPointInTriangle2D(float x, float y, float[] triangle, int index) {
		boolean b1 = sign(x, y, triangle[index], triangle[index + 1], triangle[index + 3], triangle[index + 4]) < 0.0f;
		boolean b2 = sign(x, y, triangle[index + 3], triangle[index + 4], triangle[index + 6], triangle[index + 7]) < 0.0f;
		boolean b3 = sign(x, y, triangle[index + 6], triangle[index + 7], triangle[index], triangle[index + 1]) < 0.0f;
		return ((b1 == b2) && (b2 == b3));
	}

	private static float sign(float p1x, float p1y, float p2x, float p2y, float p3x, float p3y) {
		return (p1x - p3x) * (p2y - p3y) - (p2x - p3x) * (p1y - p3y);
	}

	/**
	 * Tests whether a point is within a 2D (convex or concave) polygon (x-y
	 * plane, z coordinate is ignored).
	 */
	public static boolean testPointInPolygon2D(float x, float y, float[] polygon) {
		boolean oddNodes = false;
		int j = polygon.length - 3;
		for (int i = 0; i < polygon.length; i += 3) {
			float ax = polygon[i];
			float ay = polygon[i + 1];
			float bx = polygon[j];
			float by = polygon[j + 1];
			if ((ay < y && by >= y) || (by < y && ay >= y)) {
				if (ax + (y - ay) / (by - ay) * (bx - ax) < x) {
					oddNodes = !oddNodes;
				}
			}
			j = i;
		}
		return oddNodes;
	}

	/**
	 * Tests whether a point is within a 2D (convex or concave) polygon (x-y
	 * plane, z coordinate is ignored).
	 */
	public static boolean testPointInPolygon2D(float x, float y, Polygon polygon) {
		boolean oddNodes = false;
		int j = polygon.getNumVertices() - 1;
		for (int i = 0; i < polygon.getNumVertices(); i++) {
			Vec3 a = polygon.get(i);
			Vec3 b = polygon.get(j);
			if ((a.y < y && b.y >= y) || (b.y < y && a.y >= y)) {
				if (a.x + (y - a.y) / (b.y - a.y) * (b.x - a.x) < x) {
					oddNodes = !oddNodes;
				}
			}
			j = i;
		}
		return oddNodes;
	}

	/**
	 * Tests if two circles overlap (x-y plane, z coordinate is ignored).
	 */
	public static boolean testCirclesOverlap2D(Vec3 p0, float r0, Vec3 p1, float r1) {
		float d = MathUtilities.length(p0.x - p1.x, p0.y - p1.y);
		if (d < r0 + r1)
			return true;
		return false;
	}

	/**
	 * Convert (in-place) an array of vertices from Y-UP to Z-UP coordinate system.
	 * @param vertices the vertices to be converted.
	 */
	public static void convertToZUp(float[] vertices) {
		for (int i = 0; i < vertices.length; i += 3) {
			float y = vertices[i + 1];
			float z = vertices[i + 2];
			vertices[i + 1] = -z;
			vertices[i + 2] = y;
		}
	}
}
