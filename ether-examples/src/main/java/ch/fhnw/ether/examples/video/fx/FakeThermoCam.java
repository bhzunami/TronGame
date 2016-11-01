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
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.ether.video.fx.IVideoCPUFX;
import ch.fhnw.ether.video.fx.IVideoGLFX;
import ch.fhnw.util.color.ColorUtilities;

public class FakeThermoCam extends AbstractVideoFX implements IVideoCPUFX, IVideoGLFX {
	@Override
	public String mainFrag() {
		return "result = hsb2rgb((result.r + result.g + result.b) / 3., 1., 1., 1.)";
	}

	@Override
	public String[] functionsFrag() {
		return new String[] {
				ColorUtilities.glsl_hsb2rgb(),
		};
	}

	@Override
	public void processFrame(final double playOutTime, final IVideoRenderTarget target, final IHostImage image) {
		ensureRGB8OrRGBA8(image);
		final int numComponents = image.getComponentFormat().getNumComponents();

		ImageProcessor.processLines(image, (pixels, j)->{
			float[] hsb = new float[image.getWidth() * 3];
			int pos = pixels.position();
			for(int i = 0; i < image.getHeight(); i++) {
				float v = ColorUtilities.toFloat(pixels.get()) + ColorUtilities.toFloat(pixels.get()) + ColorUtilities.toFloat(pixels.get());
				hsb[i*3+0] = v / 3f;
				hsb[i*3+1] = 1f;
				hsb[i*3+2] = 1f;
				if(numComponents == 4) 
					pixels.get();
			}
			pixels.position(pos);
			ColorUtilities.putRGBfromHSB(pixels, hsb, numComponents);
		});
	}
}
