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
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import org.jcodec.api.JCodecException;
import org.jcodec.api.MediaInfo;
import org.jcodec.api.UnsupportedFormatException;
import org.jcodec.api.specific.AVCMP4Adaptor;
import org.jcodec.api.specific.ContainerAdaptor;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.s302.S302MDecoder;
import org.jcodec.common.AudioDecoder;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.JCodecUtil.Format;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform8Bit;

import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.util.Log;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Extracts frames from a movie into uncompressed images suitable for
 * processing.
 * 
 * Supports going to random points inside of a movie ( seeking ) by frame number
 * of by second.
 * 
 * NOTE: Supports only AVC ( H.264 ) in MP4 ( ISO BMF, QuickTime ) at this
 * point.
 * 
 * EtherGL: some additions for our needs.
 * 
 * @author The JCodec project
 * 
 */
public class FrameGrab {
	private static final Log LOG = Log.create();

	private final AbstractMP4DemuxerTrack videoTrack;
	private final AbstractMP4DemuxerTrack audioTrack;
	private       ContainerAdaptor        decoder;
	private final ThreadLocal<byte[][]>   buffers = new ThreadLocal<>();
	private long                          seekPos = -1;
	private final AudioFormat             audioInfo;
	@SuppressWarnings("unused")
	private final AudioDecoder            audioDecoder;
	@SuppressWarnings("unused")
	private final ByteBuffer              audioBuffer;

	public FrameGrab(SeekableByteChannel in) throws IOException, JCodecException {
		ByteBuffer header = ByteBuffer.allocate(65536);
		in.read(header);
		header.flip();
		Format detectFormat = JCodecUtil.detectFormat(header);

		switch (detectFormat) {
		case MOV:
			MP4Demuxer d1 = new MP4Demuxer(in);
			videoTrack = d1.getVideoTrack();
			if(!(d1.getAudioTracks().isEmpty())) {
				audioTrack = d1.getAudioTracks().get(0);
				AudioSampleEntry as = (AudioSampleEntry) audioTrack.getSampleEntries()[0]; 
				audioInfo = new AudioFormat(
						as.getFormat().isSigned() ? Encoding.PCM_SIGNED : Encoding.PCM_UNSIGNED, 
								as.getFormat().getSampleRate(),
								as.getFormat().getSampleSizeInBits(), 
								as.getFormat().getChannels(),
								as.getFormat().getFrameSize(),
								as.getFormat().getFrameRate(), 
								as.getFormat().isBigEndian());
				audioDecoder = audioDecoder(as.getFourcc());
				audioBuffer  = ByteBuffer.allocate(96000 * audioInfo.getFrameSize() * 10);
			} else {
				audioTrack   = null;
				audioInfo    = null;
				audioDecoder = null;
				audioBuffer  = null;
			}
			break;
		case MPEG_PS:
			throw new UnsupportedFormatException("MPEG PS is temporarily unsupported.");
		case MPEG_TS:
			throw new UnsupportedFormatException("MPEG TS is temporarily unsupported.");
		default:
			throw new UnsupportedFormatException("Container format is not supported by JCodec");
		}
		decodeLeadingFrames();
	}

	SeekableDemuxerTrack sdt() {
		return videoTrack;
	}

	/**
	 * Position frame grabber to a specific second in a movie. As a result the
	 * next decoded frame will be precisely at the requested second.
	 * 
	 * WARNING: potentially very slow. Use only when you absolutely need precise
	 * seek. Tries to seek to exactly the requested second and for this it might
	 * have to decode a sequence of frames from the closes key frame. Depending
	 * on GOP structure this may be as many as 500 frames.
	 * 
	 * @param second
	 * @return
	 * @throws IOException
	 * @throws JCodecException
	 */
	public FrameGrab seekToSecondPrecise(double second) throws IOException, JCodecException {
		sdt().seek(second);
		decodeLeadingFrames();
		return this;
	}

	/**
	 * Position frame grabber to a specific frame in a movie. As a result the
	 * next decoded frame will be precisely the requested frame number.
	 * 
	 * WARNING: potentially very slow. Use only when you absolutely need precise
	 * seek. Tries to seek to exactly the requested frame and for this it might
	 * have to decode a sequence of frames from the closes key frame. Depending
	 * on GOP structure this may be as many as 500 frames.
	 * 
	 * @param frameNumber
	 * @return
	 * @throws IOException
	 * @throws JCodecException
	 */
	public FrameGrab seekToFramePrecise(int frameNumber) throws IOException, JCodecException {
		sdt().gotoFrame(frameNumber);
		decodeLeadingFrames();
		return this;
	}

