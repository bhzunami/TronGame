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
import ch.fhnw.util.color.ColorUtilities;

public class AnalogTVFX extends AbstractVideoFX implements IVideoCPUFX {
	private static final int VBLANK = 32;

	private static final Parameter Y  = new Parameter("y",  "Y Gain",        0, 4, 1);
	private static final Parameter A  = new Parameter("a",  "Chroma Gain",   0, 4, 1);
	private static final Parameter P  = new Parameter("p",  "Chroma Phase",  0, (float)(2 * Math.PI), 0);
	private static final Parameter C  = new Parameter("c",  "Chroma Shift",  0, 32, 0);
	private static final Parameter HA = new Parameter("h",  "H-Amplitude",   0, 32, 0);
	private static final Parameter HF = new Parameter("hf", "H-Frequency",   1, 100, 1);
	private static final Parameter HP = new Parameter("hf", "H-Phase",       0, 2,   0);
	private static final Parameter HD = new Parameter("hf", "H-Decay",       0, 1,   0);
	private static final Parameter V  = new Parameter("v",  "V-Roll",        0, 64,  0);

	long      lineCount;
	float[][] yuvFrame = new float[1][1];
	int       vOff;

	public AnalogTVFX() {
		super(Y, A, P, C, HA, HF, HP, HD, V);
	}

	@Override
	public void processFrame(final double playOutTime, final IVideoRenderTarget target, final IHostImage image) {
		ensureRGB8OrRGBA8(image);
		final int numComponents = image.getComponentFormat().getNumComponents();

		final float  y  = getVal(Y);
		final float  a  = getVal(A);
		final float  p  = getVal(P);
		final int    c  = ((int)getVal(C)) * 3;
		final double ha = getVal(HA);
		final double hf = getVal(HF);
		final float  hd = getVal(HD);
		final double hp = getVal(HP);
		
		
		if(vOff < 0) vOff = 0;
		vOff += (int)getVal(V);

		if(yuvFrame.length != image.getHeight() + VBLANK || yuvFrame[0].length != image.getWidth() * 3)
			yuvFrame = new float[image.getHeight() + VBLANK][image.getWidth() * 3];

		ImageProcessor.processLines(image, (pixels, j)->{
			final float[] yuv  = yuvFrame[j];
			final int     hoff = 3 * (int)((Math.sin(lineCount++ / hf) + 1.0) * ha + hp * j);      
			ColorUtilities.getYUVfromRGB(pixels, yuv, numComponents);
			for(int i = 3; i < yuv.length; i += 3) {
				final int    idxY   = (i + hoff) % yuv.length;
				final int    idxC   = (idxY + c) % yuv.length;
				final double amplC  = Math.hypot(yuv[idxC+1], yuv[idxC+2]) * a;
				final double phaseC = Math.atan2(yuv[idxC+1], yuv[idxC+2]) + p;
				yuv[i+0] = yuv[idxY] * y + hd * yuv[i - 3];
				yuv[i+1] = (float) (Math.sin(phaseC) * amplC);
				yuv[i+2] = (float) (Math.cos(phaseC) * amplC);
			}
		});

		ImageProcessor.processLines(image, (pixels, j)->{
			final float[] yuv  = yuvFrame[(j+vOff) % yuvFrame.length];
			ColorUtilities.putRGBfromYUV(pixels, yuv, numComponents);
		});
	}
}
