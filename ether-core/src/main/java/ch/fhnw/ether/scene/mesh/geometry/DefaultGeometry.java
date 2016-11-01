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

import java.util.Arrays;

import ch.fhnw.util.FloatList;
import ch.fhnw.util.math.Vec3;

// note: position is always expected as first attribute
public final class DefaultGeometry extends AbstractGeometry {

	private final IGeometryAttribute[] attributes;
	private final float[][] data;

	/**
	 * Generates geometry from the given data with the given attribute-layout.
	 * All data is copied. Changes on the passed arrays will not affect this
	 * geometry.
	 * 
	 * @param type
	 *            Primitive type of this geometry (points, lines, triangles)
	 * 
	 * @param attributes
	 *            Kind of attributes, must be same order as attribData
	 * 
	 * @param data
	 *            Vertex data, may contain positions, colors, normals, etc.
	 */
	public DefaultGeometry(IGeometryAttribute[] attributes, float[][] data) {
		this.attributes = Arrays.copyOf(attributes, attributes.length);
		this.data = new float[data.length][];
		for (int i = 0; i < data.length; ++i)
			this.data[i] = Arrays.copyOf(data[i], data[i].length);
		checkAttributeConsistency(attributes, this.data);
	}
	
	public DefaultGeometry(IGeometryAttribute[] attributes, FloatList[] data) {
		this.attributes = Arrays.copyOf(attributes, attributes.length);
		this.data = new float[data.length][];
		for (int i = 0; i < data.length; ++i)
			this.data[i] = data[i].toArray(); // FloatList.toArray returns a copy
		checkAttributeConsistency(attributes, this.data);
	}
	
	private DefaultGeometry(DefaultGeometry g) {
		attributes = g.attributes;
		this.data = new float[g.data.length][];
		for (int i = 0; i < g.data.length; ++i)
			this.data[i] = Arrays.copyOf(g.data[i], g.data[i].length);
	}

	/**
	 * Create copy of this geometry.
	 * 
	 * @return the copy
	 */
	public DefaultGeometry copy() {
		return new DefaultGeometry(this);
	}
	
	@Override
	public IGeometryAttribute[] getAttributes() {
		return attributes;
	}
	
	@Override
	public float[][] getData() {
		return data;
	}
	
	@Override
	public void inspect(int index, IAttributeVisitor visitor) {
		visitor.visit(attributes[index], data[index]);
	}

	@Override
	public void inspect(IAttributesVisitor visitor) {
		visitor.visit(attributes, data);
	}

	@Override
	public void modify(int index, IAttributeVisitor visitor) {
		visitor.visit(attributes[index], data[index]);
		updateRequest();
	}

	@Override
	public void modify(IAttributesVisitor visitor) {
		visitor.visit(attributes, data);
		checkAttributeConsistency(attributes, data);
		updateRequest();
	}

	private static void checkAttributeConsistency(IGeometryAttribute[] attributes, float[][] data) {
		// check basic setup
		if (attributes[0] != POSITION_ARRAY)
			throw new IllegalArgumentException("first attribute must be position");
		if (attributes.length != data.length)
			throw new IllegalArgumentException("# attribute types != # attribute data");

		// check for correct individual lengths
		for (int i = 0; i < attributes.length; ++i) {
			if (data[i].length % attributes[i].getNumComponents() != 0)
				throw new IllegalArgumentException(attributes[i].id() + ": size " + data[i].length + " is not a multiple of attribute size " + attributes[i].getNumComponents());
		}

		// check for correct overall lengths
		int numElements = data[0].length / attributes[0].getNumComponents();
		for (int i = 1; i < attributes.length; ++i) {
			int ne = data[i].length / attributes[i].getNumComponents();
			if (ne != numElements)
				throw new IllegalArgumentException(attributes[i].id() + ": size " + ne + " does not match size of position attribute (" + numElements + ")");
		}
	}

	// ---- static helpers for simple geometry creation from arrays

	public static DefaultGeometry createV(float[] vertices) {
		IGeometryAttribute[] attributes = { POSITION_ARRAY };
		float[][] data = { vertices };
		return new DefaultGeometry(attributes, data);
	}

	public static DefaultGeometry createVN(float[] vertices, float[] normals) {
		normals = checkNormals(vertices, normals);
		IGeometryAttribute[] attributes = { POSITION_ARRAY, NORMAL_ARRAY };
		float[][] data = { vertices, normals };
		return new DefaultGeometry(attributes, data);
	}

	public static DefaultGeometry createVC(float[] vertices, float[] colors) {
		IGeometryAttribute[] attributes = { POSITION_ARRAY, COLOR_ARRAY };
		float[][] data = { vertices, colors };
		return new DefaultGeometry(attributes, data);
	}

	public static DefaultGeometry createVM(float[] vertices, float[] texCoords) {
		IGeometryAttribute[] attributes = { POSITION_ARRAY, COLOR_MAP_ARRAY };
		float[][] data = { vertices, texCoords };
		return new DefaultGeometry(attributes, data);
	}

	public static DefaultGeometry createVNC(float[] vertices, float[] normals, float[] colors) {
		normals = checkNormals(vertices, normals);
		IGeometryAttribute[] attributes = { POSITION_ARRAY, NORMAL_ARRAY, COLOR_ARRAY };
		float[][] data = { vertices, normals, colors };
		return new DefaultGeometry(attributes, data);
	}

	public static DefaultGeometry createVNM(float[] vertices, float[] normals, float[] texCoords) {
		normals = checkNormals(vertices, normals);
		IGeometryAttribute[] attributes = { POSITION_ARRAY, NORMAL_ARRAY, COLOR_MAP_ARRAY };
		float[][] data = { vertices, normals, texCoords };
		return new DefaultGeometry(attributes, data);
	}

	public static DefaultGeometry createVCM(float[] vertices, float[] colors, float[] texCoords) {
		IGeometryAttribute[] attributes = { POSITION_ARRAY, COLOR_ARRAY, COLOR_MAP_ARRAY };
		float[][] data = { vertices, colors, texCoords };
		return new DefaultGeometry(attributes, data);
	}

	public static DefaultGeometry createVNCM(float[] vertices, float[] normals, float[] colors, float[] texCoords) {
		normals = checkNormals(vertices, normals);
		IGeometryAttribute[] attributes = { POSITION_ARRAY, NORMAL_ARRAY, COLOR_ARRAY, COLOR_MAP_ARRAY };
		float[][] data = { vertices, normals, colors, texCoords };
		return new DefaultGeometry(attributes, data);
	}
	
	private static float[] checkNormals(float[] vertices, float[] normals) {
		if (normals == null)
			return IGeometry.createNormals(vertices);
		if (normals.length == 3)
			return IGeometry.createNormals(vertices.length / 3, new Vec3(normals[0], normals[1], normals[2]));
		return normals;
	}
}
