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

package ch.fhnw.util.math.path;

import java.util.List;

import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec3;

public final class CatmullRomSpline implements IPath {
	private final Vec3[] points;
	private final boolean continuous;
	private final int segments;

	public CatmullRomSpline(List<Vec3> points, boolean continuous) {
		if (continuous) {
			if (points.size() < 2)
				throw new IllegalArgumentException("continuous catmull-rom spline requires a minimum of 2 points");			
		} else {
			if (points.size() < 4)
				throw new IllegalArgumentException("non-continuous catmull-rom spline requires a minimum of 4 points");
		}
		this.points = points.toArray(new Vec3[points.size()]);
		this.continuous = continuous;
		this.segments = continuous ? points.size() : points.size() - 3;
	}
	
	@Override
	public int getNumNodes() {
		return points.length;
	}

	@Override
	public Vec3 position(float t) {
		t = range(t);
		float u = t * segments;
		int segment = (int) u;
		u -= segment;
		return position(continuous ? segment : (segment + 1), u, points, continuous);
	}
	
	@Override
	public Vec3 velocity(float t) {
		t = range(t);
		float u = t * segments;
		int segment = (int) u;
		u -= segment;
		return velocity(continuous ? segment : (segment + 1), u, points, continuous);
	}

	@Override
	public Vec3 acceleration(float t) {
		t = range(t);
		float u = t * segments;
		int segment = (int) u;
		u -= segment;
		return acceleration(continuous ? segment : (segment + 1), u, points, continuous);
	}

	// calculates value for given segment and position u
	private static Vec3 position(int segment, float u, Vec3[] points, boolean continuous) {
		int n = points.length;
		float u2 = u * u;
		float u3 = u2 * u;
		Vec3 v = points[segment].scale(1.5f * u3 - 2.5f * u2 + 1.0f);
		if (continuous || segment > 0)
			v = v.add(points[(n + segment - 1) % n].scale(-0.5f * u3 + u2 - 0.5f * u));
		if (continuous || segment < (n - 1))
			v = v.add(points[(segment + 1) % n].scale(-1.5f * u3 + 2f * u2 + 0.5f * u));
		if (continuous || segment < (n - 2))
			v = v.add(points[(segment + 2) % n].scale(0.5f * u3 - 0.5f * u2));
		return v;
	}

	// calculates derivative for given segment and position u
	private static Vec3 velocity(int segment, float u, Vec3[] points, boolean continuous) {
		int n = points.length;
		float u2 = u * u;
		Vec3 v = points[segment].scale(4.5f * u2 - 5 * u);
		if (continuous || segment > 0)
			v = v.add(points[(n + segment - 1) % n].scale(-1.5f * u2 + u * 2 - 0.5f));
		if (continuous || segment < (n - 1))
			v = v.add(points[(segment + 1) % n].scale(-4.5f * u2 + 4 * u + 0.5f));
		if (continuous || segment < (n - 2))
			v = v.add(points[(segment + 2) % n].scale(1.5f * u2 - u));
		return v;
	}
	
	// calculates curvature (2nd derivative) for given segment and position u
	private static Vec3 acceleration(int segment, float u, Vec3[] points, boolean continuous) {
		int n = points.length;
		Vec3 v = points[segment].scale(9 * u - 5);
		if (continuous || segment > 0)
			v = v.add(points[(n + segment - 1) % n].scale(-3 * u + 2));
		if (continuous || segment < (n - 1))
			v = v.add(points[(segment + 1) % n].scale(-9 * u + 4));
		if (continuous || segment < (n - 2))
			v = v.add(points[(segment + 2) % n].scale(3 * u - 1));
		return v;
	}

	private float range(float t) {
		if (continuous) {
			t = t % 1;
			if (t < 0)
				t +=1;
		} else {
			t = MathUtilities.clamp(t, 0, 1);
		}
		return t;
	}
}
