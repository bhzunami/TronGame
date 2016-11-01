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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.util.Pair;
import ch.fhnw.util.TextUtilities;

public class JavaSoundSource extends AbstractFrameSource implements Runnable, IAudioSource {
	private static final float       S2F    = Short.MAX_VALUE;

	private final float sampleRate;
	private final int   nChannels;
	private final int   frameSize;

	private static final List<Pair<Mixer.Info, Line.Info>> sources = new ArrayList<>();

	private static final Parameter SOURCE = new Parameter("src", "Source", 0, getSources());

	private int                          source = -1;
	private TargetDataLine               line;
	private final byte[]                 buffer;
	long                                 samples;
	Throwable                            lastErr;
	private final BlockingQueue<float[]> data = new LinkedBlockingQueue<>();
	Thread                               t;

	public JavaSoundSource(int nChannels, float sampleRate, int frameSize) {
		super(SOURCE);
		this.nChannels  = nChannels;
		this.sampleRate = sampleRate;
		this.frameSize  = Math.max(1, (frameSize / nChannels) * nChannels);
		this.buffer     = new byte[frameSize * 2];
	}

	@Override
	public void run() {
		for(;;) {
			try {
				final float[] fbuffer = new float[frameSize];
				final int read = line.read(buffer, 0, buffer.length);
				int       idx  = 0;
				for(int i = 0; i < read; i += 2) {
					int s = buffer[i] << 8 | (buffer[i+1] & 0xFF);
					fbuffer[idx++] = s / S2F;
				}
				samples += read / 2;
				data.add(fbuffer);
			} catch(Throwable t) {
				if(lastErr == null)
					lastErr = t;
			}
		}
	}

	@Override
	protected void run(IRenderTarget<?> target) throws RenderCommandException {
		if(lastErr != null) throw new RenderCommandException(lastErr);
		int pSrc = (int) getVal(SOURCE);
		if(source != pSrc) {
			try {
				close();
				line = (TargetDataLine)AudioSystem.getLine(sources.get(pSrc).second);
				line.open(new AudioFormat(sampleRate, 16, nChannels, true, true), buffer.length);
				line.start();
				source = pSrc;
				if(t == null) {
					t = new Thread(this, "Audio Input");
					t.setDaemon(true);
					t.setPriority(Thread.MAX_PRIORITY);
					t.start();
				}
			} catch (LineUnavailableException e) {
				throw new RenderCommandException(e);
			}
		}
		try {
			while(data.size() > 4) data.take();
			((IAudioRenderTarget)target).setFrame(this, createAudioFrame(samples, data.take()));
		} catch(InterruptedException e) {
			throw new RenderCommandException(e);
		}
	}	

	@Override
	public long getLengthInFrames() {
		return FRAMECOUNT_UNKNOWN;
	}

	@Override
	public double getLengthInSeconds() {
		return LENGTH_INFINITE;
	}

	@Override
	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public float getFrameRate() {
		double result = (frameSize / getNumChannels()) * sampleRate;
		return (float)result;
	}

	@Override
	public int getNumChannels() {
		return nChannels;
	}

	public void close() {
		if(line != null) {
			line.stop();
			line.close();
		}
	}

	public synchronized static String[] getSources() {
		if(sources.isEmpty()) {
			for(Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
				try(Mixer mixer = AudioSystem.getMixer(mixerInfo)) {
					for(Line.Info lineInfo : mixer.getTargetLineInfo()) {
						if(TargetDataLine.class.isAssignableFrom(lineInfo.getLineClass()))
							sources.add(new Pair<>(mixerInfo, lineInfo));
					}
				}
			}
		}

		String[] result = new String[sources.size()];
		int idx = 0;
		for(Pair<Mixer.Info, Line.Info> src : sources)
			result[idx++] = src.first.getName();
		return result;
	}

	@Override
	public String toString() {
		return TextUtilities.getShortClassName(this) + ": " + ((line == null || line.getLineInfo() == null)  ?
				sampleRate + " Hz, " + nChannels + " channels, " + frameSize + " samples/frame" :
					line.getLineInfo().toString());

	}
}
