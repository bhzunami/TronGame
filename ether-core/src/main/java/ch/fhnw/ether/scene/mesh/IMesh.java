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

package ch.fhnw.ether.scene.mesh;

import java.util.EnumSet;
import java.util.Set;

import ch.fhnw.ether.scene.I3DObject;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.IMaterial;
import ch.fhnw.util.math.Mat4;

/**
 * Basic mesh abstraction. A mesh is a light weight structure that combines
 * render queue (= pass), flags, material, geometry, and a transform.
 * 
 * @author radar
 */
public interface IMesh extends I3DObject {

	// TODO: extend to concept of "queues" with a more flexible and extensible scheme
	enum Queue {
		DEPTH, TRANSPARENCY, POST, OVERLAY, DEVICE_SPACE_OVERLAY, SCREEN_SPACE_OVERLAY
	}

	enum Flag {
		DONT_CULL_FACE,
		DONT_CAST_SHADOW
	}
	
	enum Primitive {
		POINTS("points", 1), 
		LINES("lines", 2), 
		LINE_STRIP("line_strip", 1),
		LINE_LOOP("line_loop", 1),
		TRIANGLES("triangles", 3),
		TRIANGLE_STRIP("triangle_strip", 1),
		TRIANGLE_FAN("triangle_fan", 1);
		
		private final String name;
		private final int numVertices;
		
		Primitive(String name, int numVertices) {
			this.name = name;
			this.numVertices = numVertices;
		}
		
		public String getName() {
			return name;
		}
		
		public int getNumVertices() {
			return numVertices;
		}
		
		@Override
		public String toString() {
			return getName();
		}
	}	

	EnumSet<Flag> NO_FLAGS = EnumSet.noneOf(Flag.class);
	
	/**
	 * Return a new instance of this mesh, that is, a shallow copy of material
	 * and geometry, but with independent position and transform.
	 */
	IMesh createInstance();

	/**
	 * Get this mesh's queue.
	 */
	Queue getQueue();

	/**
	 * Get an immutable set of this mesh's flags.
	 */
	Set<Flag> getFlags();

	/**
	 * Test this mesh for the specified flag.
	 */
	boolean hasFlag(Flag flag);
	
	/**
	 * Get this mesh's primitive type.
	 */
	Primitive getType();

	/**
	 * Get this mesh's material.
	 */
	IMaterial getMaterial();

	/**
	 * Get this mesh's geometry.
	 */
	IGeometry getGeometry();

	/**
	 * Get this mesh's transform.
	 */
	Mat4 getTransform();

	/**
	 * Set this mesh's transform.
	 */
	void setTransform(Mat4 transform);
	
	/**
	 * Get a copy of the transformed position data (position * transform).
	 */
	float[] getTransformedPositionData();

	/**
	 * Get a copy of the transformed geometry data (positions and normals
	 * transformed, all other attributes copied).
	 * 
	 * @return
	 */
	float[][] getTransformedGeometryData();
	
	/**
	 * Get the number of primitives in this geometry.
	 */
	int getNumPrimitives();
}
