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

/**
 * Basic line representation. Loosely based on Apache Commons Math. Immutable.
 * 
 * @author radar
 *
 */
public final class Line {
	private final Vec3 origin;
	private final Vec3 direction;

	/**
	 * Create line from two given points. Origin is calculated as point closest
	 * to zero.
	 */
	public Line(Vec3 p1, Vec3 p2) {
		Vec3 delta = p2.subtract(p1);
		float length = delta.length();
		if (length == 0.0) {
			throw new IllegalArgumentException();
		}
		direction = delta.scale(1 / length);
		origin = p1.add(direction.scale(-p1.dot(delta) / length));
	}
	
	private Line(Vec3 origin, Vec3 direction, boolean ignore) {
		this.origin = origin;
		this.direction = direction;
	}
	
	/**
	 * Create line from provided ray (origin and direction). Note that origin
	 * will not be retained, but recalculated as point closest to zero.
	 */
	public static Line fromRay(Vec3 origin, Vec3 direction) {
		return new Line(origin, origin.add(direction));
	}
	
	/**
	 * Return origin of this line.
	 */
	public Vec3 getOrigin() {
		return origin;
	}

	/**
	 * Return direction of this line.
	 */
	public Vec3 getDirection() {
		return direction;
	}
	
	/**
	 * Return a line that has reversed direction.
	 */
	public Line reverse() {
		return new Line(origin, direction.negate(), true);
	}
	
	/**
	 * Test if this line contains given point.
	 */
	public boolean contains(Vec3 point) {
		float d = distance(point);
		return MathUtilities.isZero(d * d);
	}

	/**
	 * Return projected point on line.
	 */
	public Vec3 project(Vec3 point) {
		Vec3 d = point.subtract(origin);
		return origin.add(direction.scale(d.dot(direction)));
	}

	/**
	 * Return distance of given point with respect to this line.
	 */
	public float distance(Vec3 point) {
        Vec3 d = point.subtract(origin);
        Vec3 n = d.add(direction.scale(-d.dot(direction)));
        return n.length();		
	}
	
	/**
	 * Return distance of given line with respect to this line.
	 */
	public float distance(Line line) {
		Vec3 n = direction.cross(line.direction);
		float l = n.length();
		if (MathUtilities.isZero(l)) {
			// parallel lines
			return origin.distance(line.origin);
		}
		float d = line.origin.subtract(origin).dot(n) / l;
		return Math.abs(d);
	}
	
	/**
	 * Return closest point on this line with respect to other line.
	 */
	public Vec3 getClosestPoint(Line line) {
		float c = direction.dot(line.direction);
		float n = 1 - c * c;
		if (MathUtilities.isZero(n)) {
			// parallel lines
			return origin; 
		}
		Vec3 d = line.origin.subtract(origin);
		float a = d.dot(direction);
		float b = d.dot(line.direction);
		float s = (a - b * c) / n;
		return origin.add(direction.scale(s));
	}
	
	/**
	 * Return intersection of this line with other line or null if they don't intersect.
	 */
	public Vec3 intersect(Line line) {
		Vec3 v = getClosestPoint(line);
		return line.contains(v) ? v : null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Line))
			return false;
		Line l = (Line) obj;
		return origin.equals(l.origin) && direction.equals(l.direction);
	}

	@Override
	public int hashCode() {
		return origin.hashCode() + direction.hashCode();
	}

	@Override
	public String toString() {
		return "[origin:" + origin + ", direction:" + direction + "]";
	}
}
