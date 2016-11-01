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

 import java.util.Arrays;

import ch.fhnw.ether.audio.AudioFrame;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.util.ClassUtilities;

 public class DCRemove extends AbstractRenderCommand<IAudioRenderTarget> {
	 final static float POLE = 0.9999f;

	 private float[] lastIn  = ClassUtilities.EMPTY_floatA;
	 private float[] lastOut = ClassUtilities.EMPTY_floatA;

	 @Override
	 protected void run(final IAudioRenderTarget target) throws RenderCommandException {
		 final AudioFrame frame = target.getFrame();
		 if(lastIn.length < frame.nChannels) {
			 lastIn  = Arrays.copyOf(lastIn,  frame.nChannels);
			 lastOut = Arrays.copyOf(lastOut, frame.nChannels);
		 }
		 final float[] samples = frame.samples;
		 for(int i = 0; i < samples.length; i ++) {
			 final int   c      = i % frame.nChannels;
			 final float sample = samples[i];
			 final float diff   = sample - lastIn[c];
			 final float intg   = POLE * lastOut[c] + diff;
			 lastIn[c]          = sample;
			 lastOut[c]         = intg;
			 samples[i]         = lastOut[c];
		 }

		 frame.modified();
	 }
 }
