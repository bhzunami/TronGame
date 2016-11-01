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

package ch.fhnw.ether.mapping;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.*;

import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BimberRaskarCalibrator implements ICalibrator {

	private RealMatrix projMatrix;
	private RealMatrix viewMatrix;

	public BimberRaskarCalibrator() {
	}

	@Override
	public double calibrate(List<Vec3> modelVertices, List<Vec3> projectedVertices, float near, float far) {
		if (modelVertices.size() < 6) {
			return Double.POSITIVE_INFINITY;
		}

		// Bimber Raskar Appendix A.1

		// step 1: fill matrix that covers the constraining equations

		RealMatrix lhs = MatrixUtils.createRealMatrix(2 * modelVertices.size(), 12);
		for (int i = 0; i < modelVertices.size(); ++i) {
			Vec3 mp = modelVertices.get(i);
			Vec3 dp = projectedVertices.get(i);
			double x = mp.x;
			double y = mp.y;
			double z = mp.z;
			double u = dp.x;
			double v = dp.y;

			lhs.setEntry(2 * i, 0, x);
			lhs.setEntry(2 * i, 1, y);
			lhs.setEntry(2 * i, 2, z);
			lhs.setEntry(2 * i, 3, 1);
			lhs.setEntry(2 * i, 4, 0);
			lhs.setEntry(2 * i, 5, 0);
			lhs.setEntry(2 * i, 6, 0);
			lhs.setEntry(2 * i, 7, 0);
			lhs.setEntry(2 * i, 8, -u * x);
			lhs.setEntry(2 * i, 9, -u * y);
			lhs.setEntry(2 * i, 10, -u * z);
			lhs.setEntry(2 * i, 11, -u);

			lhs.setEntry(2 * i + 1, 0, 0);
			lhs.setEntry(2 * i + 1, 1, 0);
			lhs.setEntry(2 * i + 1, 2, 0);
			lhs.setEntry(2 * i + 1, 3, 0);
			lhs.setEntry(2 * i + 1, 4, x);
			lhs.setEntry(2 * i + 1, 5, y);
			lhs.setEntry(2 * i + 1, 6, z);
			lhs.setEntry(2 * i + 1, 7, 1);
			lhs.setEntry(2 * i + 1, 8, -v * x);
			lhs.setEntry(2 * i + 1, 9, -v * y);
			lhs.setEntry(2 * i + 1, 10, -v * z);
			lhs.setEntry(2 * i + 1, 11, -v);
		}

		// step 2: find u-vector corresponding to smallest singular value (S)
		// (=solution)

		// note that the Apache SVD implementation returns values in descending
		// order, so smallest column will always be last column and we can skip
		// search as done in the original Bimber-Raskar code.
		/*
		 * RealMatrix d = svd.getS(); RealMatrix u = svd.getU(); int smallestCol = 0; for (int j = 0; j < 12; ++j) {
		 * double a = d.getEntry(smallestCol, smallestCol); double b = d.getEntry(j, j); if (a * a > b * b) smallestCol
		 * = j; } RealVector s = u.getColumnVector(smallestCol);
		 */
		RealMatrix l = lhs.transpose().multiply(lhs);
		RealVector s = new SingularValueDecomposition(l).getU().getColumnVector(11);

		// step 3: write 12x1 vector as 3x4 matrix (row-wise)
		RealMatrix pmv = MatrixUtils.createRealMatrix(3, 4);
		pmv.setRowVector(0, s.getSubVector(0, 4));
		pmv.setRowVector(1, s.getSubVector(4, 4));
		pmv.setRowVector(2, s.getSubVector(8, 4));

		// step 4: decompose pmv into 4x4 projection and view matrices
		double scale = pmv.getSubMatrix(2, 2, 0, 2).getRowVector(0).getNorm();
		pmv = pmv.scalarMultiply(1.0 / scale);

		if (pmv.getEntry(2, 3) > 0)
			pmv = pmv.scalarMultiply(-1.0);

		Vector3D q0 = toVector3D(pmv.getSubMatrix(0, 0, 0, 2).getRowVector(0));
		Vector3D q1 = toVector3D(pmv.getSubMatrix(1, 1, 0, 2).getRowVector(0));
		Vector3D q2 = toVector3D(pmv.getSubMatrix(2, 2, 0, 2).getRowVector(0));
		double q03 = pmv.getEntry(0, 3);
		double q13 = pmv.getEntry(1, 3);
		double q23 = pmv.getEntry(2, 3);

		double tz = q23;
		double tzeps = 1.0;
		if (tz > 0.0)
			tzeps = -1.0;

		tz = tzeps * q23;

		Vector3D r2 = q2.scalarMultiply(tzeps);

		double u0 = q0.dotProduct(q2);
		double v0 = q1.dotProduct(q2);

		double a = q0.crossProduct(q2).getNorm();
		double b = q1.crossProduct(q2).getNorm();

		Vector3D r0 = q0.subtract(q2.scalarMultiply(u0)).scalarMultiply(tzeps / a);
		Vector3D r1 = q1.subtract(q2.scalarMultiply(v0)).scalarMultiply(tzeps / b);

		double tx = tzeps * (q03 - u0 * tz) / a;
		double ty = tzeps * (q13 - v0 * tz) / b;

		// create rotation matrix and translation vector
		// (skipped since not needed for our purpose here)

		// create 4x4 projection and view matrices
		projMatrix = MatrixUtils.createRealMatrix(4, 4);
		projMatrix.setEntry(0, 0, -a);
		projMatrix.setEntry(0, 1, 0.0);
		projMatrix.setEntry(0, 2, -u0);
		projMatrix.setEntry(0, 3, 0);
		projMatrix.setEntry(1, 0, 0);
		projMatrix.setEntry(1, 1, -b);
		projMatrix.setEntry(1, 2, -v0);
		projMatrix.setEntry(1, 3, 0);
		projMatrix.setEntry(2, 0, 0);
		projMatrix.setEntry(2, 1, 0);
		if (far >= Double.POSITIVE_INFINITY) {
			projMatrix.setEntry(2, 2, -1.0);
			projMatrix.setEntry(2, 3, -2.0 * near);
		} else {
			projMatrix.setEntry(2, 2, -(far + near) / (far - near));
			projMatrix.setEntry(2, 3, -2.0 * far * near / (far - near));
		}
		projMatrix.setEntry(3, 0, 0);
		projMatrix.setEntry(3, 1, 0);
		projMatrix.setEntry(3, 2, -1);
		projMatrix.setEntry(3, 3, 0);

		viewMatrix = MatrixUtils.createRealMatrix(4, 4);
		viewMatrix.setEntry(0, 0, r0.getX());
		viewMatrix.setEntry(0, 1, r0.getY());
		viewMatrix.setEntry(0, 2, r0.getZ());
		viewMatrix.setEntry(1, 0, r1.getX());
		viewMatrix.setEntry(1, 1, r1.getY());
		viewMatrix.setEntry(1, 2, r1.getZ());
		viewMatrix.setEntry(2, 0, r2.getX());
		viewMatrix.setEntry(2, 1, r2.getY());
		viewMatrix.setEntry(2, 2, r2.getZ());
		viewMatrix.setEntry(0, 3, tx);
		viewMatrix.setEntry(1, 3, ty);
		viewMatrix.setEntry(2, 3, tz);
		viewMatrix.setEntry(3, 3, 1.0);

		return getError(projMatrix, viewMatrix, modelVertices, projectedVertices);
	}

	@Override
	public Mat4 getProjMatrix() {
		return toMat4(projMatrix);
	}

	@Override
	public Mat4 getViewMatrix() {
		return toMat4(viewMatrix);
	}

	private double getError(RealMatrix projMatrix, RealMatrix viewMatrix, List<Vec3> modelVertices, List<Vec3> projectedVertices) {
		if (modelVertices.size() != projectedVertices.size())
			throw new IllegalArgumentException("lists of vectors do not have same size");

		RealMatrix pm = projMatrix.multiply(viewMatrix);
		ArrayList<Vec3> projectedPoints = new ArrayList<>(modelVertices.size());
		RealVector rp = new ArrayRealVector(4);
		for (Vec3 p : modelVertices) {
			rp.setEntry(0, p.x);
			rp.setEntry(1, p.y);
			rp.setEntry(2, p.z);
			rp.setEntry(3, 1.0);
			RealVector pp = pm.operate(rp);
			pp = pp.mapDivide(pp.getEntry(3));
			projectedPoints.add(new Vec3(pp.getEntry(0), pp.getEntry(1), 0.0));
		}

		double error = 0.0;
		for (int i = 0; i < projectedPoints.size(); ++i) {
			error += projectedPoints.get(i).distance(projectedVertices.get(i));
		}
		return error;
	}

	private Mat4 toMat4(RealMatrix m) {
		float[] v = new float[16];
		for (int i = 0; i < 16; ++i) {
			v[i] = (float) m.getEntry(i % 4, i / 4);
		}
		return new Mat4(v);
	}

	private static Vector3D toVector3D(RealVector v) {
		return new Vector3D(v.getEntry(0), v.getEntry(1), v.getEntry(2));
	}
}
