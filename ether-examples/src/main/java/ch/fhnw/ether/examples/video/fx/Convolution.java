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

package ch.fhnw.ether.examples.video.fx;

import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.ImageProcessor;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.ether.video.fx.IVideoCPUFX;
import ch.fhnw.ether.video.fx.IVideoGLFX;
import ch.fhnw.util.color.ColorUtilities;
import ch.fhnw.util.math.Mat3;

public class Convolution extends AbstractVideoFX implements IVideoCPUFX, IVideoGLFX {
	private static final Parameter KERNEL = new Parameter("kernel_sel", "Effect", 0, 
			"Identity", 
			"Edge Detection1", 
			"Edge Detection2", 
			"Emboss",
			"Sharpen", 
			"Box Blur", 
			"Gaussian Blur");

	private static final Mat3[] KERNELS = {
			new Mat3( 0, 0, 0,   0, 1, 0,   0, 0, 0),
			new Mat3( 1, 0,-1,   0, 0, 0,  -1, 0, 1),
			new Mat3( 0, 1, 0,   1,-4, 1,   0, 1, 0),
			new Mat3( 4, 0, 0,   0, 0, 0,   0, 0,-4),
			new Mat3( 0,-1, 0,  -1, 5,-1,   0,-1, 0),
			normalize(new Mat3( 1, 1, 1,   1, 1, 1,   1, 1, 1)),
			normalize(new Mat3( 1, 2, 1,   2, 4, 2,   1, 2, 1)),
	};

	private static final boolean[] GREYSCALE = {
			false,
			true,
			true,
			false,
			false,
			false,
			false,
	};

	@Override
	public String mainFrag() {
		return lines(
				"result = ",
				"textureOffset(frame, uv, ivec2(-1,  1)) * kernel[0][0] +",
				"textureOffset(frame, uv, ivec2( 0,  1)) * kernel[0][1] +",
				"textureOffset(frame, uv, ivec2( 1,  1)) * kernel[0][2] +",

				"textureOffset(frame, uv, ivec2(-1,  0)) * kernel[1][0] +",
				"textureOffset(frame, uv, ivec2( 0,  0)) * kernel[1][1] +",
				"textureOffset(frame, uv, ivec2( 1,  0)) * kernel[1][2] +",

				"textureOffset(frame, uv, ivec2(-1, -1)) * kernel[2][0] +",
				"textureOffset(frame, uv, ivec2( 0, -1)) * kernel[2][1] +",
				"textureOffset(frame, uv, ivec2( 1 ,-1)) * kernel[2][2];",

				"result.a = 1.;",
				"if(greyscale) {",
				"	float val = result.r + result.b + result.g;",
				"	result.r = val;",
				"	result.g = val;",
				"	result.b = val;",
				"}"
				);
	}

	public Convolution() {
		super(
				NO_UNIFORMS, 
				NO_INOUT,
				uniforms("kernel", new Mat3(),"greyscale", Boolean.FALSE),
				KERNEL);
	}

	private static Mat3 normalize(Mat3 mat3) {
		float s = 0;
		s += Math.abs(mat3.m00);
		s += Math.abs(mat3.m10);
		s += Math.abs(mat3.m20);

		s += Math.abs(mat3.m01);
		s += Math.abs(mat3.m11);
		s += Math.abs(mat3.m22);

		s += Math.abs(mat3.m02);
		s += Math.abs(mat3.m12);
		s += Math.abs(mat3.m22);

		s = 1 / s;

		return new Mat3(
				s * mat3.m00,
				s * mat3.m10,
				s * mat3.m20,

				s * mat3.m01,
				s * mat3.m11,
				s * mat3.m21,

				s * mat3.m02,
				s * mat3.m12,
				s * mat3.m22
				);
	}

	@Override
	public void processFrame(double playOutTime, IVideoRenderTarget target) {
		setUniform("kernel",      KERNELS[(int) getVal(KERNEL)]);
		setUniform("greyscale",   Boolean.valueOf(GREYSCALE[(int) getVal(KERNEL)])); 
	}

	private float[][] outFrame = new float[1][1];

	@Override
	public void processFrame(final double playOutTime, final IVideoRenderTarget target, final IHostImage image) {
		ensureRGB8OrRGBA8(image);
		final int numComponents = image.getComponentFormat().getNumComponents();

		if(image.getHeight() != outFrame.length || image.getWidth() != outFrame[0].length * 3)
			outFrame = new float[image.getHeight()][image.getWidth() * 3];

		Mat3    kernel    = KERNELS[(int) getVal(KERNEL)];
		boolean greyscale = GREYSCALE[(int) getVal(KERNEL)]; 

		for(int j = image.getHeight() - 1; --j >= 1;) {
			int idx = 0;
			if(greyscale) {
				for(int i = 1; i< image.getWidth() - 1; i++) {
					float val = convolve(image, i, j, kernel, 0) + convolve(image, i, j, kernel, 1) + convolve(image, i, j, kernel, 2); 
					outFrame[j][idx++] = val; 
					outFrame[j][idx++] = val; 
					outFrame[j][idx++] = val; 
				}
			} else {
				for(int i = 1; i< image.getWidth() - 1; i++) {
					outFrame[j][idx++] = convolve(image, i, j, kernel, 0); 
					outFrame[j][idx++] = convolve(image, i, j, kernel, 1); 
					outFrame[j][idx++] = convolve(image, i, j, kernel, 2); 
				}
			}
		}

		if(numComponents == 4) {
			ImageProcessor.processLines(image, (pixels, j) -> {
				int idx = 0;
				for(int i = image.getWidth(); --i >= 0;) {
					pixels.put(ColorUtilities.toByte(outFrame[j][idx++]));
					pixels.put(ColorUtilities.toByte(outFrame[j][idx++]));
					pixels.put(ColorUtilities.toByte(outFrame[j][idx++]));
					pixels.put((byte)255);
				}
			});
		} else {
			ImageProcessor.processLines(image, (pixels, j) -> {
				int idx = 0;
				for(int i = image.getWidth(); --i >= 0;) {
					pixels.put(ColorUtilities.toByte(outFrame[j][idx++]));
					pixels.put(ColorUtilities.toByte(outFrame[j][idx++]));
					pixels.put(ColorUtilities.toByte(outFrame[j][idx++]));
				}
			});
		}
	}

	private float convolve(IHostImage image, int x, int y, Mat3 kernel, int c) {
		return
				image.getComponentFloat(x-1, y-1, c) * kernel.m00 +
				image.getComponentFloat(x-1, y,   c) * kernel.m10 +
				image.getComponentFloat(x-1, y+1, c) * kernel.m20 +

				image.getComponentFloat(x,   y-1, c) * kernel.m01 +
				image.getComponentFloat(x,   y,   c) * kernel.m11 +
				image.getComponentFloat(x,   y+1, c) * kernel.m21 +

				image.getComponentFloat(x+1, y-1, c) * kernel.m02 +
				image.getComponentFloat(x+1, y,   c) * kernel.m12 +
				image.getComponentFloat(x+1, y+1, c) * kernel.m22;
	}
}
