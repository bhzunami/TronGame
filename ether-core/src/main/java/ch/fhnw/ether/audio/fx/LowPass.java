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

 public class LowPass extends AbstractRenderCommand<IAudioRenderTarget> {
	 private static final Parameter FREQ = new Parameter("f",  "Frequency",  0, 20000, 30);

	 private ButterworthFilter[][] lowPass;

	 float freqOld  = -1;
	 private final int strength;

	 public LowPass(int strength) {
		 super(FREQ);
		 this.strength = strength;
	 }

	 @Override
	 protected void run(final IAudioRenderTarget target) throws RenderCommandException {
		 final float              freq      = getVal(FREQ);
		 final int                nChannels = target.getNumChannels();
		 final float[]            samples   = target.getFrame().samples;

		 if(freq != freqOld) {
			 lowPass = new ButterworthFilter[nChannels][strength];
			 for(int i = 0; i < nChannels; i++)
				 for(int j = 0; j < strength; j++)
					 lowPass[i][j] = ButterworthFilter.getLowpassFilter(target.getSampleRate(), freq);
			 freqOld  = freq;
		 }

		 for(int i = 0; i < samples.length; i += nChannels)
			 for(int c = 0; c < nChannels; c++)
				 for(int j = 0; j < strength; j++)
					 samples[i+c] = lowPass[c][j].process(samples[i+c]);

		 target.getFrame().modified();
	 }	
 }
