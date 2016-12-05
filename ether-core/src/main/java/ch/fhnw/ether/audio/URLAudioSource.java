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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.util.ByteList;
import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;


public class URLAudioSource extends AbstractFrameSource implements Runnable, IDisposable, IAudioSource {
	private static final Log LOG = Log.create();

	private static final int           BUFFER_SZ = 128;
	private static final double        SEC2US = 1000000;
	private static final MidiEvent[]   EMPTY_MidiEventA = new MidiEvent[0];

	private       double             frameSizeInSec;
	private       int                frameSizeInBytes;
	private final URL                url;
	private       AudioFormat        fmt;       
	private       long               frameCount;
	private int                      noteOn;
	private AudioInputStream         midiStream;
	private final TreeSet<MidiEvent> notes = new TreeSet<>(new Comparator<MidiEvent>() {
		@Override
		public int compare(MidiEvent o1, MidiEvent o2) {
			int   result  = (int) (o1.getTick() - o2.getTick());
			return result == 0 ? o1.getMessage().getMessage()[1] - o2.getMessage().getMessage()[1] : result;
		}
	});
	private final BlockingQueue<float[]> data = new LinkedBlockingQueue<>();
	private final AtomicInteger          numPlays     = new AtomicInteger();
	private       long                   samples;
	private       Semaphore              bufSemaphore = new Semaphore(512);

	public URLAudioSource(URL url) throws IOException {
		this(url, Integer.MAX_VALUE, -BUFFER_SZ);
	}

	public URLAudioSource(final URL url, final int numPlays) throws IOException {
		this(url, numPlays, -BUFFER_SZ);
	}

	public URLAudioSource(final URL url, final int numPlays, double frameSizeInSec) throws IOException {
		this.url            = url;
		this.numPlays.set(numPlays);
		this.frameSizeInSec = frameSizeInSec;

		try {
			if(TextUtilities.hasFileExtension(url.getPath(), "mid")) {
				send(MidiSystem.getSequence(url), new Receiver() {
					@Override
					public void send(MidiMessage message, long timeStamp) {
						if(message instanceof ShortMessage && (message.getMessage()[0] & 0xFF) == ShortMessage.NOTE_ON && (message.getMessage()[2] > 0))
							noteOn++;
						if(message instanceof ShortMessage && (
								(message.getMessage()[0] & 0xFF) == ShortMessage.NOTE_ON ||
								(message.getMessage()[0] & 0xFF) == ShortMessage.NOTE_OFF))
							notes.add(new MidiEvent(message, timeStamp));
					}

					@Override
					public void close() {}
				});
				
				getStream(url);
			} else {
				try (AudioInputStream in = getStream(url)) {
					this.fmt   = in.getFormat();
					frameCount = in.getFrameLength();

					if(fmt.getSampleSizeInBits() != 16)
						throw new IOException("Only 16 bit audio supported, got " + fmt.getSampleSizeInBits());
					if(fmt.getEncoding() != Encoding.PCM_SIGNED)
						throw new IOException("Only signed PCM audio supported, got " + fmt.getEncoding());
				} catch (UnsupportedAudioFileException e) {
					throw new IOException(e);
				}
			}
		} catch (UnsupportedAudioFileException | InvalidMidiDataException e) {
			throw new IOException(e);
		}
		rewind();
	}

