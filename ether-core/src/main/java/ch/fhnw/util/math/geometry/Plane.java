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

import ch.fhnw.util.HashUtilities;
import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec3;

/**
 * Basic plane representation. Loosely based on Apache Commons Math3. Immutable.
 * 
 * @author radar
 *
 */
public final class Plane {
	private Vec3 origin;
	private Vec3 normal;
	private float offset;

	/**
	 * Create plane at zero origin, with given normal.
	 */
	public Plane(Vec3 normal) {
		this.origin = Vec3.ZERO;
		this.normal = normal.normalize();
		this.offset = 0;
	}

	/**
	 * Create plane at given origin and normal.
	 */
	public Plane(Vec3 origin, Vec3 normal) {
		this.origin = origin;
		this.normal = normal.normalize();
		this.offset = -origin.dot(this.normal);
	}
	
	/**
	 * Create plane from three given points.
	 */
	public Plane(Vec3 p1, Vec3 p2, Vec3 p3) {
		this(p1, p2.subtract(p1).cross(p3.subtract(p1)));
	}
	
	/**
	 * Returns new plane that has reversed normal of this plane.
	 */
	public Plane reverse() {
		return new Plane(origin, normal.negate());
	}

	/**
	 * Returns origin of this plane.
	 */
	public Vec3 getOrigin() {
		return origin;
	}

	/**
	 * Returns normal of this plane.
	 */
	public Vec3 getNormal() {
		return normal;
	}

	/**
	 * Returns offset of this plane (i.e. closest distance to zero).
	 */
	public float getOffset() {
		return offset;
	}

	/**
	 * Returns distance of given point from plane.
	 */
	public float distance(Vec3 point) {
		return normal.dot(point) + offset;
	}
	
	/**
	 * Project given point onto plane.
	 */
	public Vec3 project(Vec3 point) {
		return point.subtract(normal.scale(distance(point)));
	}

	/**
	 * Intersect line with plane and returns intersection point, or null if no
	 * intersection exists.
	 */
	public Vec3 intersect(Line line) {
		float dot = normal.dot(line.getDirection());
		if (dot == 0)
			return null;
		float k = -(offset + normal.dot(line.getOrigin())) / dot;
		return line.getOrigin().add(line.getDirection().scale(k));
	}
	
	/**
	 * Intersect plane with plane and returns intersection line, or null if no
	 * intersection exists.
	 */
	public Line intersect(Plane plane) {
		Vec3 d = normal.cross(plane.normal);
		if (MathUtilities.isZero(d.length()))
			return null;
		Vec3 p = intersect(this, plane, new Plane(d));
		return new Line(p, p.add(d));
	}
	
	/**
	 * Intersect three planes and returns intersection point, or null if no
	 * intersection exists.
	 */
	public static Vec3 intersect(Plane plane1, Plane plane2, Plane plane3) {
        float a1 = plane1.normal.x;
        float b1 = plane1.normal.y;
        float c1 = plane1.normal.z;
        float d1 = plane1.offset;

        float a2 = plane2.normal.x;
        float b2 = plane2.normal.y;
        float c2 = plane2.normal.z;
        float d2 = plane2.offset;

        float a3 = plane3.normal.x;
        float b3 = plane3.normal.y;
        float c3 = plane3.normal.z;
        float d3 = plane3.offset;

        float a23 = b2 * c3 - b3 * c2;
        float b23 = c2 * a3 - c3 * a2;
        float c23 = a2 * b3 - a3 * b2;
        float det = a1 * a23 + b1 * b23 + c1 * c23;

        if (MathUtilities.isZero(det))
            return null;

        float r = 1 / det;
        return new Vec3((-a23 * d1 - (c1 * b3 - c3 * b1) * d2 - (c2 * b1 - c1 * b2) * d3) * r,
                        (-b23 * d1 - (c3 * a1 - c1 * a3) * d2 - (c1 * a2 - c2 * a1) * d3) * r,
                        (-c23 * d1 - (b1 * a3 - b3 * a1) * d2 - (b2 * a1 - b1 * a2) * d3) * r);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Plane))
			return false;
		Plane p = (Plane) obj;
		return offset == p.offset && origin.equals(p.origin) && normal.equals(p.normal);
	}

	@Override
	public int hashCode() {
		return origin.hashCode() + normal.hashCode() + HashUtilities.hash(offset);
	}

	@Override
	public String toString() {
		return "[origin:" + origin + ", normal:" + normal + ", offset:" + offset + "]";
	}
}
