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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.fhnw.util.Pair;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.tessellator.Triangulation;

/**
 * Polygon class based on a list of vertices. Supports concave polygons, except
 * for clip / split, which has limitatinos for concave polygons. Immutable.
 */
public final class Polygon {
	private final Vec3[] vertices;
	private final Vec3 normal;
	private float[] triangles;

	/**
	 * Create new polygon from array of vertices.
	 */
	public Polygon(Vec3... vertices) {
		if (vertices.length < 3)
			throw new IllegalArgumentException("polygon requires at least 3 vertices");
		this.vertices = Arrays.copyOf(vertices, vertices.length);
		this.normal = calculateNormal(this.vertices);
	}

	/**
	 * Create new polygon from list of vertices.
	 */
	public Polygon(Collection<Vec3> vertices) {
		this(vertices.toArray(new Vec3[vertices.size()]));
	}

	private Polygon(Vec3[] vertices, Vec3 normal, float[] triangles) {
		this.vertices = vertices;
		this.normal = normal;
		this.triangles = triangles;
	}

	/**
	 * Returns true if polygon is convex, false otherwise.
	 */
	public boolean isConvex() {
		for (int i = 0; i < vertices.length; ++i) {
			Vec3 v0 = get(i);
			Vec3 v1 = get(i + 1);
			Vec3 v2 = get(i + 2);
			Vec3 c = v1.subtract(v0).cross(v2.subtract(v1));
			if (normal.dot(c) <= 0)
				return false;
		}
		return true;
	}

	/**
	 * Returns true if polygon is planar, false otherwise.
	 */
	public boolean isPlanar() {
		Plane plane = getPlane();
		for (Vec3 v : vertices) {
			if (!MathUtilities.isZero(plane.distance(v)))
				return false;
		}
		return true;
	}

	/**
	 * Get number of vertices in this polygon.
	 */
	public int getNumVertices() {
		return vertices.length;
	}

	/**
	 * Get polygon vertex at given index.
	 */
	public Vec3 get(int index) {
		index = Math.floorMod(index, vertices.length);
		return vertices[index];
	}
	
	/**
	 * Get polygon normal.
	 */
	public Vec3 getNormal() {
		return normal;
	}

	/**
	 * Get normalized U axis, based on polygon edge 0.
	 */
	public Vec3 getU() {
		return vertices[1].subtract(vertices[0]).normalize();
	}

	/**
	 * Get normalized V axis, based on polygon edge 0 (V = N x U).
	 */
	public Vec3 getV() {
		return normal.cross(getU()).normalize();
	}

	/**
	 * Get normalized W axis (same as polygon normal).
	 */
	public Vec3 getW() {
		return normal;
	}
	
	/**
	 * Get basis of this polygon, based on V0 and UVW.
	 */
	public Basis getBasis() {
		return new Basis(getPlane(), vertices[1]);
	}

	/**
	 * Get vertex on polygon edge of given interval t = [0, 1]
	 */
	public Vec3 getVertexOnEdge(int index, float t) {
		Vec3 v0 = get(index);
		Vec3 v1 = get(index + 1);
		return v0.add(v1.subtract(v0).scale(t));
	}

	/**
	 * Get line of given polygon edge.
	 */
	public Line getLine(int index) {
		return new Line(get(index), get(index + 1));
	}

	/**
	 * Get polygon plane.
	 */
	public Plane getPlane() {
		return new Plane(vertices[0], normal);
	}

	/**
	 * Get extent of polygon with respect to given polygon edge.
	 */
	public float getExtent(int index) {
		return getExtent(get(index + 1).subtract(get(index)));
	}

	/**
	 * Get extent of polygon with respect to U axis (edge 0).
	 */
	public float getExtentU() {
		return getExtent(0);
	}

	/**
	 * Get extent of polygon with respect to V axis (V = N x U).
	 */
	public float getExtentV() {
		return getExtent(getV());
	}
	
	/**
	 * Get center of UV extent rectangle.
	 */
	public Vec3 getCenter() {
		return vertices[0].add(getU().scale(getExtentU() * 0.5f)).add(getV().scale(getExtentV() * 0.5f));
	}
	
	/**
	 * Returns the closest polygon vertex with respect to a given vertex. Note:
	 * Does not check whether vertex is within polygon.
	 */
	public int getClosestVertex(Vec3 v) {
		float d = Float.POSITIVE_INFINITY;
		int index = 0;
		for (int i = 0; i < vertices.length; ++i) {
			float dd = vertices[i].distance(v);
			if (dd < d) {
				d = dd;
				index = i;
			}
		}
		return index;
	}

	/**
	 * Returns the closest polygon vertex with respect to a given vertex. Note:
	 * Does not check whether vertex is within polygon.
	 */
	public int getClosestEdge(Vec3 v) {
		float d = Float.POSITIVE_INFINITY;
		int index = 0;
		for (int i = 0; i < vertices.length; ++i) {
			Line line = getLine(i);
			float dd = line.distance(v);
			if (dd < d) {
				d = dd;
				index = i;
			}
		}
		return index;
	}

