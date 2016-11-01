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
import java.util.List;

import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec3;

// #faces = 20 * f^2
// #edges = 30 * f^2
// #point = 10 * f^2 + 2

// FIXME: we still create too many points when vertex coherence is not set

/**
 * Generator class for geodesic sphere. Points / lines / triangles are generated
 * starting from a icosahedron, with given iteration depth.
 * 
 * @author radar
 *
 */
public class GeodesicSphere {
	private static final float SCALE = 0.5f;

	private static final double PI2 = Math.PI / 2;
	private static final double PI5 = Math.PI / 5;

	private static final double W = 2.0 * Math.acos(1.0 / (2.0 * Math.sin(PI5)));

	private static final double[] THETAS = { 0, -PI5, PI5, 3 * PI5, 5 * PI5, -3 * PI5, 0, 2 * PI5, 4 * PI5, -4 * PI5, -2 * PI5, 0 };
	private static final double[] PHIS = { PI2, PI2 - W, PI2 - W, PI2 - W, PI2 - W, PI2 - W, W - PI2, W - PI2, W - PI2, W - PI2, W - PI2, -PI2 };

	private static final Vec3[] VERTICES = makeVertices(THETAS, PHIS);

	//@formatter:off
    private static final int[][] INDICES = {
        { 0, 1, 2 }, { 0, 2, 3 }, { 0, 3, 4 }, { 0, 4, 5 }, { 0, 5, 1 },
        { 1, 6, 2 }, { 2, 7, 3 }, { 3, 8, 4 }, { 4, 9, 5 }, { 5, 10, 1 },
        
        { 1, 10, 6 }, { 2, 6, 7 }, { 3, 7, 8 }, { 4, 8, 9 }, { 5, 9, 10 },
        { 11, 7, 6 }, { 11, 8, 7 }, { 11, 9, 8 }, { 11, 10, 9 }, { 11, 6, 10 }
    };
    
    private static final boolean[][] LFLAGS = {
    	{ true,  true,  false }, { true,  true,  false }, { true,  true,  false }, { true,  true,  false }, { true,  true,  false }, 
    	{ true,  true,  false }, { true,  true,  false }, { true,  true,  false }, { true,  true,  false }, { true,  true,  false }, 
    	
    	{ false, true,  false }, { false, true,  false }, { false, true,  false }, { false, true,  false }, { false, true,  false }, 
    	{ true,  false, false }, { true,  false, false }, { true,  false, false }, { true,  false, false }, { true,  false, false }, 
    };

    private static final boolean[][] PFLAGS = {
    	{ true,  true,  false }, { false, true,  false }, { false, true,  false }, { false, true,  false }, { false, true,  false }, 
    	{ false, false, false }, { false, false, false }, { false, false, false }, { false, false, false }, { false, false, false },
    	
    	{ false, false, false }, { false, false, false }, { false, false, false }, { false, false, false }, { false, false, false },
    	{ true,  false, true  }, { false, false, true  }, { false, false, true  }, { false, false, true  }, { false, false, true  },
    };
    //@formatter:on

	private static Vec3[] makeVertices(double[] thetas, double[] phis) {
		Vec3[] vertices = new Vec3[thetas.length];
		for (int i = 0; i < vertices.length; ++i) {
			vertices[i] = sphericalToCartesian(SCALE, thetas[i], phis[i]);
		}
		return vertices;
	}

	private static Vec3 sphericalToCartesian(double r, double theta, double phi) {
		double x = r * Math.cos(phi) * Math.cos(theta);
		double y = r * Math.cos(phi) * Math.sin(theta);
		double z = r * Math.sin(phi);
		return new Vec3(x, y, z);
	}


	private final int depth;
	private final boolean vertexCoherence;

	private float[] points;
	private float[] lines;
	private float[] triangles;
	private float[] normals;
	private float[] texCoords;

	public GeodesicSphere(int depth) {
		this(depth, false);
	}