	private AudioInputStream openStream(Synthesizer synth, AudioFormat format, Map<String, Object> props) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return (AudioInputStream) synth.getClass().getMethod("openStream", AudioFormat.class, Map.class).invoke(synth, format, props);
	}

	private Synthesizer findAudioSynthesizer() throws MidiUnavailableException, ClassNotFoundException {
		Class<?> audioSynth = Class.forName("com.sun.media.sound.AudioSynthesizer");

		// First check if default synthesizer is AudioSynthesizer.
		Synthesizer synth = MidiSystem.getSynthesizer();
		if (audioSynth.isAssignableFrom(synth.getClass())) {
			return synth;
		}

		// If default synthesizer is not AudioSynthesizer, check others.
		MidiDevice.Info[] midiDeviceInfo = MidiSystem.getMidiDeviceInfo();
		for (int i = 0; i < midiDeviceInfo.length; i++) {
			MidiDevice dev = MidiSystem.getMidiDevice(midiDeviceInfo[i]);
			if (audioSynth.isAssignableFrom(dev.getClass())) {
				return (Synthesizer)dev;
			}
		}
		return null;
	}

	public AudioInputStream getStream(URL url) throws UnsupportedAudioFileException, IOException {
		if(midiStream != null) return midiStream;

		if(TextUtilities.hasFileExtension(url.getPath(), "mid")) {
			try {
				Sequence seq = MidiSystem.getSequence(url);

				this.fmt = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 
						44100,
						16,
						1,
						2,
						44100,
						false);

				Synthesizer synth = findAudioSynthesizer();
				if (synth == null)
					throw new IOException("No AudioSynthesizer was found!");

				Map<String, Object> p = new HashMap<String, Object>();
				p.put("interpolation", "sinc");
				p.put("max polyphony", "1024");
				midiStream = openStream(synth, fmt, p);

				frameCount = (long)((send(seq, synth.getReceiver()) + 1.0) * fmt.getFrameRate());
				
				return midiStream;
			} catch(IOException e) {
				throw e;
			} catch(Throwable t) {
				throw new IOException(t);
			}
		} else {
			List<AudioFileReader> providers = getAudioFileReaders();
			AudioInputStream result = null;

			for(int i = 0; i < providers.size(); i++) {
				AudioFileReader reader = providers.get(i);
				try {
					result = reader.getAudioInputStream(url);
					break;
				} catch (UnsupportedAudioFileException e) {
					continue;
				} catch(IOException e) {
					continue;
				}
			}

			if( result==null ) {
				throw new UnsupportedAudioFileException("could not get audio input stream from input URL:"+url);
			}

			AudioFormat format = result.getFormat();
			if(format.getEncoding() != Encoding.PCM_SIGNED || format.getSampleSizeInBits() < 0) {
				AudioFormat fmt = new AudioFormat(Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
				return AudioSystem.getAudioInputStream(fmt, result);
			}
			return result;
		}
	}

	public static AudioInputStream getStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
		List<AudioFileReader> providers = getAudioFileReaders();
		AudioInputStream result = null;
		ByteList         bl     = new ByteList();
		bl.readFully(stream);
		byte[] buffer = bl.toArray();

		for(int i = providers.size(); --i >= 0; ) {
			AudioFileReader reader = providers.get(i);
			try {
				result = reader.getAudioInputStream(new ByteArrayInputStream(buffer));
				break;
			} catch (UnsupportedAudioFileException e) {
				continue;
			} catch(IOException e) {
				continue;
			}
		}

		if( result==null ) {
			throw new UnsupportedAudioFileException("could not get audio input stream from input URL");
		}

		AudioFormat format = result.getFormat();
		if(format.getEncoding() != Encoding.PCM_SIGNED || format.getSampleSizeInBits() < 0) {
			AudioFormat fmt = new AudioFormat(Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
			return AudioSystem.getAudioInputStream(fmt, result);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static List<AudioFileReader> getAudioFileReaders() {
		try {
			return (List<AudioFileReader>) ClassUtilities.getMethod(AudioSystem.class, "getAudioFileReaders").invoke(null);
		} catch (Throwable t) {
			LOG.severe(t);
			return Collections.emptyList();
		}
	}

	public URL getURL() {
		return url;
	}

	@Override
	public float getSampleRate() {
		return fmt.getSampleRate();
	}

	@Override
	public void run() {
		try {
			frameSizeInBytes = frameSizeInSec > 0 
					? fmt.getChannels() * fmt.getSampleSizeInBits() / 8 * (int)(frameSizeInSec * fmt.getSampleRate())
							: fmt.getChannels() * fmt.getSampleSizeInBits() / 8 * (int)-frameSizeInSec;
					byte[] buffer = new byte[frameSizeInBytes];

					do {
						long sampleCount = 0;
						try (AudioInputStream in = getStream(url)) {
							for(;;) {
								int read = in.read(buffer);
								if(read < 0) break;
								sampleCount += read / fmt.getFrameSize();
								data.add(AudioUtilities.pcmBytes2float(fmt, buffer, read));
								bufSemaphore.acquire();
								if(midiStream != null && sampleCount > frameCount)  {
									midiStream.close();
									midiStream = null;
									midiStream = getStream(url);
									break;
								}
							}
						}
					} while(numPlays.decrementAndGet() > 0);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	protected void run(IRenderTarget<?> target) throws RenderCommandException {
		try {
			final float[] outData = data.take();
			bufSemaphore.release();
			AudioFrame frame = createAudioFrame(samples, outData);
			frame.setLast(data.isEmpty() && numPlays.get() <= 0);
			((IAudioRenderTarget)target).setFrame(this, frame);
			samples += outData.length;
		} catch(Throwable t) {
			throw new RenderCommandException(t);
		}
	}

	@Override
	public void dispose() {
		try {
			numPlays.set(0);
			while(numPlays.get() >= 0) {
				if(data.poll(10, TimeUnit.MILLISECONDS) != null)
					bufSemaphore.release();
			}
		} catch(Throwable t) {
			LOG.warning(t);
		}
	}

	public void rewind() {
		Thread t = new Thread(this, "AudioReader:" + url.toExternalForm());
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	public MidiEvent[] getMidi(double time, double timeWindow) {
		long timeT   = (long) (time * SEC2US);
		long windowT = (long) (timeWindow * SEC2US);
		SortedSet<MidiEvent> result = notes.subSet(noteOn(0, 0, timeT - windowT / 2), noteOn(0, 0, timeT + windowT / 2));
		return result.isEmpty() ? EMPTY_MidiEventA : result.toArray(new MidiEvent[result.size()]);
	}

	private MidiEvent noteOn(int key, int velocity, long ticks) {
		try {
			return new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON,  0, key, velocity), ticks);
		} catch(InvalidMidiDataException e) {
			return null;
		}
	}

	private double send(Sequence seq, Receiver recv) {
		float divtype = seq.getDivisionType();
		assert (seq.getDivisionType() == Sequence.PPQ);
		Track[] tracks = seq.getTracks();
		int[] trackspos = new int[tracks.length];
		int mpq = (int)SEC2US / 2;
		int seqres = seq.getResolution();
		long lasttick = 0;
		long curtime = 0;
		while (true) {
			MidiEvent selevent = null;
			int seltrack = -1;
			for (int i = 0; i < tracks.length; i++) {
				int trackpos = trackspos[i];
				Track track = tracks[i];
				if (trackpos < track.size()) {
					MidiEvent event = track.get(trackpos);
					if (selevent == null
							|| event.getTick() < selevent.getTick()) {
						selevent = event;
						seltrack = i;
					}
				}
			}
			if (seltrack == -1)
				break;
			trackspos[seltrack]++;
			long tick = selevent.getTick();
			if (divtype == Sequence.PPQ)
				curtime += ((tick - lasttick) * mpq) / seqres;
			else
				curtime = tick;
			lasttick = tick;
			MidiMessage msg = selevent.getMessage();
			if (msg instanceof MetaMessage) {
				if (divtype == Sequence.PPQ)
					if (((MetaMessage) msg).getType() == 0x51) {
						byte[] data = ((MetaMessage) msg).getData();
						mpq = ((data[0] & 0xff) << 16)
								| ((data[1] & 0xff) << 8) | (data[2] & 0xff);
					}
			} else {
				if (recv != null)
					recv.send(msg, curtime);
			}
		}
		return curtime / SEC2US;
	}

	public int getNumNotes() {
		return noteOn;
	}

	@Override
	public String toString() {
		return url.toString();
	}

	@Override
	public long getLengthInFrames() {
		if(frameCount < 0) return FRAMECOUNT_UNKNOWN;
		long result = frameCount;
		result *= fmt.getChannels();
		result *= fmt.getSampleSizeInBits();
		result /= frameSizeInBytes * 8;
		return result;
	}

	@Override
	public int getNumChannels() {
		return fmt.getChannels();
	}

	@Override
	public double getLengthInSeconds() {
		if(frameCount < 0) return LENGTH_UNKNOWN;
		double result = frameCount;
		result /= fmt.getFrameRate();
		return result;
	}

	@Override
	public float getFrameRate() {
		double result = (fmt.getChannels() * fmt.getSampleSizeInBits() * fmt.getFrameRate()) / (8 * frameSizeInBytes);
		return (float)result;
	}

	public void getMidiEvents(Collection<MidiEvent> result) {
		result.clear();
		result.addAll(notes);
	}
}
