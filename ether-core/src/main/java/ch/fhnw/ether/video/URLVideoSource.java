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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.xuggle.xuggler.IContainerFormat;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.IAudioSource;
import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.AbstractMediaTarget;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.platform.IImageSupport;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.platform.Platform.OS;
import ch.fhnw.util.MIME;
import ch.fhnw.util.TextUtilities;

public class URLVideoSource extends AbstractFrameSource implements IAudioSource, IVideoSource {
	private static final boolean USE_JCODEC = Platform.getOS() == OS.WINDOWS;

	private int                          width;
	private int                          height;
	private float                        frameRate;
	private long                         lengthInFrames;
	private float                        sampleRate;
	private int                          numChannels;
	private double                       length;
	private double                       start;
	private URL                          url;
	private final FrameAccess            frameAccess;
	private final BlockingQueue<float[]> audioData = new LinkedBlockingQueue<>();
	private long                         samples;
	private final AtomicBoolean          startup   = new AtomicBoolean(true);
	private final IImageSupport          still     = Platform.get().getImageSupport();

	public URLVideoSource(URL url) throws IOException {
		this(url, Integer.MAX_VALUE);
	}

	public URLVideoSource(URL url, int numPlays) throws IOException {
		this(url, numPlays, 8);
	}

	public URLVideoSource(URL url, int numPlays, int queueSize) throws IOException {
		try {
			this.url    = url;
			frameAccess = access(url, numPlays, queueSize);
			init(url, 0, frameAccess.getDuration(), numPlays);
		} catch(Throwable t) {
			throw new IOException(t);
		}
	}

	public URLVideoSource(URL url, double startInSec, double lengthInSec, int numPlays, int queueSize) throws IOException {
		try {
			this.url    = url;
			frameAccess = access(url, numPlays, queueSize);
			init(url, startInSec, lengthInSec, numPlays);
		} catch(Throwable t) {
			throw new IOException(t);
		}
	}

	private FrameAccess access(URL url, int numPlays, int queueSize) throws IOException, URISyntaxException {
		String mime = MIME.getContentTypeFor(url);
		if(MIME.match(mime, MIME.MT_GIF))
			return new GIFAccess(this, numPlays);
		return still.canRead(mime) ? new FrameAccess(this) : USE_JCODEC ? new JCodecAccess(this, numPlays) : new XuggleAccess(this, numPlays, queueSize);
	}

	private void init(URL url, double startInSec, double lengthInSec, int numPlays) {
		width       = frameAccess.getWidth();
		height      = frameAccess.getHeight();
		frameRate   = frameAccess.getFrameRate();
		sampleRate  = frameAccess.getSampleRate();
		numChannels = frameAccess.getNumChannels();
		start       = startInSec;
		length      = lengthInSec;
		lengthInFrames  = lengthInSec == frameAccess.getDuration() ? frameAccess.getFrameCount() : (long)(lengthInSec * frameRate);
		if(startInSec != 0) throw new IllegalArgumentException("startTime != 0 not implemented yet");
	}

	@Override
	public String toString() {
		return frameAccess.toString();
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
	public float getFrameRate() {
		return frameRate;
	}

	@Override
	public long getLengthInFrames() {
		return lengthInFrames;
	}

	@Override
	public double getLengthInSeconds() {
		return length;
	}	

	public URL getURL() {
		return url;
	}

	@Override
	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public int getNumChannels() {
		return numChannels;
	}

	@Override
	protected void run(IRenderTarget<?> target) throws RenderCommandException {
		try {
			if(target instanceof IVideoRenderTarget) {
				if(target.isRealTime()) {
					double targetTime;
					do {
						targetTime = target.getTime();
						if(frameAccess.decodeFrame()) {
							if(frameAccess.getPlayOutTimeInSec() < targetTime && target instanceof AbstractMediaTarget)
								((AbstractMediaTarget<?,?>)target).incFrameCount();
						}
					} while(frameAccess.getPlayOutTimeInSec() < targetTime);
				} else
					frameAccess.decodeFrame();
				VideoFrame frame = new VideoFrame(frameAccess, audioData);
				startup.set(false);
				if(frameAccess.getPlayOutTimeInSec() > start + length)
					frameAccess.rewind();
				if(frameAccess.numPlays <= 0)
					frame.setLast(true);
				((IVideoRenderTarget)target).setFrame(this, frame);
			} else if(target instanceof IAudioRenderTarget && target.isRendering()) {
				float[] frameData = startup.get() ? audioData.poll() : audioData.poll(500, TimeUnit.MILLISECONDS);
				if(frameData == null) {
					((IAudioRenderTarget)target).setFrame(this, createAudioFrame(samples, 64));
					samples += 64;
				} else {
					((IAudioRenderTarget)target).setFrame(this, createAudioFrame(samples, frameData));
					samples += frameData.length;
				}
			}
		} catch(Throwable t) {
			throw new RenderCommandException(t);
		}
	}

	public double getPlayoutTimeInSec() {
		return frameAccess.getPlayOutTimeInSec();
	}

	private static Set<String> TYPES;

	public synchronized static boolean canRead(String mimeType) {
		if(TYPES == null) {
			TYPES = new HashSet<>();
			if(USE_JCODEC) {
				TYPES.add(MIME.MT_MOV);
				TYPES.add(MIME.MT_MP4);
				TYPES.add(MIME.MT_GIF);
			} else {
				String[][] types = MIME.getMimeTypes();
				for(IContainerFormat fmt : IContainerFormat.getInstalledInputFormats()) {
					for(String type : TextUtilities.split(fmt.getInputFormatShortName(), ','))
						TYPES.add(type(type, types));
				}
				TYPES.add(MIME.MT_MOV);
				TYPES.add(MIME.MT_GIF);
			}
		}
		return TYPES.contains(mimeType);
	}

	private static String type(String subType, String[][] types) {
		for(String[] mt : types)
			if(mt[1].equals(subType))
				return MIME.type(mt[0], mt[1]);
		return MIME.type(MIME.VIDEO, subType);
	}

	@Override
	public long[] getShotStarts() {
		if(frameAccess instanceof GIFAccess) {
			int[]  starts = ((GIFAccess)frameAccess).getShotStarts();
			long[] result = new long[starts.length];
			for(int i = 0; i < starts.length; i++)
				result[i] = starts[i];
			return result;
		}
		else return new long[] {lengthInFrames};
	}
}
