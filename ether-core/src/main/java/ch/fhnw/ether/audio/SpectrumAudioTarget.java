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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jtransforms.fft.FloatFFT_1D;

import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.media.AbstractFrame;
import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.ITimebase;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.util.Log;
import ch.fhnw.util.math.MathUtilities;

public class SpectrumAudioTarget implements IAudioRenderTarget {
	private static final Log LOG = Log.create();

	private final int                         numChannels;
	private final float                       sRate;
	private double                            sTime;
	private RenderProgram<IAudioRenderTarget> program;
	private final AtomicReference<AudioFrame> frame = new AtomicReference<>();
	private final FloatFFT_1D                 fft;
	private final BlockBuffer                 buffer;
	private final int                         fftSize;
	private final AtomicBoolean               isRendering = new AtomicBoolean();
	private       AudioFrame                  currentFrame;
	private       boolean                     done;
	private       long                        totalFrames;
	private       long                        relFrames;
	private       ITimebase                   timebase;
	
	public SpectrumAudioTarget(int numChannels, float sampleRate, float minFreq, Window windowType) {
		this.numChannels = numChannels;
		this.sRate       = sampleRate;
		this.fftSize     = MathUtilities.nextPowerOfTwo((int)(sRate / minFreq));
		LOG.info("FFT of " + fftSize + " at " + sRate + " Hz");
		this.fft         = new FloatFFT_1D(fftSize);
		this.buffer      = new BlockBuffer(fftSize, true, windowType);
	}

	@Override
	public void render() {
		sTime += getFrame().samples.length;
	}

	@Override
	public double getTime() {
		if(timebase != null) return timebase.getTime();
		return sTime / (getSampleRate() * getNumChannels());
	}

	@Override
	public int getNumChannels() {
		return numChannels;
	}

	@Override
	public float getSampleRate() {
		return sRate;
	}

	public float[] getSpectrum() {
		if(done) return null;
		isRendering.set(true);
		try {
			float[] result = buffer.nextBlock();
			if(result == null) {
				if(runOneCycle()) {
					for(result = buffer.nextBlock(); result == null; result = buffer.nextBlock())
						runOneCycle();

					if(currentFrame.isLast())
						done = true;
					fft.realForward(result);
					return result;
				}
			}
		} catch(Throwable t) {
			LOG.severe(t);
		} finally {
			isRendering.set(false);
		}
		return null;
	}

	private boolean runOneCycle() throws RenderCommandException {
		AbstractFrame tmp;
		program.runInternal(this);
		render();
		tmp = getFrame();
		if(tmp != null) {
			currentFrame = frame.getAndSet(null);
			buffer.add(currentFrame.getMonoSamples());
			return true;
		}
		return false;
	}

	@Override
	public boolean isRendering() {
		return isRendering.get();
	}

	@Override
	public void sleepUntil(double time) {}

	@Override
	public void sleepUntil(double time, Runnable callback) {}

	@Override
	public void start() throws RenderCommandException {}

	@Override
	public void stop() throws RenderCommandException {}

	@Override
	public IAudioSource getAudioSource() {
		return (IAudioSource)program.getFrameSource();
	}

	@Override
	public AudioFrame getFrame() {
		return frame.get();
	}

	@Override
	public void setFrame(AbstractFrameSource src, AudioFrame frame) {
		this.frame.set(frame);
	}

	@Override
	public void useProgram(RenderProgram<IAudioRenderTarget> program) throws RenderCommandException {
		this.program = program;
		program.setTarget(this);
	}

	@Override
	public final long getTotalElapsedFrames() {
		return totalFrames;
	}
	
	@Override
	public final long getRelativeElapsedFrames() {
		return relFrames;
	}
	
	@Override
	public void setTimebase(ITimebase timebase) {
		this.timebase = timebase;
	}
	
	@Override
	public boolean isRealTime() {
		return timebase == null ? false : timebase.isRealTime(); 
	}
}
