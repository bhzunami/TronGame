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

import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

/**
 * Basis representation, useful for basis transformations.
 * 
 * @author radar
 *
 */
public class Basis {
	private final Vec3 u;
	private final Vec3 v;
	private final Vec3 w;
	private final Vec3 origin;
	
	public Basis(Vec3 u, Vec3 v, Vec3 w, Vec3 origin) {
		this.u = u;
		this.v = v;
		this.w = w;
		this.origin = origin;
	}
	
	public Basis(Vec3 v1, Vec3 v2, Vec3 v3) {
		this.u = v2.subtract(v1).normalize();
		this.v = v3.subtract(v1).normalize();
		this.w = u.cross(v).normalize();
		this.origin = v1;
	}
	
	public Basis(Plane plane, Vec3 point) {
		this.u = plane.project(point).subtract(plane.getOrigin()).normalize();
		this.w = plane.getNormal();
		this.v = w.cross(u).normalize();
		this.origin = plane.getOrigin();
	}
	
	public Vec3 getU() {
		return u;
	}
	
	public Vec3 getV() {
		return v;
	}
	
	public Vec3 getW() {
		return w;
	}
	
	public Vec3 getOrigin() {
		return origin;
	}
	
	public Mat4 getUVWToXYZTransform() {
		return getXYZToUVWTransform().inverse();
	}
	
	public Mat4 getXYZToUVWTransform() {
		return new Mat4(u, v, w, origin);		
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Basis))
			return false;
		Basis b = (Basis) obj;
		return origin.equals(b.origin) && u.equals(b.u) && v.equals(b.v) && w.equals(b.w);
	}
	
	@Override
	public int hashCode() {
		return origin.hashCode() + u.hashCode() + v.hashCode() + w.hashCode();
	}

	@Override
	public String toString() {
		return "[" + origin + ", " + u + ", " + v + ", " + w + "]";
	}	
}
