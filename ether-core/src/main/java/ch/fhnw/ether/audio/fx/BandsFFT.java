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

import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.Smooth;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

 public class BandsFFT extends AbstractRenderCommand<IAudioRenderTarget> {
	 public enum Div {LINEAR, LOGARITHMIC}

	 private static final double BASE = 1.2;
	 private final float[]       freqs;
	 private final float[]       scales;
	 private final FFT           spectrum;
	 private int                 nHarmonics;
	 private Smooth              smooth;
	 private  float[]            power;


	 public BandsFFT(FFT fft, float ... freqs) {
		 this(fft, freqs, null);
	 }

	 public BandsFFT(FFT fft, float[] freqs, float[] scales) {
		 this.freqs    = freqs.clone();
		 this.scales   = new float[freqs.length - 1];
		 this.spectrum = fft;
		 if(scales == null)
			 Arrays.fill(this.scales, 1f);
		 else
			 System.arraycopy(scales, 0, this.scales, 0, Math.min(this.scales.length, scales.length));
	 }

	 public BandsFFT(FFT fft, float low, float high, int nBands, Div bands) {
		 this.freqs    = new float[nBands+1];
		 this.scales   = new float[nBands];
		 this.spectrum = fft;
		 switch(bands) {
		 case LINEAR:
			 float delta = (high - low) / nBands;
			 freqs[0] = low;
			 for(int i = 1; i < nBands+1; i++) {
				 low += delta;
				 freqs[i] = Math.min(low, high);
			 }
			 for(int i = 0; i < nBands; i++) {
				 float x = 2f * i / nBands;
				 x *= x;
				 scales[i] = 1f + x;
			 }
			 break;
		 case LOGARITHMIC:
			 double h = Math.pow(BASE, nBands) - 1;
			 double d = high - low;
			 for(int i = 0; i < nBands + 1; i++)
				 freqs[i] = Math.min(low + (float) ((d * (Math.pow(BASE, i)-1))   / h), high);
			 for(int i = 0; i < nBands; i++)
				 scales[i] = 1f;
			 break;
		 }
	 }

	 @Override
	 protected void init(IAudioRenderTarget target) throws RenderCommandException {
		 smooth = new Smooth(freqs.length - 1, 0.05f);
		 power  = new float[smooth.size()];
	 }

	 public void setHarmonics(int nHarmonics) {
		 this.nHarmonics = nHarmonics;
	 }

	 public int getHarmonics() {
		 return nHarmonics;
	 }

	 @Override
	 protected void run(final IAudioRenderTarget target) throws RenderCommandException {
		 final float[] spec = spectrum.power().clone();

		 AudioUtilities.multiplyHarmonics(spec, nHarmonics);

		 for(int band = 0; band < power.length; band++)
			 power[band] = scales[band] * spectrum.power(freqs[band], freqs[band+1], spec);

		 smooth.update(target.getTime(), power);			
	 }	

	 public float power(int i) {
		 return smooth.get(i);
	 }

	 public float[] power(float[] values) {
		 return smooth.get(values);
	 }
 }
