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
 */package ch.fhnw.ether.audio.fx;

 import ch.fhnw.ether.audio.ButterworthFilter;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.RenderCommandException;

 public class BandPass extends AbstractRenderCommand<IAudioRenderTarget> {
	 private static final Parameter LOW  = new Parameter("low",  "Low",  0, 20000, 30);
	 private static final Parameter HIGH = new Parameter("high", "High", 0, 20000, 20000);

	 private ButterworthFilter[][] bandPass;

	 float lowOld  = -1;
	 float highOld = -1;
	 private final int strength;

	 public BandPass(int strength) {
		 super(LOW, HIGH);
		 this.strength = strength;
	 }

	 @Override
	 protected void run(final IAudioRenderTarget target) throws RenderCommandException {
		 final float              low       = getVal(LOW);
		 final float              high      = getVal(HIGH);
		 final int                nChannels = target.getNumChannels();
		 final float[]            samples   = target.getFrame().samples;

		 if(low != lowOld || high != highOld) {
			 bandPass = new ButterworthFilter[nChannels][strength];
			 for(int i = 0; i < nChannels; i++)
				 for(int j = 0; j < strength; j++)
					 bandPass[i][j] = ButterworthFilter.getBandpassFilter(target.getSampleRate(), low, high);
			 lowOld  = low;
			 highOld = high;
		 }

		 for(int i = 0; i < samples.length; i += nChannels)
			 for(int c = 0; c < nChannels; c++)
				 for(int j = 0; j < strength; j++)
					 samples[i+c] = bandPass[c][j].process(samples[i+c]);

		 target.getFrame().modified();
	 }	
 }
