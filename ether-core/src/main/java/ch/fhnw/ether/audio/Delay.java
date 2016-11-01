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

import ch.fhnw.util.IModifierFloat;

public class Delay {
	private float[]     buffer;
	private int         ptr;
	private int         len;
	private final int   nChannels;
	private final float sRate;

	public Delay(IAudioRenderTarget target, double lengthInSec) {
		nChannels = target.getNumChannels();
		sRate     = target.getSampleRate();
		len       = sec2samples(lengthInSec);
		buffer    = new float[len];
	}

	private int sec2samples(final double lengthInSec) {
		return (int)(nChannels * lengthInSec * sRate) + 1;
	}

	public void setLength(double lengthInSec) {
		int newLen = sec2samples(lengthInSec);
		if(newLen > len) {
			buffer = Arrays.copyOf(buffer, newLen);
			int length = (len - ptr) - 1;
			System.arraycopy(buffer, ptr + 1, buffer, buffer.length - length, length);
		}
		len = newLen;
	}

	public void modifySamples(AudioFrame frame, IModifierFloat modifier) {
		final float[] samples = frame.samples;
		for(int i = 0; i < samples.length; i++) {
			final float sample = samples[i];
			buffer[ptr++] = sample;
			if(ptr >= len) ptr = 0;
			samples[i] = modifier.modify(sample, i);
		}
		frame.modified();
	}

	public float get(final double delay) {
		final int c   = ptr % nChannels;
		final int off = (int)(delay * sRate);
		return buffer[(ptr + len - (off * nChannels + c)) % len];
	}
}
