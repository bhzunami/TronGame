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
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.ether.image.ImageProcessor;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.VideoFrame;
import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.ether.video.fx.IVideoCPUFX;
import ch.fhnw.ether.video.fx.IVideoGLFX;
import ch.fhnw.util.color.ColorUtilities;
import ch.fhnw.util.math.MathUtilities;

public class MotionBlur extends AbstractVideoFX implements IVideoCPUFX, IVideoGLFX {
	private static final Parameter DECAY = new Parameter("decay", "Decay", 0.01f, 1f, 1f);

	private static final VideoFrame DUMMY_FRAME = new VideoFrame(IHostImage.create(1, 1, ComponentType.BYTE, ComponentFormat.RGB));
	private static final String     PREVIOUS    = "previous";
	
	private float[][]    buffer     = new float[1][1];
	private VideoFrame[] vbuffer    = {DUMMY_FRAME, DUMMY_FRAME};
	private int          vbufferIdx = 0;

	public MotionBlur() {
		super(
				NO_UNIFORMS,
				NO_INOUT,
				uniforms(PREVIOUS, DUMMY_FRAME),
				DECAY);
	}

	@Override
	public void processFrame(double playOutTime, IVideoRenderTarget target) {
		vbuffer[vbufferIdx] = target.getFrame();
		vbufferIdx ^= 1;
		setUniform(PREVIOUS, vbuffer[vbufferIdx]);
	}

	@Override
	public String mainFrag() {
		return "result = mix(texture(previous,uv),result,decay);";
	}

	@Override
	public void processFrame(final double playOutTime, final IVideoRenderTarget target, final IHostImage image) {
		// XXX RGBA support?
		
		if(buffer[0].length != image.getWidth() * 3 || buffer.length != image.getHeight())
			buffer  = new float[image.getHeight()][image.getWidth() * 3];

		float decay = getVal(DECAY);

		ImageProcessor.processLines(image, (pixels, j) -> {
			int           idx     = 0;
			final float[] bufferJ = buffer[j];
			for(int i = image.getWidth(); --i >= 0;) {
				position(pixels, image, i, j);

				float r = ColorUtilities.toFloat(pixels.get());
				float g = ColorUtilities.toFloat(pixels.get());
				float b = ColorUtilities.toFloat(pixels.get());

				bufferJ[idx] = MathUtilities.lerp(bufferJ[idx], r, decay); idx++;
				bufferJ[idx] = MathUtilities.lerp(bufferJ[idx], g, decay); idx++;
				bufferJ[idx] = MathUtilities.lerp(bufferJ[idx], b, decay);

				idx -= 2;

				position(pixels, image, i, j);					
				pixels.put(ColorUtilities.toByte(bufferJ[idx++]));
				pixels.put(ColorUtilities.toByte(bufferJ[idx++]));
				pixels.put(ColorUtilities.toByte(bufferJ[idx++]));

			}
		});
	}
	
	private static void position(ByteBuffer pixels, IHostImage image, int x, int y) {
		pixels.position((y * image.getWidth() + x) * image.getNumBytesPerPixel());
	}
}
