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

package ch.fhnw.ether.controller.tool;

import java.util.Map;
import java.util.TreeMap;

import ch.fhnw.ether.scene.I3DObject;
import ch.fhnw.ether.scene.camera.IViewCameraState;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.view.IView;
import ch.fhnw.ether.view.ProjectionUtilities;
import ch.fhnw.util.math.geometry.BoundingBox;
import ch.fhnw.util.math.geometry.GeometryUtilities;

/**
 * Utilities for 3D object picking
 */
public final class PickUtilities {
	public enum PickMode {
		POINT,
		// TODO: INSIDE & INTERSECT
	}

	private static final float PICK_DISTANCE = 10;

	public static Map<Float, I3DObject> pickFromScene(PickMode mode, float x, float y, float w, float h, IView view) {
		IViewCameraState vcs = view.getController().getRenderManager().getViewCameraState(view);
		final Map<Float, I3DObject> pickables = new TreeMap<>();
		for (I3DObject object : view.getController().getScene().get3DObjects()) {
			float d = pickObject(mode, x, y, w, h, vcs, object);
			if (d < Float.POSITIVE_INFINITY)
				pickables.put(d, object);
		}
		return pickables;
	}
	
	public static float pickObject(PickMode mode, float x, float y, float w, float h, IViewCameraState vcs, I3DObject object) {
		BoundingBox b = object.getBounds();
		
		if (b == null || !(b.isValid()))
			return Float.POSITIVE_INFINITY;
		
		float d = pickBoundingBox(mode, x, y, w, h, vcs, b);
		if (d == Float.POSITIVE_INFINITY)
			return Float.POSITIVE_INFINITY;

		if (!(object instanceof IMesh))
			return d;
		
		IMesh mesh = (IMesh)object;
		float[] data = mesh.getTransformedPositionData();
		
		switch (mesh.getType()) {
		case POINTS:
			return pickPoints(mode, x, y, w, h, vcs, data);
		case LINES:
			return pickLines(mode, x, y, w, h, vcs, data);
		case LINE_STRIP:
			return pickLineStripOrLoop(mode, x, y, w, h, vcs, data, false);
		case LINE_LOOP:
			return pickLineStripOrLoop(mode, x, y, w, h, vcs, data, true);
		case TRIANGLES:
			return pickTriangles(mode, x, y, w, h, vcs, data);
		case TRIANGLE_STRIP:
		case TRIANGLE_FAN:
			throw new UnsupportedOperationException("picking of triangle strips and fans currently unsupported");
		}
		return Float.POSITIVE_INFINITY;
	}

	public static float pickBoundingBox(PickMode mode, float x, float y, float w, float h, IViewCameraState vcs, BoundingBox bounds) {
		BoundingBox b = new BoundingBox();
		float xmin = bounds.getMinX();
		float xmax = bounds.getMaxX();
		float ymin = bounds.getMinY();
		float ymax = bounds.getMaxY();
		float zmin = bounds.getMinZ();
		float zmax = bounds.getMaxZ();

		float[] v = new float[] {
			xmin, ymin, zmin, xmin, ymin, zmax, 
			xmin, ymax, zmin, xmin, ymax, zmax, 
			xmax, ymin, zmin, xmax, ymin, zmax, 
			xmax, ymax, zmin, xmax, ymax, zmax, 
		};
		b.add(ProjectionUtilities.projectToScreen(vcs.getViewProjMatrix(), vcs.getViewport(), v));
		b.grow(PICK_DISTANCE, PICK_DISTANCE, 0);

		if (b.isValid() && b.getMaxZ() > 0 && x > b.getMinX() && x < b.getMaxX() && y > b.getMinY() && y < b.getMaxY())
			return Math.max(0, b.getMinZ());

		return Float.POSITIVE_INFINITY;
	}

	public static float pickPoints(PickMode mode, float x, float y, float w, float h, IViewCameraState vcs, float[] v) {
		v = ProjectionUtilities.projectToScreen(vcs, v);

		float zMin = Float.POSITIVE_INFINITY;
		for (int i = 0; i < v.length; i += 3) {
			float d = (float) Math.sqrt((v[i] - x) * (v[i] - x) + (v[i + 1] - y) * (v[i + 1] - y));
			if (d < PICK_DISTANCE)
				zMin = Math.min(zMin, v[i + 2]);
		}
		return zMin;
	}

	public static float pickLines(PickMode mode, float x, float y, float w, float h, IViewCameraState vcs, float[] v) {
		v = ProjectionUtilities.projectToScreen(vcs, v);

		float zMin = Float.POSITIVE_INFINITY;
		for (int i = 0; i < v.length; i += 6) {
			float t = hitLine2D(x, y, v[i], v[i + 1], v[i + 3], v[i + 4]);
			if (t == -1)
				continue;
			float z = v[i + 2] + t * (v[i + 5] - v[i + 2]);
			zMin = Math.min(zMin, z);
		}
		return zMin;
	}

	public static float pickLineStripOrLoop(PickMode mode, float x, float y, float w, float h, IViewCameraState vcs, float[] v, boolean loop) {
		v = ProjectionUtilities.projectToScreen(vcs, v);

		float zMin = Float.POSITIVE_INFINITY;
		for (int i = 0; i < v.length; i += 3) {
			float t = hitLine2D(x, y, v[i], v[i + 1], v[(i + 3) % v.length], v[(i + 4) % v.length]);
			if (t == -1)
				continue;
			float z = v[i + 2] + t * (v[(i + 5) % v.length] - v[i + 2]);
			zMin = Math.min(zMin, z);
			if (!loop && i == v.length - 6)
				break;
		}
		return zMin;
	}
	
	public static float pickTriangles(PickMode mode, float x, float y, float w, float h, IViewCameraState vcs, float[] v) {
		v = ProjectionUtilities.projectToScreen(vcs, v);

		float zMin = Float.POSITIVE_INFINITY;
		for (int i = 0; i < v.length; i += 9) {
			float z = GeometryUtilities.intersectScreenRayWithTriangle(x, y, v, i);
			if (z > 0)
				zMin = Math.min(zMin, z);
		}
		return zMin;
	}

	
	private static float hitLine2D(float x, float y, float x0, float y0, float x1, float y1) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float dl2 = dx * dx + dy * dy;
		if (dl2 == 0)
			return -1;

		float mx = x - x0;
		float my = y - y0;

		float t = (mx * dx + my * dy) / dl2;
		if (t < 0 || t > 1)
			return -1;

		float px = t * dx;
		float py = t * dy;

		float d2 = (mx - px) * (mx - px) + (my - py) * (my - py);
		if (d2 > PICK_DISTANCE * PICK_DISTANCE)
			return -1;
		return t;
	}
}