	/**
	 * Returns new polygon with reversed orientation. Vertex index 0 of new
	 * polygon is same as vertex index 0 of old polygon.
	 */
	public Polygon flip() {
		Vec3[] v = new Vec3[vertices.length];
		for (int i = 0; i < vertices.length; ++i)
			v[(vertices.length - i) % vertices.length] = vertices[i];
		return new Polygon(v, normal.negate(), null);
	}

	/**
	 * Rotates polygon indices so that provided index becomes index 0.
	 */
	public Polygon rotate(int index) {
		Vec3[] v = new Vec3[vertices.length];
		for (int i = 0; i < vertices.length; ++i)
			v[i] = get(i + index);
		return new Polygon(v, normal, null);
	}
	
	/**
	 * Projects a vertex onto the polygon and returns projected vertex or null
	 * if projection is outside polygon.
	 */
	public Vec3 project(Vec3 v) {
		return intersect(Line.fromRay(v, normal));
	}
	
	/**
	 * Intersects this polygon with a given line.
	 */
	public Vec3 intersect(Line line) {
		Vec3 o = line.getOrigin();
		Vec3 d = line.getDirection();
		float[] triangles = getTriangleVertices();
		for (int i = 0; i < triangles.length; i += 9) {
			float t = GeometryUtilities.intersectRayWithTriangle(o, d, triangles, i);
			if (t < Float.POSITIVE_INFINITY)
				return o.add(d.scale(t));
		}
		return null;
	}
	
	/**
	 * Returns a new polygon, with all vertices transformed by matrix m.
	 */
	public Polygon transform(Mat4 m) {
		Vec3[] v = new Vec3[vertices.length];
		for (int i = 0; i < vertices.length; ++i)
			v[i] = m.transform(vertices[i]);
		Vec3 n = calculateNormal(v);
		float[] t = triangles != null ? m.transform(triangles) : null;
		return new Polygon(v, n, t);
	}

	/**
	 * Clips this polygon with respect to provided plane. Vertices behind plane
	 * are clipped. First edge of returned polygon will be same as of original
	 * polygon, or will be set to the clipping line if clipped. Applies the
	 * Sutherland-Hodman algorithm for clipping, which has the usual limitations
	 * for concave polygons. Returns null if polygon is completely behind plane.
	 */
	public Polygon clip(Plane plane) {
		List<Vec3> front = new ArrayList<>(vertices.length);

		float d0 = plane.distance(vertices[vertices.length - 1]);
		float d1 = plane.distance(vertices[0]);
		int startIndex = (d0 <= 0 && d1 > 0) ? 1 : 0;		
		
		Vec3 startVertex = vertices[vertices.length - 1];
		float startDistance = plane.distance(startVertex);
		for (int i = 0; i < vertices.length; ++i) {
			Vec3 endVertex = vertices[i];
			float endDistance = plane.distance(endVertex);

			Vec3 intersection = (startDistance * endDistance <= 0) ? plane.intersect(new Line(startVertex, endVertex)) : null;

			if (endDistance > 0) {
				if (startDistance <= 0)
					front.add(intersection);
				front.add(endVertex);
			} else if (startDistance > 0) {
				front.add(intersection);
			}

			startVertex = endVertex;
			startDistance = plane.distance(startVertex);
		}
		if (front.size() < 3)
			return null;

		// need to rotate edges
		Vec3[] v = new Vec3[front.size()];
		for (int i = 0; i < v.length; ++i)
			v[i] = front.get((i + startIndex) % v.length);
		return new Polygon(v, normal, null);
	}
	
	/**
	 * Clips polygon from line parallel to edge with given index and at given distance.
	 */
	public Polygon clipFrom(int index, float distance) {
		return clip(getPlane(index, distance, true));
	}
	
	/**
	 * Clips polygon to line parallel to edge with given index and at given distance.
	 */
	public Polygon clipTo(int index, float distance) {
		return clip(getPlane(index, distance, false));
	}

	/**
	 * Splits this polygon with respect to provided plane and returns a pair of
	 * polygons, where pair.first contains the split in front of plane, and
	 * pair.second contains the split behind the plane, or null for first /
	 * second if polygon completely behind or in front of plane. First edge is
	 * preserved if possible, otherwise set to the clipping line. Applies the
	 * Sutherland-Hodman algorithm for splitting, which has the usual
	 * limitations for concave polygons.
	 */
	public Pair<Polygon, Polygon> split(Plane plane) {
		return new Pair<>(clip(plane), clip(plane.reverse()));
	}

	/**
	 * Splits this polygon with respect to given edge index and distance from
	 * edge. Pair.first will contain the split from edge to distance, pair.second will
	 * contain split away from distance.
	 */
	public Pair<Polygon, Polygon> split(int index, float distance) {
		return split(getPlane(index, distance, true));
	}
	
