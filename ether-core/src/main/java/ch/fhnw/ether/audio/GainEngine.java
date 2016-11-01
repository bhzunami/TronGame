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

/**
 * Get the level of a signal using averaging and attack/sustain/decay speeds
 */
public class GainEngine {
	
	private final static double MIN_LEVEL = AudioUtilities.dbToLevel(AudioUtilities.MIN_GAIN);
	private final static double MAX_LEVEL = AudioUtilities.dbToLevel(AudioUtilities.MAX_GAIN);
	
	private final  AverageBuffer history; // Contains squared sample values
	private int                  sustainSpeed; // Number of samples
	private double               attackSpeed; // increase per sample
	private double               decaySpeed; // decrease per sample
	private double               smoothedGain;
	private int                  sustainCountDown;
	private double               jumpLevel;
	private final float          sRate;
	private final int            numChannels;
	
	public GainEngine(float sampleRate, int numChannels, double historySizeInSeconds, double sustainSpeed, double attackSpeed, double decaySpeed, double jumpLevel) {
		this.history     = new AverageBuffer(sampleRate, numChannels, historySizeInSeconds);
		this.sRate       = sampleRate;
		this.numChannels = numChannels;
		setSustainSpeed(sustainSpeed);
		setAttackSpeed(attackSpeed);
		setDecaySpeed(decaySpeed);
		setJumpLevel(jumpLevel);
		this.smoothedGain = MAX_LEVEL;
	}

	public double getSustainSpeed() {
		return sustainSpeed / (sRate * numChannels);
	}

	public void setSustainSpeed(double sustainSpeed) {
		this.sustainSpeed = (int) (sustainSpeed * sRate * numChannels);
	}

	public double getAttackSpeed() {
		return attackSpeed * sRate;
	}

	public double getJumpLevel() {
		return jumpLevel;
	}

	public void setJumpLevel(double jumpLevel) {
		this.jumpLevel = jumpLevel;
	}

	public void setAttackSpeed(double attackSpeed) {
		this.attackSpeed = attackSpeed / sRate;
	}

	public double getDecaySpeed() {
		return decaySpeed * sRate;
	}

	public void setDecaySpeed(double decaySpeed) {
		this.decaySpeed = decaySpeed / sRate;
	}
	
	public void process(AudioFrame frame) {
		final float[] samples = frame.getMonoSamples();
		
		for (int i = 0; i < samples.length; i++) {
			history.push(samples[i] * samples[i]);
			double immediateGain = Math.sqrt(history.getAverage());
			if (immediateGain >= smoothedGain) {
				// Attack
				if (smoothedGain < jumpLevel)
					smoothedGain = immediateGain;
				else
					smoothedGain += attackSpeed;
				if (smoothedGain > immediateGain)
					smoothedGain = immediateGain;
				sustainCountDown = sustainSpeed;
			} else {
				if (sustainCountDown > 0) {
					// Sustain
					sustainCountDown--;
				} else {
					// Decay
					smoothedGain -= decaySpeed;
					if (smoothedGain < MIN_LEVEL)
						smoothedGain = MIN_LEVEL;
					if (smoothedGain < immediateGain)
						smoothedGain = immediateGain;
				}
			}
		}
	}
	
	public double getGain() {
		return smoothedGain;
	}

	public void reset() {
		history.reset();
		smoothedGain     = MAX_LEVEL;
		sustainCountDown = 0;
	}

}
