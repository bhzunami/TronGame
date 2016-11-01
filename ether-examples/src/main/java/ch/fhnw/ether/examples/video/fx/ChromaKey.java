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

import java.nio.ByteBuffer;

import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.ImageProcessor;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.ether.video.fx.IVideoCPUFX;
import ch.fhnw.ether.video.fx.IVideoGLFX;
import ch.fhnw.util.color.ColorUtilities;
import ch.fhnw.util.math.MathUtilities;


public class ChromaKey extends AbstractVideoFX implements IVideoCPUFX, IVideoGLFX {
	private static final Parameter HUE    = new Parameter("hue",   "Hue",                0, 1,    0.5f);
	private static final Parameter RANGE  = new Parameter("range", "Color Range",        0, 0.5f, 0.1f);
	private static final Parameter S_MIN  = new Parameter("sMin",  "Saturation Minimum", 0, 1,    0.1f);
	private static final Parameter B_MIN  = new Parameter("bMin",  "Brightness Minimum", 0, 1,    0.1f);

	private final IHostImage mask;

	public ChromaKey(IHostImage mask) {
		super(
				NO_UNIFORMS,
				NO_INOUT,
				uniforms("mask", mask),			
				HUE, RANGE, S_MIN, B_MIN);
		ensureRGB8OrRGBA8(mask);
		this.mask = mask;
	}

	@Override
	public String mainFrag() {
		return lines(
				"vec4 maskc = texture(mask,uv);",
				"vec4 hsba  = rgb2hsb(maskc.r, maskc.g, maskc.b, maskc.a);",
				"float hh   = wrap(hue + range);",
				"float hl   = wrap(hue - range);",
				"if(!(hsba.y > sMin && hsba.z > bMin && hsba.x > hl && hsba.x < hh))",
				"  result = maskc;"
				);
	}

	@Override
	public String[] functionsFrag() {
		return new String[] {
				ColorUtilities.glsl_hsb2rgb(),
				ColorUtilities.glsl_rgb2hsb(),
				lines(
						"float wrap(float v) {",
						"  int result = int(v * 1000.) % 1000;",
						"  return (result < 0 ? result + 1000 : result) / 1000.;",
						"}"
						)
		};
	}

	// XXX RGBA support?
	@Override
	public void processFrame(final double playOutTime, final IVideoRenderTarget target, final IHostImage image) {
		ensureRGB8OrRGBA8(image);

		final float hue   = getVal(HUE);
		final float range = getVal(RANGE);
		final float sMin  = getVal(S_MIN);
		final float bMin  = getVal(B_MIN);
		final float hh    = MathUtilities.wrap(hue + range);
		final float hl    = MathUtilities.wrap(hue - range);

		ImageProcessor.processLines(image, (pixels, j)->{
			final float[] hsb = new float[image.getWidth() * 3];
			final int     pos = pixels.position();
			ByteBuffer    mask = this.mask.getPixels().asReadOnlyBuffer();
			mask.position(pos);
			ColorUtilities.getHSBfromRGB(mask, hsb, this.mask.getComponentFormat().getNumComponents());
			pixels.position(pos);
			for(int i = 0; i < image.getWidth(); i++) {
				int idx = i * 3;
				if(hsb[idx+1] > sMin && hsb[idx+2] > bMin && hsb[idx+0] > hl && hsb[idx+0] < hh) {
					pixels.get();
					pixels.get();
					pixels.get();
				} else {
					pixels.put(ColorUtilities.toByte(this.mask.getComponentFloat(i, j, 0)));
					pixels.put(ColorUtilities.toByte(this.mask.getComponentFloat(i, j, 1)));
					pixels.put(ColorUtilities.toByte(this.mask.getComponentFloat(i, j, 2)));
				}
			}
		});
	}
}