	/**
	 * Position frame grabber to a specific second in a movie.
	 * 
	 * Performs a sloppy seek, meaning that it may actually not seek to exact
	 * second requested, instead it will seek to the closest key frame
	 * 
	 * NOTE: fast, as it just seeks to the closest previous key frame and
	 * doesn't try to decode frames in the middle
	 * 
	 * @param second
	 * @return
	 * @throws IOException
	 * @throws JCodecException
	 */
	public FrameGrab seekToSecondSloppy(double second) throws IOException, JCodecException {
		sdt().seek(second);
		goToPrevKeyframe();
		return this;
	}

	/**
	 * Position frame grabber to a specific frame in a movie
	 * 
	 * Performs a sloppy seek, meaning that it may actually not seek to exact
	 * frame requested, instead it will seek to the closest key frame
	 * 
	 * NOTE: fast, as it just seeks to the closest previous key frame and
	 * doesn't try to decode frames in the middle
	 * 
	 * @param frameNumber
	 * @return
	 * @throws IOException
	 * @throws JCodecException
	 */
	public FrameGrab seekToFrameSloppy(int frameNumber) throws IOException, JCodecException {
		sdt().gotoFrame(frameNumber);
		goToPrevKeyframe();
		return this;
	}

	private void goToPrevKeyframe() throws IOException {
		sdt().gotoFrame(detectKeyFrame((int) sdt().getCurFrame()));
	}

	private void decodeLeadingFrames() throws IOException, JCodecException {
		SeekableDemuxerTrack sdt = sdt();

		int curFrame = (int) sdt.getCurFrame();
		int keyFrame = detectKeyFrame(curFrame);
		sdt.gotoFrame(keyFrame);

		Packet frame = sdt.nextFrame();
		decoder = detectDecoder(sdt, frame);

		while (frame.getFrameNo() < curFrame) {
			decoder.decodeFrame8Bit(frame, getBuffer());
			frame = sdt.nextFrame();
		}
		sdt.gotoFrame(curFrame);
	}

	private byte[][] getBuffer() {
		byte[][] buf = buffers.get();
		if (buf == null) {
			buf = decoder.allocatePicture8Bit();
			buffers.set(buf);
		}
		return buf;
	}

	private int detectKeyFrame(int start) {
		int[] seekFrames = videoTrack.getMeta().getSeekFrames();
		if (seekFrames == null)
			return start;
		int prev = seekFrames[0];
		for (int i = 1; i < seekFrames.length; i++) {
			if (seekFrames[i] > start)
				break;
			prev = seekFrames[i];
		}
		return prev;
	}

	private ContainerAdaptor detectDecoder(SeekableDemuxerTrack videoTrack, Packet frame) throws JCodecException {
		if (videoTrack instanceof AbstractMP4DemuxerTrack) {
			SampleEntry se = ((AbstractMP4DemuxerTrack) videoTrack).getSampleEntries()[((MP4Packet) frame).getEntryNo()];
			VideoDecoder byFourcc = videoDecoder(se.getHeader().getFourcc());
			if (byFourcc instanceof H264Decoder)
				return new AVCMP4Adaptor(((AbstractMP4DemuxerTrack) videoTrack).getSampleEntries());
		}
		throw new UnsupportedFormatException("Codec is not supported");
	}

	private VideoDecoder videoDecoder(String fourcc) {
		if (fourcc.equals("avc1"))
			return new H264Decoder();
		else if (fourcc.equals("m1v1") || fourcc.equals("m2v1"))
			return new MPEGDecoder();
		else if (fourcc.equals("apco") || fourcc.equals("apcs") || fourcc.equals("apcn") || fourcc.equals("apch") || fourcc.equals("ap4h"))
			return new ProresDecoder();
		LOG.info("No video decoder for '" + fourcc + "'");
		return null;
	}

	private AudioDecoder audioDecoder(String fourcc) {
		if ("sowt".equals(fourcc) || "in24".equals(fourcc) || "twos".equals(fourcc) || "in32".equals(fourcc))
			return new PCMDecoder(audioInfo);
		else if ("s302".equals(fourcc))
			return new S302MDecoder(); /*
		else if(AACDecoder.canDecode(profile))
			return new AACDecoder(decoderSpecificInfo, audioInfo); */
		LOG.info("No audio decoder for '" + fourcc + "'");
		return null;
	}