	public GeodesicSphere(int depth, boolean vertexCoherence) {
		this.depth = depth;
		this.vertexCoherence = vertexCoherence;
	}

	public float[] getPoints() {
		if (points == null) {
			List<Vec3> vertices = new ArrayList<>();

			if (vertexCoherence) {
				for (int i = 0; i < 20; i++)
					subdividePoints(VERTICES[INDICES[i][0]], VERTICES[INDICES[i][1]], VERTICES[INDICES[i][2]], vertices, true, true, true, depth);
			} else {
				for (int i = 0; i < 20; i++)
					subdividePoints(VERTICES[INDICES[i][0]], VERTICES[INDICES[i][1]], VERTICES[INDICES[i][2]], vertices, PFLAGS[i][0], PFLAGS[i][1],
							PFLAGS[i][2], depth);
			}
			System.out.println("# points:" + vertices.size());

			points = Vec3.toArray(vertices);
		}
		return points;
	}

	public float[] getLines() {
		if (lines == null) {
			List<Vec3> vertices = new ArrayList<>();

			if (vertexCoherence) {
				for (int i = 0; i < 20; i++)
					subdivideLines(VERTICES[INDICES[i][0]], VERTICES[INDICES[i][1]], VERTICES[INDICES[i][2]], vertices, true, true, true, depth);
			} else {
				for (int i = 0; i < 20; i++)
					subdivideLines(VERTICES[INDICES[i][0]], VERTICES[INDICES[i][1]], VERTICES[INDICES[i][2]], vertices, LFLAGS[i][0], LFLAGS[i][1],
							LFLAGS[i][2], depth);
			}
			System.out.println("# lines:" + vertices.size() / 2);

			lines = Vec3.toArray(vertices);
		}
		return lines;
	}

	public float[] getTriangles() {
		if (triangles == null) {
			List<Vec3> vertices = new ArrayList<>();

			for (int i = 0; i < 20; i++)
				subdivideTriangles(VERTICES[INDICES[i][0]], VERTICES[INDICES[i][1]], VERTICES[INDICES[i][2]], vertices, depth);

			System.out.println("# triangles:" + vertices.size() / 3);

			triangles = Vec3.toArray(vertices);
		}
		return triangles;
	}

	public float[] getNormals() {
		if (normals == null) {
			normals = getTriangles().clone();
			for (int i = 0; i < normals.length; i += 3) {
				float l = MathUtilities.length(normals[i], normals[i + 1], normals[i + 2]);
				normals[i] /= l;
				normals[i + 1] /= l;
				normals[i + 2] /= l;
			}
		}
		return normals;
	}

	public float[] getTexCoords() {
		if (texCoords == null) {
			float[] vertices = getTriangles();
			float r = SCALE;
			texCoords = new float[vertices.length / 3 * 2];
			int j = 0;
			for (int i = 0; i < vertices.length;) {
				float x0 = vertices[i++];
				float y0 = vertices[i++];
				float z0 = vertices[i++];
				float s0 = toS(x0, y0);
				float t0 = toT(z0, r);
				float x1 = vertices[i++];
				float y1 = vertices[i++];
				float z1 = vertices[i++];
				float s1 = toS(x1, y1);
				float t1 = toT(z1, r);
				float x2 = vertices[i++];
				float y2 = vertices[i++];
				float z2 = vertices[i++];
				float s2 = toS(x2, y2);
				float t2 = toT(z2, r);

				boolean p0 = Math.abs(z0) == SCALE;
				boolean p1 = Math.abs(z1) == SCALE;
				boolean p2 = Math.abs(z2) == SCALE;
				
				if (p0) {
					if (s1 < s2 - 0.5f)
						s1 += 1;
					else if (s1 > s2 + 0.5f)
						s1 -= 1;
					s0 = (s1 + s2) / 2;
				} else if (p1) {
					if (s0 < s2 - 0.5f)
						s0 += 1;
					else if (s0 > s2 + 0.5f)
						s0 -= 1;
					s1 = (s0 + s2) / 2;
				} else if (p2) {
					if (s0 < s1 - 0.5f)
						s0 += 1;
					else if (s0 > s1 + 0.5f)
						s0 -= 1;
					s2 = (s0 + s1) / 2;
				} else {
					if (s1 < s0 - 0.5f)
						s1 += 1;
					else if (s1 > s0 + 0.5f)
						s1 -= 1;
					if (s2 < s0 - 0.5f)
						s2 += 1;
					else if (s2 > s0 + 0.5f)
						s2 -= 1;

				}
				
				texCoords[j++] = s0;
				texCoords[j++] = t0;
				texCoords[j++] = s1;
				texCoords[j++] = t1;
				texCoords[j++] = s2;
				texCoords[j++] = t2;
			}
		}
		return texCoords;
	}

