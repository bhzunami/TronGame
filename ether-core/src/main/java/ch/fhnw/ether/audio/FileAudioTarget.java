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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.util.FloatList;

public class FileAudioTarget extends AbstractAudioTarget {
	private final int        numChannels;
	private final float      sRate;
	private double           sTime;
	private final File       file;
	private final FloatList  buffer = new FloatList();
	
	public FileAudioTarget(File file, int numChannels, float sampleRate) {
		super(Thread.NORM_PRIORITY, false);
		this.numChannels = numChannels;
		this.sRate       = sampleRate;
		this.file        = file;
	}

	@Override
	public void render() {
		sTime += getFrame().samples.length;
		buffer.addAll(getFrame().samples);
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

	@Override
	public void stop() throws RenderCommandException {
		byte[] bytes = new byte[buffer.size() * 2];
		for(int i = buffer.size(); --i >= 0;) {
			int val = (int) (buffer.get(i) * Short.MAX_VALUE);
			bytes[i*2+0] = (byte) val;
			bytes[i*2+1] = (byte) (val >> 8);
		}
		AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(bytes), 
				new AudioFormat(sRate, 16, 1, true, false), 
				buffer.size());
		try(FileOutputStream out = new FileOutputStream(file)) {
			AudioSystem.write(in, Type.WAVE, out);
		} catch (IOException e) {
			throw new RenderCommandException(e);
		}
		super.stop();
	}
}
