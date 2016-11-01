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

import java.util.Collection;

import ch.fhnw.util.HashUtilities;
import ch.fhnw.util.math.Vec3;

/**
 * Axis aligned 3D bounding box. Also contains 2D operations that only take x
 * and y coordinates into account.
 * 
 * @author radar
 */
public final class BoundingBox {
	private boolean valid;
	private float minX;
	private float maxX;
	private float minY;
	private float maxY;
	private float minZ;
	private float maxZ;

	public BoundingBox() {
		reset();
	}

	public void reset() {
		valid = false;
		minX = Float.POSITIVE_INFINITY;
		maxX = Float.NEGATIVE_INFINITY;
		minY = Float.POSITIVE_INFINITY;
		maxY = Float.NEGATIVE_INFINITY;
		minZ = Float.POSITIVE_INFINITY;
		maxZ = Float.NEGATIVE_INFINITY;
	}

	public boolean isValid() {
		return valid;
	}

	public Vec3 getMin() {
		assert valid;
		return new Vec3(minX, minY, minZ);
	}

	public Vec3 getMax() {
		assert valid;
		return new Vec3(maxX, maxY, maxZ);
	}

	public Vec3 getCenter() {
		assert valid;
		return new Vec3(getCenterX(), getCenterY(), getCenterZ());
	}

	public Vec3 getExtent() {
		assert valid;
		return new Vec3(getExtentX(), getExtentY(), getExtentZ());
	}

	public float getMinX() {
		assert valid;
		return minX;
	}

	public float getMinY() {
		assert valid;
		return minY;
	}

	public float getMinZ() {
		assert valid;
		return minZ;
	}

	public float getMaxX() {
		assert valid;
		return maxX;
	}

	public float getMaxY() {
		assert valid;
		return maxY;
	}

	public float getMaxZ() {
		assert valid;
		return maxZ;
	}

	public float getCenterX() {
		assert valid;
		return minX + getExtentX() / 2;
	}

	public float getCenterY() {
		assert valid;
		return minY + getExtentY() / 2;
	}

	public float getCenterZ() {
		assert valid;
		return minZ + getExtentZ() / 2;
	}

	public float getExtentX() {
		assert valid;
		return maxX - minX;
	}

	public float getExtentY() {
		assert valid;
		return maxY - minY;
	}

	public float getExtentZ() {
		assert valid;
		return maxZ - minZ;
	}

	public float getRadius() {
		assert valid;
		return getMax().distance(getCenter());
	}

	public void add(float x, float y, float z) {
		// skip illegal values
		if (Float.isInfinite(x) || Float.isInfinite(y) || Float.isInfinite(z) || Float.isNaN(x) || Float.isNaN(y)
				|| Float.isNaN(z))
			return;

		minX = Math.min(minX, x);
		maxX = Math.max(maxX, x);
		minY = Math.min(minY, y);
		maxY = Math.max(maxY, y);
		minZ = Math.min(minZ, z);
		maxZ = Math.max(maxZ, z);
		valid = true;
	}

	public void add(double x, double y, double z) {
		add((float) x, (float) y, (float) z);
	}

	public void add(Vec3 vertex) {
		if (vertex != null) {
			add(vertex.x, vertex.y, vertex.z);
		}
	}

	public void add(Collection<Vec3> vertices) {
		if (vertices != null) {
			vertices.forEach(this::add);
		}
	}

	public void add(float[] vertices) {
		if (vertices != null) {
			for (int i = 0; i < vertices.length; i += 3) {
				add(vertices[i], vertices[i + 1], vertices[i + 2]);
			}
		}
	}

	public void add(double[] vertices) {
		if (vertices != null) {
			for (int i = 0; i < vertices.length; i += 3) {
				add(vertices[i], vertices[i + 1], vertices[i + 2]);
			}
		}
	}

	public void add(BoundingBox b) {
		if (b != null) {
			add(b.minX, b.minY, b.minZ);
			add(b.maxX, b.maxY, b.maxZ);
		}
	}

	public void grow(float x, float y, float z) {
		if (!isValid())
			return;

		minX -= x;
		maxX += x;
		minY -= y;
		maxY += y;
		minZ -= z;
		maxZ += z;
	}

	public boolean intersects(BoundingBox b) {
		assert valid && b.valid;
		return !(maxX < b.minX || minX > b.maxX || maxY < b.minY || minY > b.maxY || maxZ < b.minZ || minZ > b.maxZ);
	}

	public boolean contains(BoundingBox b) {
		assert b.valid;
		return contains(b.minX, b.minY, b.minZ) && contains(b.maxX, b.maxY, b.maxZ);
	}

	public boolean contains(float x, float y, float z) {
		assert valid;
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
	}

	public boolean contains(double x, double y, double z) {
		return contains((float) x, (float) y, (float) z);
	}

	public boolean contains(Vec3 vertex) {
		return contains(vertex.x, vertex.y, vertex.z);
	}

	public boolean intersects2D(BoundingBox b) {
		assert valid && b.valid;
		return !(maxX < b.minX || minX > b.maxX || maxY < b.minY || minY > b.maxY);
	}

	public boolean contains2D(BoundingBox b) {
		assert b.valid;
		return contains2D(b.minX, b.minY) && contains2D(b.maxX, b.maxY);
	}

	public boolean contains2D(float x, float y) {
		assert valid;
		return x >= minX && x <= maxX && y >= minY && y <= maxY;
	}

	public boolean contains2D(double x, double y) {
		return contains2D((float) x, (float) y);
	}

	public boolean contains2D(Vec3 vertex) {
		return contains2D(vertex.x, vertex.y);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof BoundingBox))
			return false;
		BoundingBox b = (BoundingBox) obj;
		return minX == b.minX && maxX == b.maxX && minY == b.minY && maxY == b.maxY && minZ == b.minZ && maxZ == b.maxZ;
	}

	@Override
	public int hashCode() {
		return HashUtilities.hash(minX, maxX, minY, maxY, minZ, maxZ);
	}

	@Override
	public String toString() {
		return valid ? "[" + minX + "," + maxX + "][" + minY + "," + maxY + "][" + minZ + "," + maxZ + "]" : "invalid";
	}
}
