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

package ch.fhnw.ether.video;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.util.IDisposable;

public class FrameAccess implements IDisposable {
	private   final URLVideoSource src;
	private         IHostImage     hostImage;
	private         IGPUImage      gpuImage;
	protected int                  numPlays;

	FrameAccess(URLVideoSource src) throws IOException {
		this.hostImage = IHostImage.read(src.getURL());
		this.src       = src;
		this.numPlays  = 0;
	}

	FrameAccess(IHostImage frame) {
		this.hostImage = frame;
		this.src       = null;
		this.numPlays  = 0;
	}

	protected FrameAccess(URLVideoSource src, int numPlays) {
		this.hostImage = null;
		this.src       = src;
		this.numPlays  = numPlays;
	}

	public IHostImage getHostImage(BlockingQueue<float[]> audioData) {
		return hostImage;
	}
	
	public IGPUImage getGPUImage(BlockingQueue<float[]> audioData) {
		if (gpuImage == null)
			gpuImage = getHostImage(audioData).createGPUImage();
		return gpuImage;
	}

	protected int getWidth() {
		return hostImage.getWidth();
	}
	protected int getHeight() {
		return hostImage.getHeight();
	}
	
	protected float getFrameRate() {
		return AbstractFrameSource.FRAMERATE_UNKNOWN;
	}

	protected float getSampleRate() {
		return 0;
	}
	
	protected int getNumChannels() {
		return 0;
	}
	
	protected long getFrameCount() {
		return 1;
	}

	protected double getDuration() {
		return AbstractFrameSource.LENGTH_INFINITE;
	}

	public double getPlayOutTimeInSec() {
		return IScheduler.ASAP;
	}
	
	public URLVideoSource getSource() {
		return src;
	}

	public boolean decodeFrame() {
		return true;
	}

	public boolean isKeyframe() {
		return true;
	}
	
	@Override
	public void dispose() {
		// help GC
		hostImage = null; 
		gpuImage  = null;
	}
	
	public void rewind() throws IOException {}
	
	@Override
	public String toString() {
		return getSource().getURL() + " (d=" + getDuration() + " fr=" + getFrameRate() + " fc=" + getFrameCount() + " w=" + getWidth() + " h=" + getHeight() + ")";
	}
}