	/**
	 * Get frame at current position in JCodec native image
	 * 
	 * @return
	 * @throws IOException
	 */
	public Picture8Bit getNativeFrame() throws IOException {
		Packet frame = videoTrack.nextFrame();
		if (frame == null)
			return null;
		return decoder.decodeFrame8Bit(frame, getBuffer());
	}

	/**
	 * Gets info about the media
	 * 
	 * @return
	 */
	public MediaInfo getMediaInfo() {
		return decoder.getMediaInfo();
	}

	public boolean skipFrame() {
		if(seekPos < 0)
			seekPos = sdt().getCurFrame();
		seekPos++;
		return seekPos < sdt().getMeta().getTotalFrames();
	}

	public Picture8Bit decode(double[] playOutTime) {
		try {
			Packet pkt;
			SeekableDemuxerTrack sdt = sdt();
			if(seekPos > 0) {
				sdt.gotoFrame(seekPos);

				int curFrame = (int) sdt.getCurFrame();
				int keyFrame = detectKeyFrame(curFrame);
				sdt.gotoFrame(keyFrame);

				pkt = sdt.nextFrame();

				while (pkt.getFrameNo() <= curFrame) {
					decoder.decodeFrame8Bit(pkt, getBuffer());
					pkt = sdt.nextFrame();
				}
				sdt.gotoFrame(curFrame);
				seekPos = -1;
			} else
				pkt = sdt.nextFrame();

			playOutTime[JCodecAccess.ATTR_PLAYOUT_TIME] = pkt.getPtsD();
			playOutTime[JCodecAccess.ATTR_IS_KEYFRAME]  = pkt.isKeyFrame() ? 1 : 0;
			
			return decoder.decodeFrame8Bit(pkt, getBuffer());
		} catch(Throwable t) {
			return null;
		}
	}

	public void grabAndSet(Picture8Bit src, IHostImage frame, BlockingQueue<float[]> audioData) {
		if (src.getColor() != ColorSpace.RGB) {
			Picture8Bit   rgb       = Picture8Bit.create(src.getWidth(), src.getHeight(), ColorSpace.RGB, src.getCrop());
			Transform8Bit transform = ColorUtil.getTransform8Bit(src.getColor(), rgb.getColor());
			transform.transform(src, rgb);
			src = rgb;
		}

		final int        numComponents = frame.getComponentFormat().getNumComponents();
		final ByteBuffer pixels        = frame.getPixels();
		final byte[]     srcData       = src.getPlaneData(0);
		final int        line          = frame.getWidth() * numComponents;

		pixels.clear();
		if(numComponents == 4) {
			for(int j = frame.getHeight(); --j >= 0;) {
				int idx = j * line;
				for (int i = frame.getWidth(); --i >= 0;) {
					pixels.put((byte) (srcData[idx+2] + 128));
					pixels.put((byte) (srcData[idx+1] + 128));
					pixels.put((byte) (srcData[idx+0] + 128));
					pixels.put((byte) 0xFF);
					idx += 3;
				}
			}
		} else {
			for(int j = frame.getHeight(); --j >= 0;) {
				int idx = j * line;
				for (int i = frame.getWidth(); --i >= 0;) {
					pixels.put((byte) (srcData[idx+2] + 128));
					pixels.put((byte) (srcData[idx+1] + 128));
					pixels.put((byte) (srcData[idx+0] + 128));
					idx += 3;
				}
			}
		}

		/*
			// --- audio
			long audioFrameNo = (audioTrack.getFrameCount() * pkt.getFrameNo()) / videoTrack.getFrameCount();
			if(audioDecoder != null) {
				while(audioTrack.getCurFrame() < audioFrameNo) {
					audioBuffer.clear();
					pkt = audioTrack.nextFrame();
					audioDecoder.decodeFrame(pkt.getData(), audioBuffer);
					audioData.add(AudioUtilities.pcmBytes2float(audioInfo, audioBuffer.array(), audioBuffer.position()));
				}
			}
		 */
	}

	public int getNumChannels() {
		return audioInfo == null ? 2 : audioInfo.getChannels();
	}

	public float getSampleRate() {
		return audioInfo == null ? 48000 : audioInfo.getSampleRate();
	}
}