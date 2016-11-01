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

import java.util.BitSet;

import javax.sound.sampled.AudioFormat;


public final class AudioUtilities {
	private static final float S2F = Short.MAX_VALUE;

	public enum Window {RECTANGLE, HANN, HAMMING}

	public final static double MIN_GAIN = -100.0;
	public final static double MAX_GAIN = 20.0;

	private final static double PI2 = Math.PI * 2.0;

	public static int log2(int size) {
		assert (size & (size - 1)) == 0: "size must be a power of two";
		int log = 0;
		if (size >= (1 << 16)) {
			log += 16;
			size >>>= 16;
		}
		if (size >= (1 << 8)) {
			log += 8;
			size >>>= 8;
		}
		if (size >= (1 << 4)) {
			log += 4;
			size >>>= 4;
		}
		if (size >= (1 << 2)) {
			log += 2;
			size >>>= 2;
		}
		if (size >= 2)
			log++;
		return log;
	}

	public static double dbToLevel(double value) {
		return Math.min(1, Math.pow(10.0, value / 20.0));
	}

	public static double levelToDb(double value) {
		return 20.0 * Math.log10(value);
	}

	public static double dbToLevel0(double value) {
		if (value <= MIN_GAIN)
			return 0.0;
		return dbToLevel(value);
	}

	public static double levelToDb0(double value) {
		double result = levelToDb(value);
		if (result < MIN_GAIN)
			return MIN_GAIN;
		return result;
	}

	public static double dbToPowerLevel(double value) {
		return Math.pow(10.0, value / 10.0);
	}

	public static double powerLevelToDb(double value) {
		return 10.0 * Math.log10(value);
	}

	public static double powerLevelToDb0(double value) {
		double result = powerLevelToDb(value);
		if (result < MIN_GAIN)
			return MIN_GAIN;
		return result;
	}

	public static float energy(float[] samples) {
		double total = 0.0f;
		for (float value : samples)
			total += value < 0 ? -value : value;
		return (float)total / samples.length;
	}

	public static double gcd(double a, double b) {
		double epsilon = 1.0 / (1L << 32);
		while (a > epsilon) {
			if (a < b) {
				// Swap
				double swap = a;
				a = b;
				b = swap;
			}
			a = a % b;
		}
		return b;
	}

	public static double gcd(double... values) {
		if (values.length == 0)
			throw new IllegalArgumentException("gcd() requires at least one value");
		double result = values[0];
		for (int i = 1; i < values.length; i++)
			result = gcd(result, values[i]);
		return result;
	}

	public static void applyWindow(Window windowType, int nChannels, float[] data) {
		int nSamples1 = (data.length / nChannels) - 1;
		switch(windowType){
		case HANN:{
			for(int i = 0; i < data.length; i++) {
				int c = i / nChannels;
				data[i] = data[i] * (float)(1.0 - Math.cos(c*PI2/nSamples1));
			}
			break;
		}
		case HAMMING:{
			for(int i = 0; i < data.length; i++) {
				int c = i / nChannels;
				data[i] = data[i] * (float)(0.53836 - 0.46164 * Math.cos(c*PI2/nSamples1));
			}
			break;
		}
		case RECTANGLE: 
			break;
		}
	}

	/**
	 * Return the peak locations as array index for the series.
	 * @param windowSize  the window size to look for peaks. a neighborhood of +/- windowSize
	 * will be inspected to search for peaks. Typical values start at 3.
	 * @param stringency  threshold for peak values. Peak with values lower than <code>
	 * mean + stringency * std</code> will be rejected. <code>Mean</code> and <code>std</code> are calculated on the 
	 * spikiness function. Typical values range from 1 to 3.
	 * @return an int array, with one element by retained peak, containing the index of 
	 * the peak in the time series array.
	 */
	public static BitSet peaks(final float[] T, final int windowSize, final float stringency) {
		// Compute peak function values
		float[] S = new float[T.length];
		float maxLeft, maxRight;
		for (int i = windowSize; i < S.length - windowSize; i++) {

			maxLeft = T[i] - T[i-1];
			maxRight = T[i] - T[i+1];
			for (int j = 2; j <= windowSize; j++) {
				if (T[i]-T[i-j] > maxLeft)
					maxLeft = T[i]-T[i-j];
				if (T[i]-T[i+j] > maxRight)
					maxRight = T[i]-T[i+j];
			}
			S[i] = 0.5f * (maxRight + maxLeft);

		}

		// Compute mean and std of peak function
		float mean = 0;
		int   n    = 0;
		float M2   = 0;
		float delta;
		for (int i = 0; i < S.length; i++) {
			n = n + 1;
			delta = S[i] - mean;
			mean = mean + delta/n;
			M2 = M2 + delta*(S[i] - mean) ;
		}

		float variance = M2/(n - 1);
		float std = (float) Math.sqrt(variance);

		// Collect only large peaks
		BitSet result = new BitSet();
		for (int i = 0; i < S.length; i++) {
			if (S[i] > 0 && (S[i]-mean) > stringency * std) {
				result.set(i);
			}
		}

		// Remove peaks too close
		BitSet toPrune = new BitSet();
		int    peak1;
		int    peak2;
		int    weakerPeak;

		for (int i = result.nextSetBit(0);;) {
			peak1 = i;
			peak2 = result.nextSetBit(i+1);
			if(peak2 < 0) break;

			if (peak2 - peak1 < windowSize) {
				// Too close, prune the smallest one
				if (T[peak2] > T[peak1])
					weakerPeak = peak1;
				else 
					weakerPeak = peak2;
				toPrune.set(weakerPeak);
			}
			i = peak2;
		}
		result.andNot(toPrune);

		return result;
	}

	public static float[] multiplyHarmonics(float[] powerSpectrumInOut, int nHarmonics) {
		for(int h = 0; h < nHarmonics; h++) {
			final int hop = h + 1;
			final int lim = powerSpectrumInOut.length / hop;
			for(int i = 0; i < lim; i++)
				powerSpectrumInOut[i] *= powerSpectrumInOut[i * hop];
		}			
		return powerSpectrumInOut;
	}
	
	public static float[] pcmBytes2float(AudioFormat fmt, byte[] src, int length) {
		float[] fbuffer = new float[length / (fmt.getSampleSizeInBits() / 8)];
		int     idx     = 0;
		if(fmt.isBigEndian()) {
			for(int i = 0; i < length; i += 2) {
				int s = src[i] << 8 | (src[i+1] & 0xFF);
				fbuffer[idx++] = s / S2F;
			}
		} else {
			for(int i = 0; i < length; i += 2) {
				int s = src[i+1] << 8 | (src[i] & 0xFF);
				fbuffer[idx++] = s / S2F;
			}
		}
		return fbuffer;
	}
}
