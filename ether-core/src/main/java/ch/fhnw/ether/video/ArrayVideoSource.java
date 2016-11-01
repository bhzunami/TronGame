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

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.AbstractMediaTarget;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.media.Sync;

public class ArrayVideoSource extends AbstractFrameSource implements IVideoSource {
	private final int              width;
	private final int              height;
	private final float            frameRate;
	private final long             lengthInFrames;
	private final double           lengthInSeconds;
	private final List<VideoFrame> frames = new ArrayList<>();
	private int                    frameIdx;

	class Target extends AbstractVideoTarget {
		protected Target() {
			super(Thread.MAX_PRIORITY, null, false);
		}

		@Override
		public void render() throws RenderCommandException {
			synchronized (ArrayVideoSource.this) {
				VideoFrame frame = getFrame();
				frame.getHostImage();
				frames.add(frame);
			}
		}
	}

	public ArrayVideoSource(IVideoSource source) throws RenderCommandException {
		this(source, Sync.SYNC);
	}

	public ArrayVideoSource(IVideoSource source, Sync sync) throws RenderCommandException {
		if(source.getLengthInFrames() <= 0)
			throw new RenderCommandException("Source '" + source + "' has an invalid frame count (" + source.getLengthInFrames() +")");
		width           = source.getWidth();
		height          = source.getHeight();
		frameRate       = source.getFrameRate();
		lengthInFrames  = source.getLengthInFrames();
		lengthInSeconds = source.getLengthInSeconds();

		Target t = new Target();
		t.useProgram(new RenderProgram<>(source));
		t.start();
		if(sync == Sync.SYNC) {
			t.sleepUntil(IScheduler.NOT_RENDERING);
			t.stop();
		}
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public long getLengthInFrames() {
		return lengthInFrames;
	}

	@Override
	public double getLengthInSeconds() {
		return lengthInSeconds;
	}

	@Override
	public float getFrameRate() {
		return frameRate;
	}

	@Override
	protected void run(IRenderTarget<?> target) throws RenderCommandException {
		if(frameIdx >= frames.size()) frameIdx = 0;
		VideoFrame frame;
		for(;;) {
			synchronized (this) {
				if(!(frames.isEmpty())) break;
			}
			AbstractMediaTarget.nap();
		}
		synchronized (this) {
			if(frameIdx == 0) {
				double now = target.getTime();
				int idx = 0;
				for(VideoFrame f : frames) {
					f.playOutTime = now + idx / getFrameRate();
					idx++;
				}
			}
			frame = frames.get(frameIdx);
			frame.setLast(false);
		}
		((IVideoRenderTarget)target).setFrame(this, frame);
		frameIdx++;
	}

	@Override
	public long[] getShotStarts() {
		return new long[] {lengthInFrames};
	}
}
