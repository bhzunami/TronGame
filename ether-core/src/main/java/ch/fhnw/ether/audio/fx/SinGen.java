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

package ch.fhnw.ether.audio.fx;

import ch.fhnw.ether.audio.AudioFrame;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.RenderCommandException;

public class SinGen extends AbstractRenderCommand<IAudioRenderTarget> {
	private static final double PI2 = Math.PI * 2;

	private static final Parameter GAIN  = new Parameter("gain",  "Gain",        0, 1,          0.5f);
	private static final Parameter F     = new Parameter("f",     "Frequency",   0, 20000,      1000);
	private static final Parameter PHI   = new Parameter("phi",   "Phase",       0, (float)PI2, 0);

	private final int channel;
	
	public SinGen(int channel) {
		super(GAIN, F, PHI);
		this.channel = channel;
	}

	@Override
	protected void run(final IAudioRenderTarget target) throws RenderCommandException {
		final AudioFrame frame     = target.getFrame();
		final float      gain      = getVal(GAIN);
		final float      f         = getVal(F);
		final double     phi       = getVal(PHI);
		final float[]    samples   = frame.samples;
		final int        nChannels = frame.nChannels;
		final int        c         = channel % nChannels;
		final float      sRate     = frame.sRate;
		final long       sTime     = frame.sTime;
		for(int i = 0; i < samples.length; i += nChannels)
			samples[i+c] += gain * (float)Math.sin(phi + ((f * PI2 * ((sTime + i) / nChannels)) / sRate)); 
		
		frame.modified();
	}
}

