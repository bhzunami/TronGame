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

package ch.fhnw.ether.scene.mesh.material;

import ch.fhnw.ether.image.IGPUTexture;
import ch.fhnw.ether.scene.attribute.AbstractAttribute;
import ch.fhnw.ether.scene.attribute.ITypedAttribute;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry.IGeometryAttribute;
import ch.fhnw.util.UpdateRequest.IUpdateTracker;
import ch.fhnw.util.math.IVec3;
import ch.fhnw.util.math.IVec4;

public interface IMaterial extends IUpdateTracker {
	interface IMaterialAttribute<T> extends ITypedAttribute<T> {
	}

	final class MaterialAttribute<T> extends AbstractAttribute<T> implements IMaterialAttribute<T> {
		public MaterialAttribute(String id) {
			super(id);
		}
	}

	// default material attributes

	// non-shaded objects
	MaterialAttribute<IVec4> COLOR = new MaterialAttribute<>("builtin.material.color");

	// texture maps
	MaterialAttribute<IGPUTexture> COLOR_MAP = new MaterialAttribute<>("builtin.material.color_map");

	// triangles only: normals & shading
	MaterialAttribute<IVec3> EMISSION = new MaterialAttribute<>("builtin.material.shading.emission");
	MaterialAttribute<IVec3> AMBIENT = new MaterialAttribute<>("builtin.material.shading.ambient");
	MaterialAttribute<IVec3> DIFFUSE = new MaterialAttribute<>("builtin.material.shading.diffuse");
	MaterialAttribute<IVec3> SPECULAR = new MaterialAttribute<>("builtin.material.shading.specular");
	MaterialAttribute<Float> SHININESS = new MaterialAttribute<>("builtin.material.shading.shininess");
	MaterialAttribute<Float> STRENGTH = new MaterialAttribute<>("builtin.material.shading.strength");
	MaterialAttribute<Float> ALPHA = new MaterialAttribute<>("builtin.material.shading.alpha");

	// lines only: line width
	MaterialAttribute<Float> LINE_WIDTH = new MaterialAttribute<>("builtin.material.line_width");

	// points only: point size
	MaterialAttribute<Float> POINT_SIZE = new MaterialAttribute<>("builtin.material.point_size");

	/**
	 * Get material name.
	 */
	String getName();
	
	/**
	 * Set material name.
	 */
	void setName(String name);
	
	/**
	 * Get array of provided material attributes.
	 */
	IMaterialAttribute<?>[] getProvidedAttributes();

	/**
	 * Get array of required geometry attributes.
	 */
	IGeometryAttribute[] getRequiredAttributes();

	/**
	 * Get a copy of the provided material attribute data. 
	 */
	Object[] getData();
}
