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

public class RGBGain extends AbstractVideoFX implements IVideoCPUFX, IVideoGLFX {
	private static final Parameter RED   = new Parameter("red",   "Red Gain",   0, 2, 1);
	private static final Parameter GREEN = new Parameter("green", "Green Gain", 0, 2, 1);
	private static final Parameter BLUE  = new Parameter("blue",  "Blue Gain",  0, 2, 1);

	public RGBGain() {
		super(RED, GREEN, BLUE);
	}

	@Override
	public String mainFrag() {
		return "result = vec4(result.r * red, result.g * green, result.b * blue, 1)";
	}

	@Override
	public void processFrame(final double playOutTime, final IVideoRenderTarget target, final IHostImage image) {
		ensureRGB8OrRGBA8(image);
		final int numComponents = image.getComponentFormat().getNumComponents();

		final float rs = getVal(RED);
		final float gs = getVal(GREEN);
		final float bs = getVal(BLUE);

		ImageProcessor.processLines(image, (pixels, j)->{
			int idx = pixels.position();
			for(int i = 0; i < image.getWidth(); i++) {
				pixels.put(ColorUtilities.toByte(ColorUtilities.toFloat(pixels.get(idx++)) * rs));
				pixels.put(ColorUtilities.toByte(ColorUtilities.toFloat(pixels.get(idx++)) * gs));
				pixels.put(ColorUtilities.toByte(ColorUtilities.toFloat(pixels.get(idx++)) * bs));
				if(numComponents == 4) {					
					pixels.get();
					idx++;
				}
			}
		});
	}
}
