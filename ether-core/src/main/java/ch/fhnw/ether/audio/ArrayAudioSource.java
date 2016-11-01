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

package ch.fhnw.ether.audio;

import java.util.Arrays;

import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.util.FloatList;


public class ArrayAudioSource extends AbstractFrameSource implements IAudioSource {
	private       int     numPlays;
	private final long    frameCount;
	private final int     nChannels;
	private final float[] data;
	private final float   sRate;
	private final int     frameSz;
	private       long    samples;
	private       int     rdPtr;

	public ArrayAudioSource(FloatList samples, int numChannels, float samplingRate, final int numPlays) {
		this(samples.toArray(), numChannels, samplingRate, numPlays);
	}

	public ArrayAudioSource(final float[] samples, int numChannels, float samplingRate, final int numPlays) {
		this.frameSz    = 128 * numChannels;
		this.numPlays   = numPlays;
		this.data       = samples;
		this.nChannels  = numChannels;
		this.sRate      = samplingRate;
		this.frameCount = (samples.length + (this.frameSz - 1)) / this.frameSz;
	}

	@Override
	protected void run(IRenderTarget<?> target) throws RenderCommandException {
		try {
			int to = rdPtr + frameSz;
			if(to >= data.length) {
				to = data.length;
				numPlays--;
			}
			final float[] outData = Arrays.copyOfRange(data, rdPtr, to);
			AudioFrame frame = createAudioFrame(samples, outData);
			frame.setLast(numPlays <= 0);
			((IAudioRenderTarget)target).setFrame(this, frame);
			samples += outData.length;
			rdPtr = to % data.length;
		} catch(Throwable t) {
			throw new RenderCommandException(t);
		}
	}

	@Override
	public float getSampleRate() {
		return sRate;
	}

	@Override
	public long getLengthInFrames() {
		return frameCount;
	}

	@Override
	public int getNumChannels() {
		return nChannels;
	}

	@Override
	public float getFrameRate() {
		double result = getLengthInFrames() / getLengthInSeconds();
		return (float)result;
	}
	
	@Override
	public double getLengthInSeconds() {
		return data.length / nChannels / getSampleRate();
	}
}
