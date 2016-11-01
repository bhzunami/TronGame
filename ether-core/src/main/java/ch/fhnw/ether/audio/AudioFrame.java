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

import ch.fhnw.ether.media.AbstractFrame;

public class AudioFrame extends AbstractFrame {
	public final int     nChannels;
	public final float[] samples;
	private      float[] monoSamples;
	public final float   sRate;
	public final long    sTime;

	public AudioFrame(long sTime, int nChannels, float sRate, float[] samples) {
		super((sTime / nChannels) / (double)sRate);
		this.nChannels = nChannels;
		this.sRate     = sRate;
		this.sTime     = sTime;
		this.samples   = samples;
	}

	public float[] getMonoSamples() {
		if(nChannels == 1)
			return samples;
		
		if(monoSamples == null) {
			monoSamples = new float[samples.length / nChannels];
			for(int i = 0; i < samples.length; i++)
				monoSamples[i / nChannels] += samples[i];
			final float cs = nChannels;
			for(int i = 0; i< monoSamples.length; i++)
				monoSamples[i] /= cs;
		}
		return monoSamples;
	}

	public void modified() {
		monoSamples = null;
	}

	public double lengthInSecs() {
		return samples.length / nChannels / sRate;
	}

	public boolean isModified() {
		return monoSamples == null;
	}
}
