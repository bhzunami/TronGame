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

import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.RenderCommandException;

public class SilenceAudioSource extends AbstractFrameSource implements IAudioSource {
	private final float sampleRate;
	private final int   nChannels;
	private final int   frameSize;

	long samples;


	public SilenceAudioSource(int nChannels, float sampleRate, int frameSize) {
		this.nChannels  = nChannels;
		this.sampleRate = sampleRate;
		this.frameSize  = Math.max(1, (frameSize / nChannels) * nChannels);
	}

	@Override
	protected void run(IRenderTarget<?> target) throws RenderCommandException {
		((IAudioRenderTarget)target).setFrame(this, createAudioFrame(samples, frameSize));
		samples += frameSize;
	}	

	@Override
	public long getLengthInFrames() {
		return FRAMECOUNT_UNKNOWN;
	}

	@Override
	public double getLengthInSeconds() {
		return LENGTH_INFINITE;
	}
	
	@Override
	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public int getNumChannels() {
		return nChannels;
	}
	
	@Override
	public float getFrameRate() {
		double result = (frameSize / getNumChannels()) * sampleRate;
		return (float)result;
	}
}