	private float toS(float x, float y) {
		float s = (float) (0.5 * Math.atan2(y, x) / Math.PI);
		if (s < 0)
			s += 1;
		return s;
	}

	private float toT(float z, float r) {
		return 1.0f - (float) (Math.acos(z / r) / Math.PI);
	}

	private void subdividePoints(Vec3 v1, Vec3 v2, Vec3 v3, List<Vec3> vertices, boolean d1, boolean d2, boolean d3, int depth) {
		if (depth == 0) {
			if (d1)
				vertices.add(v1);
			if (d2)
				vertices.add(v2);
			if (d3)
				vertices.add(v3);
			return;
		}
		Vec3 v12 = v1.add(v2).normalize().scale(SCALE);
		Vec3 v23 = v2.add(v3).normalize().scale(SCALE);
		Vec3 v31 = v3.add(v1).normalize().scale(SCALE);
		subdividePoints(v1, v12, v31, vertices, d1, true, false, depth - 1);
		subdividePoints(v2, v23, v12, vertices, d2, true, false, depth - 1);
		subdividePoints(v3, v31, v23, vertices, d3, false, false, depth - 1);
		subdividePoints(v12, v23, v31, vertices, false, false, false, depth - 1);
	}

	private void subdivideLines(Vec3 v1, Vec3 v2, Vec3 v3, List<Vec3> vertices, boolean d1, boolean d2, boolean d3, int depth) {
		if (depth == 0) {
			if (d1) {
				vertices.add(v1);
				vertices.add(v2);
			}
			if (d2) {
				vertices.add(v2);
				vertices.add(v3);
			}
			if (d3) {
				vertices.add(v3);
				vertices.add(v1);
			}
			return;
		}
		Vec3 v12 = v1.add(v2).normalize().scale(SCALE);
		Vec3 v23 = v2.add(v3).normalize().scale(SCALE);
		Vec3 v31 = v3.add(v1).normalize().scale(SCALE);
		subdivideLines(v1, v12, v31, vertices, d1, true, d3, depth - 1);
		subdivideLines(v2, v23, v12, vertices, d2, true, d1, depth - 1);
		subdivideLines(v3, v31, v23, vertices, d3, true, d2, depth - 1);
		subdivideLines(v12, v23, v31, vertices, false, false, false, depth - 1);
	}

	private void subdivideTriangles(Vec3 v1, Vec3 v2, Vec3 v3, List<Vec3> vertices, int depth) {
		if (depth == 0) {
			vertices.add(v1);
			vertices.add(v2);
			vertices.add(v3);
			return;
		}
		Vec3 v12 = v1.add(v2).normalize().scale(SCALE);
		Vec3 v23 = v2.add(v3).normalize().scale(SCALE);
		Vec3 v31 = v3.add(v1).normalize().scale(SCALE);
		subdivideTriangles(v1, v12, v31, vertices, depth - 1);
		subdivideTriangles(v2, v23, v12, vertices, depth - 1);
		subdivideTriangles(v3, v31, v23, vertices, depth - 1);
		subdivideTriangles(v12, v23, v31, vertices, depth - 1);
	}
}
