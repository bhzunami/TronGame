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

package ch.fhnw.ether.view;

import ch.fhnw.ether.scene.camera.IViewCameraState;
import ch.fhnw.util.Viewport;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.Vec4;
import ch.fhnw.util.math.geometry.Line;

public final class ProjectionUtilities {
	public static Vec3 projectToDevice(IViewCameraState vcs, Vec3 v) {
		return projectToDevice(vcs, new Vec4(v));
	}

	public static Vec3 projectToDevice(Mat4 viewProjMatrix, Vec3 v) {
		return projectToDevice(viewProjMatrix, new Vec4(v));
	}

	public static Vec3 projectToDevice(IViewCameraState vcs, Vec4 v) {
		return projectToDevice(vcs.getViewProjMatrix(), v);
	}

	public static Vec3 projectToDevice(Mat4 viewProjMatrix, Vec4 v) {
		Vec4 proj = viewProjMatrix.transform(v);

		if (proj.w == 0)
			return null;

		// map x, y and z to range [-1, 1]
		float x = proj.x / proj.w;
		float y = proj.y / proj.w;
		float z = proj.z / proj.w;

		return new Vec3(x, y, z);
	}

	public static float[] projectToDevice(IViewCameraState vcs, float[] v) {
		return projectToDevice(vcs.getViewProjMatrix(), v);
	}

	public static float[] projectToDevice(Mat4 viewProjMatrix, float[] v) {
		float[] proj = viewProjMatrix.transform(v);
		for (int i = 0; i < v.length; i += 3) {
			proj[i] = proj[i];
			proj[i + 1] = proj[i + 1];
			proj[i + 2] = proj[i + 2];
		}
		return proj;
	}

	public static Vec3 projectToScreen(IViewCameraState vcs, Vec3 v) {
		return projectToScreen(vcs, new Vec4(v));
	}

	public static Vec3 projectToScreen(Mat4 viewProjMatrix, Viewport viewport, Vec3 v) {
		return projectToScreen(viewProjMatrix, viewport, new Vec4(v));
	}

	public static Vec3 projectToScreen(IViewCameraState vcs, Vec4 v) {
		return projectToScreen(vcs.getViewProjMatrix(), vcs.getViewport(), v);
	}

	public static Vec3 projectToScreen(Mat4 viewProjMatrix, Viewport viewport, Vec4 v) {
		Vec4 proj = viewProjMatrix.transform(v);

		if (proj.w == 0)
			return null;

		// map x, y and z to range [0, 1]
		float x = 0.5f * (proj.x / proj.w + 1);
		float y = 0.5f * (proj.y / proj.w + 1);
		float z = 0.5f * (proj.z / proj.w + 1);

		// map x and y to viewport
		x = x * viewport.w + viewport.x;
		y = y * viewport.h + viewport.y;
		return new Vec3(x, y, z);
	}

	public static float[] projectToScreen(IViewCameraState vcs, float[] v) {
		return projectToScreen(vcs.getViewProjMatrix(), vcs.getViewport(), v);
	}

	public static float[] projectToScreen(Mat4 viewProjMatrix, Viewport viewport, float[] v) {
		float[] proj = viewProjMatrix.transform(v);
		for (int i = 0; i < v.length; i += 3) {
			proj[i] = 0.5f * (proj[i] + 1) * viewport.w + viewport.x;
			proj[i + 1] = 0.5f * (proj[i + 1] + 1) * viewport.h + viewport.y;
			proj[i + 2] = 0.5f * (proj[i + 2] + 1);
		}
		return proj;
	}

	public static Vec3 unprojectFromScreen(IViewCameraState vcs, Vec3 v) {
		return unprojectFromScreen(vcs.getViewProjInvMatrix(), vcs.getViewport(), v);
	}

	public static Vec3 unprojectFromScreen(Mat4 viewProjInvMatrix, Viewport viewport, Vec3 v) {
		// map x and y from window coordinates
		float x = (v.x - viewport.x) / viewport.w;
		float y = (v.y - viewport.y) / viewport.h;
		float z = v.z;

		// map to range -1 to 1
		x = x * 2 - 1;
		y = y * 2 - 1;
		z = z * 2 - 1;

		Vec4 result = viewProjInvMatrix.transform(new Vec4(x, y, z, 1));

		if (result.w == 0) {
			return null;
		}

		return new Vec3(result.x / result.w, result.y / result.w, result.z / result.w);
	}

	public static float deviceToScreenX(IView view, float x) {
		return (1.0f + x) / 2.0f * view.getViewport().w;
	}

	public static float deviceToScreenY(IView view, float y) {
		return (1.0f + y) / 2.0f * view.getViewport().h;
	}

	public static float screenToDeviceX(IView view, float x) {
		return 2f * x / view.getViewport().w - 1f;
	}

	public static float screenToDeviceY(IView view, float y) {
		return 2f * y / view.getViewport().h - 1f;
	}

	public static Line getRay(IViewCameraState vcs, float x, float y) {
		Vec3 p0 = unprojectFromScreen(vcs, new Vec3(x, y, 0.1f));
		Vec3 p1 = unprojectFromScreen(vcs, new Vec3(x, y, 0.9f));
		return new Line(p0, p1);
	}
}
