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

import ch.fhnw.util.math.MathUtilities;

public class Smooth {
	private final float[] values;
	private final double  decay;
	private double        lastUpdate = -1;
	private double        max;
	
	public Smooth(int nChannels, double decayInSecs) {
		this.values = new float[nChannels];
		this.decay  = decayInSecs;
	}

	public synchronized void update(double time, float ... values) {
		max *= 0.99;
		for(float v : values)
			max = Math.max(max, v);
		if(max == 0 || Double.isNaN(max)) max = 0.01f;
		
		if(lastUpdate > 0) {
			float gain = (float)Math.pow(decay, time - lastUpdate);
			for(int band = 0; band < values.length; band++)
				this.values[band] = (float) MathUtilities.clamp(Math.max(this.values[band] * gain, values[band]) / max, 0, 1);
			lastUpdate = time;
		} else {
			for(int band = 0; band < values.length; band++)
				this.values[band] = (float) MathUtilities.clamp(this.values[band] / max, 0, 1);
			lastUpdate = time;
		}
	}
	
	public synchronized float get(int band) {
		return values[band];
	}

	public int size() {
		return values.length;
	}

	public synchronized float[] get(float[] values) {
		System.arraycopy(this.values, 0, values, 0, Math.min(values.length, this.values.length));
		return values;
	}
}