	/**
	 * Returns a polygon that is offset from original polygon. Only works
	 * properly for convex polygons.
	 */
	public Polygon offset(float distance) {
		if (distance > 0) {
			int n = getNumVertices();
			List<Plane> planes = new ArrayList<>(n);
			for (int i = 0; i < n; ++i)
				planes.add(getPlane(i, distance, false));
			Polygon p = this;
			for (int i = 0; i < getNumVertices(); ++i) {
				p = p.clip(planes.get(i));
				if (p == null)
					return null;
			}
			return p;
		} else if (distance < 0) {
			Vec3[] v = new Vec3[vertices.length];
			for (int i = 0; i < vertices.length; ++i) {
				Plane plane = getPlane(i, distance, true);
				Vec3 v0 = get(i);
				Vec3 n0 = get(i - 1).subtract(v0).normalize();
				Vec3 n1 = get(i + 1).subtract(v0).normalize();
				Vec3 v1 = v0.add(n0).add(n1);
				Line l = new Line(v0, v1);
				v[i] = plane.intersect(l);
			}
			return new Polygon(v, normal, null);
		}
		return this;
	}

	/**
	 * Extrudes this polygon along the normal by distance d, and returns list of
	 * new polygons. Does not add any caps.
	 */
	public List<Polygon> extrude(float d) {
		return extrude(d, false, false);
	}

	/**
	 * Extrudes this polygon along the normal by distance d, and returns list of
	 * new polygons. Edges of original polygon will become first edges of each
	 * side polygon. Adds bottom cap (= this polygon, reversed) and top cap (=
	 * this polygon, translated) if requested. Bottom cap will be added as first
	 * element in the list, top cap as last element.
	 */
	public List<Polygon> extrude(float d, boolean addBottomCap, boolean addTopCap) {
		return extrude(normal, d, addBottomCap, addTopCap);
	}

	/**
	 * Extrudes this polygon along given direction by distance d, and returns list of
	 * new polygons. Edges of original polygon will become first edges of each
	 * side polygon. Adds bottom cap (= this polygon, reversed) and top cap (=
	 * this polygon, translated) if requested. Bottom cap will be added as first
	 * element in the list, top cap as last element.
	 */
	public List<Polygon> extrude(Vec3 direction, float d, boolean addBottomCap, boolean addTopCap) {
		List<Polygon> p = new ArrayList<>(vertices.length + (addBottomCap ? 1 : 0) + (addTopCap ? 1 : 0));
		if (addBottomCap)
			p.add(flip());
		for (int i = 0; i < vertices.length; ++i) {
			Vec3 v0 = get(i);
			Vec3 v1 = get(i + 1);
			Vec3 v2 = v1.add(direction.scale(d));
			Vec3 v3 = v0.add(direction.scale(d));
			p.add(new Polygon(v0, v1, v2, v3));
		}
		if (addTopCap)
			p.add(transform(Mat4.translate(direction.scale(d))));
		return p;
	}

	/**
	 * Triangulates this polygon and returns triangle array of vertices.
	 * Triangulation is cached, i.e. repeated calls will return immediately.
	 */
	public float[] getTriangleVertices() {
		if (triangles == null) {
			triangles = new float[vertices.length * 3];
			int i = 0;
			for (Vec3 v : vertices) {
				triangles[i++] = v.x;
				triangles[i++] = v.y;
				triangles[i++] = v.z;
			}
			triangles = Triangulation.triangulateAndExpand(triangles);
		}
		return triangles;
	}
	
	/**
	 * Get an unmodifiable list of the polygon's vertices.
	 */
	public List<Vec3> asList() {
		return Collections.unmodifiableList(Arrays.asList(vertices));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Polygon))
			return false;
		Polygon p = (Polygon) obj;
		return Arrays.equals(vertices, p.vertices);
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		for (Vec3 v : vertices)
			hash += v.hashCode();
		return hash;
	}

	@Override
	public String toString() {
		String result = "[ ";
		for (Vec3 v : vertices)
			result += v;
		return result + " ] [ " + normal + " ]";
	}

	private float getExtent(Vec3 n) {
		Plane p = new Plane(n);
		float min = Float.POSITIVE_INFINITY;
		float max = Float.NEGATIVE_INFINITY;
		for (Vec3 v : vertices) {
			float d = p.distance(v);
			min = Math.min(d, min);
			max = Math.max(d, max);
		}
		return max - min;
	}
	
	private Plane getPlane(int index, float distance, boolean front) {
		Vec3 n = normal.cross(get(index + 1).subtract(get(index))).normalize();
		Vec3 o = get(index).add(n.scale(distance));
		return new Plane(o, front ? n.negate() : n);
	}

	private static Vec3 calculateNormal(Vec3[] vertices) {
		// newell's method
		float nx = 0;
		float ny = 0;
		float nz = 0;
		for (int i = 0; i < vertices.length; ++i) {
			Vec3 v0 = vertices[i];
			Vec3 v1 = vertices[(i + 1) % vertices.length];
			nx += (v0.y - v1.y) * (v0.z + v1.z);
			ny += (v0.z - v1.z) * (v0.x + v1.x);
			nz += (v0.x - v1.x) * (v0.y + v1.y);
		}
		float l = MathUtilities.length(nx, ny, nz);
		return new Vec3(nx / l, ny / l, nz / l);
	}
}
