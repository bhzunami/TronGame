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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

import org.jcodec.api.JCodecException;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Picture8Bit;

import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.util.Log;

public final class JCodecAccess extends FrameAccess {
	private static final Log log = Log.create();

	private   SeekableByteChannel channel;
	protected FrameGrab           grab;

	public JCodecAccess(URLVideoSource src, int numPlays) throws IOException, URISyntaxException {
		super(src, numPlays);
		this.channel = NIOUtils.readableFileChannel(new File(src.getURL().toURI()));
		try {
			this.grab = new FrameGrab(channel);
		} catch(JCodecException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void dispose() {
		try {
			channel.close();
		} catch (IOException e) {
		}
		this.channel = null;
		this.grab    = null;
	}

	@Override
	public double getDuration() {
		return grab.sdt().getMeta().getTotalDuration();
	}

	@Override
	public float getFrameRate() {
		double result = getFrameCount() / getDuration();
		return (float)result;
	}

	@Override
	public long getFrameCount() {
		return grab.sdt().getMeta().getTotalFrames();
	}

	@Override
	public int getWidth() {
		return grab.getMediaInfo().getDim().getWidth();
	}

	@Override
	public int getHeight() {
		return grab.getMediaInfo().getDim().getHeight();
	}

	@Override
	public void rewind() {
		try {
			numPlays--;
			grab.seekToFramePrecise(0);
		} catch (Throwable t) {
			log.warning(t);
		}
	}

	static final int ATTR_PLAYOUT_TIME = 0;
	static final int ATTR_BASE_TIME    = 1;
	static final int ATTR_IS_KEYFRAME  = 2;

	private Picture8Bit currentPicture;
	private double[]    attrs = new double[3];
	@Override
	public boolean decodeFrame() {
		try {
			currentPicture = grab.decode(attrs);
			if(currentPicture == null) {
				rewind();
				attrs[ATTR_BASE_TIME]    = attrs[ATTR_PLAYOUT_TIME];
				attrs[ATTR_PLAYOUT_TIME] = 0;
				currentPicture = grab.decode(attrs);
			}
			return currentPicture != null;
		} catch(Throwable t) {
			return false;
		}
	}

	@Override
	public double getPlayOutTimeInSec() {
		return attrs[ATTR_PLAYOUT_TIME] + attrs[ATTR_BASE_TIME];
	}

	@Override
	public boolean isKeyframe() {
		return attrs[ATTR_IS_KEYFRAME] != 0;
	}

	@Override
	public IHostImage getHostImage(BlockingQueue<float[]> audioData) {
		IHostImage result = IHostImage.create(getWidth(), getHeight(), ComponentType.BYTE, ComponentFormat.RGB);
		grab.grabAndSet(currentPicture, result, audioData);
		return result;
	}

	@Override
	public IGPUImage getGPUImage(BlockingQueue<float[]> audioData) {
		return getHostImage(audioData).createGPUImage();
	}

	@Override
	protected int getNumChannels() {
		return grab.getNumChannels();
	}

	@Override
	protected float getSampleRate() {
		return grab.getSampleRate();
	}	
}
