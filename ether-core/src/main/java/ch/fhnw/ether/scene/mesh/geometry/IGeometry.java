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

package ch.fhnw.ether.scene.mesh.geometry;

import ch.fhnw.ether.scene.attribute.AbstractAttribute;
import ch.fhnw.ether.scene.attribute.ITypedAttribute;
import ch.fhnw.util.UpdateRequest.IUpdateTracker;
import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec3;

public interface IGeometry extends IUpdateTracker {
	interface IGeometryAttribute extends ITypedAttribute<float[]> {
		int getNumComponents();
	}

	final class GeometryAttribute extends AbstractAttribute<float[]> implements IGeometryAttribute {
		private final int numComponents;

		public GeometryAttribute(String id, int numComponents) {
			super(id);
			this.numComponents = numComponents;
		}

		@Override
		public int getNumComponents() {
			return numComponents;
		}
	}

	@FunctionalInterface
	interface IAttributeVisitor {
		/**
		 * Inspect or modify a specific attribute of a geometry.
		 */
		void visit(IGeometryAttribute attribute, float[] data);
	}

	@FunctionalInterface
	interface IAttributesVisitor {
		/**
		 * Inspect or modify attributes of a geometry through visitor. Note that
		 * in the current implementation, the attributes must not be changed,
		 * otherwise the mesh will result in an undefined state. It is however
		 * ok to replace all attribute data arrays with new arrays, e.g. of
		 * different size.
		 */
		void visit(IGeometryAttribute[] attributes, float[][] data);
	}

	// default geometry attributes

	// position array (note that this attribute is mandatory)
	IGeometryAttribute POSITION_ARRAY = new GeometryAttribute("builtin.material.position_array", 3);

	// non-shaded objects
	IGeometryAttribute COLOR_ARRAY = new GeometryAttribute("builtin.material.color_array", 4);

	// texture maps
	IGeometryAttribute COLOR_MAP_ARRAY = new GeometryAttribute("builtin.material.color_map_array", 2);

	// triangles only: normals & shading
	IGeometryAttribute NORMAL_ARRAY = new GeometryAttribute("builtin.material.normal_array", 3);

	// lines only: line width
	IGeometryAttribute LINE_WIDTH_ARRAY = new GeometryAttribute("builtin.material.line_width_array", 1);

	// points only: point size
	IGeometryAttribute POINT_SIZE_ARRAY = new GeometryAttribute("builtin.material.point_size_array", 1);

	/**
	 * Get attributes this geometry provides. Warning: Does not copy the
	 * internal array, and changes to the array will leave the geometry in
	 * undefined state.
	 */
	IGeometryAttribute[] getAttributes();

	/**
	 * Get data this geometry provides. Warning: Does not copy and returns
	 * internal arrays.
	 */
	float[][] getData();
	
	/**
	 * Inspect specific attribute of this geometry through visitor.
	 * 
	 * @param index
	 *            index of attribute to be visited
	 * @param visitor
	 *            attribute visitor used for inspection
	 */
	void inspect(int index, IAttributeVisitor visitor);

	/**
	 * Inspect all attributes of this geometry through visitor.
	 * 
	 * @param visitor
	 *            attributes visitor used for inspection
	 */
	void inspect(IAttributesVisitor visitor);

	/**
	 * Modify specific attribute of this geometry through visitor.
	 * 
	 * @param index
	 *            index of attribute to be visited
	 * @param visitor
	 *            attribute visitor used for modification
	 * 
	 * @throws UnsupportedOperationException
	 *             if geometry cannot be modified.
	 */
	void modify(int index, IAttributeVisitor visitor);

	/**
	 * Modify all attributes of this geometry through visitor.
	 * 
	 * @param visitor
	 *            attributes visitor used for modification
	 * 
	 * @throws UnsupportedOperationException
	 *             if geometry cannot be modified.
	 */
	void modify(IAttributesVisitor visitor);
	
	
	/**
	 * Create array of normals from given triangles.
	 */
	static float[] createNormals(float[] triangles) {
		float[] normals = new float[triangles.length];
		for (int i = 0; i < triangles.length; i += 9) {
			float ax = triangles[i + 3] - triangles[i];
			float ay = triangles[i + 4] - triangles[i + 1];
			float az = triangles[i + 5] - triangles[i + 2];
			float bx = triangles[i + 6] - triangles[i];
			float by = triangles[i + 7] - triangles[i + 1];
			float bz = triangles[i + 8] - triangles[i + 2];
			
			float nx = ay * bz - az * by;
			float ny = az * bx - ax * bz;
			float nz = ax * by - ay * bx;
			
			float l = MathUtilities.length(nx, ny, nz);
			if (MathUtilities.isZero(l)) {
				nx = 0;
				ny = 0;
				nz = 1;
			} else {
				nx /= l;
				ny /= l;
				nz /= l;
			}
			
			normals[i] = normals[i + 3] = normals[i + 6] = nx;
			normals[i + 1] = normals[i + 4] = normals[i + 7] = ny;
			normals[i + 2] = normals[i + 5] = normals[i + 8] = nz;
		}
		return normals;
	}

	/**
	 * Create array of normals from given triangle count and constant normal.
	 */
	static float[] createNormals(int n, Vec3 normal) {
		float[] normals = new float[n * 3];
		for (int i = 0; i < normals.length;) {
			normals[i++] = normal.x;
			normals[i++] = normal.y;
			normals[i++] = normal.z;
		}
		return normals;
	}
}
