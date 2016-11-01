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

package ch.fhnw.ether.mapping.tool;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import ch.fhnw.util.math.Vec3;

class CalibrationContext {
	boolean calibrated = false;
	int currentSelection = -1;
	List<Vec3> modelVertices = new ArrayList<>();
	List<Vec3> projectedVertices = new ArrayList<>();

	CalibrationContext() {
	}

	void load(Preferences p, int index) {
		byte[] mv = p.getByteArray("modelVertices_" + index, null);
		byte[] pv = p.getByteArray("projectedVertices_" + index, null);
		if (mv != null) {
			modelVertices = fromByteArray(mv);
			projectedVertices = fromByteArray(pv);
			calibrated = true;
		} else {
			modelVertices = new ArrayList<>();
			projectedVertices = new ArrayList<>();
			calibrated = false;
		}
	}

	void save(Preferences p, int index) {
		if (calibrated) {
			p.putByteArray("modelVertices_" + index, toByteArray(modelVertices));
			p.putByteArray("projectedVertices_" + index, toByteArray(projectedVertices));
		} else {
			p.remove("modelVertices_" + index);
			p.remove("projectedVertices_" + index);
		}
	}

	private byte[] toByteArray(List<Vec3> vertices) {
		ByteBuffer bb = ByteBuffer.allocate(vertices.size() * 3 * 8);
		FloatBuffer fb = bb.asFloatBuffer();
		for (Vec3 v : vertices) {
			fb.put(v.x);
			fb.put(v.y);
			fb.put(v.z);
		}
		return bb.array();
	}

	private List<Vec3> fromByteArray(byte[] bytes) {
		List<Vec3> list = new ArrayList<>();
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		FloatBuffer dd = bb.asFloatBuffer();
		for (int i = 0; i < dd.capacity(); i += 3) {
			list.add(new Vec3(dd.get(), dd.get(), dd.get()));
		}
		return list;
	}
}
