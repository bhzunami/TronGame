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

 import ch.fhnw.ether.audio.AudioFrame;
 import ch.fhnw.ether.audio.AudioUtilities;
 import ch.fhnw.ether.audio.GainEngine;
 import ch.fhnw.ether.audio.IAudioRenderTarget;
 import ch.fhnw.ether.media.AbstractRenderCommand;
 import ch.fhnw.ether.media.Parameter;
 import ch.fhnw.ether.media.RenderCommandException;
 import ch.fhnw.ether.ui.IPlotable;
import ch.fhnw.util.color.RGB;

 public class AutoGain extends AbstractRenderCommand<IAudioRenderTarget> implements IPlotable {
	 public static final Parameter TARGET  = new Parameter("gain",    "Gain [dB]", -120, 0,  -10);
	 public static final Parameter ATTACK  = new Parameter("attack",  "Attack",       0, 1,   0.3f);
	 public static final Parameter SUSTAIN = new Parameter("sustain", "Sustain",      0, 10,  5);
	 public static final Parameter DECAY   = new Parameter("decay",   "Decay",        0, 1,   0.1f);

	 private final static double MAX2AVG      = 0.5;
	 private static final double SMOOTH_DELAY = 0.1;
	 private static final double MIN_LEVEL    = AudioUtilities.dbToLevel(-40.0);
	 private static final double ACCURACY     = AudioUtilities.dbToLevel(1.0); // Width of 'void' range, where no correction occurs

	 private GainEngine gainEngine;
	 private float      correction;

	 public AutoGain() {
		 super(TARGET, ATTACK, SUSTAIN, DECAY);
	 }

	 @Override
	 protected void init(IAudioRenderTarget target) {
		 gainEngine   = new GainEngine(target.getSampleRate(), target.getNumChannels(), SMOOTH_DELAY, getVal(SUSTAIN), getVal(ATTACK), getVal(DECAY), MIN_LEVEL);
	 }

	 public float gain() {
		 return correction;
	 }

	 public void reset() {
		 gainEngine.reset();
	 }

	 @Override
	 protected void run(final IAudioRenderTarget target) throws RenderCommandException {
		 final AudioFrame frame = target.getFrame();

		 gainEngine.setAttackSpeed(getVal(ATTACK));
		 gainEngine.setSustainSpeed(getVal(SUSTAIN));
		 gainEngine.setDecaySpeed(getVal(DECAY));

		 double targetUpper    = AudioUtilities.dbToLevel(getVal(TARGET));
		 double targetLower    = targetUpper / ACCURACY;
		 double thresholdLevel = MIN_LEVEL * MAX2AVG;

		 gainEngine.process(frame);

		 double gain = gainEngine.getGain();
		 if (gain < thresholdLevel)
			 gain = thresholdLevel;

		 float correction = 1.0f;
		 if (gain < targetLower)
			 correction = (float)(targetLower / gain);
		 else if (gain > targetUpper)
			 correction = (float)(gain / targetUpper);

		 this.correction = correction;
		 final float[] samples = frame.samples;
		 for (int i = 0; i < samples.length; i++)
			 samples[i] *= correction;

		 frame.modified(); 

		 clear();
		 bar(AudioUtilities.energy(frame.samples), RGB.GRAY);
		 point(correction, 0, 15, RGB.RED);
	 }
 }
