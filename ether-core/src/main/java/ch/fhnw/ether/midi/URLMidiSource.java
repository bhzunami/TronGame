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

package ch.fhnw.ether.midi;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.AbstractMediaTarget;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.RenderCommandException;

public class URLMidiSource extends AbstractFrameSource implements IMidiSource {
	private final Sequence          seq;
	private final URL               url;
	private final long              frameCount;
	private final float             divtype;
	private final Track[]           tracks;
	private final int               seqres;
	private final int[]             trackspos;
	private       int               mpq        = (int)AbstractMediaTarget.SEC2US / 2;
	private       long              lasttick   = -1;
	private       long              curtime    = 0;
	private       int               numPlays;
	private       double            startTime  = -1;
	private final List<MidiMessage> msgs = new ArrayList<>();
	private final double            length;
	int                             count;

	public URLMidiSource(URL url) throws InvalidMidiDataException, IOException {
		this(url, Integer.MAX_VALUE);
	}

	public URLMidiSource(final URL url, final int numPlays) throws IOException, InvalidMidiDataException {
		this.url       = url;
		this.seq       = MidiSystem.getSequence(url);
		this.numPlays  = numPlays;
		this.divtype   = seq.getDivisionType();
		this.tracks    = seq.getTracks();
		this.seqres    = seq.getResolution();
		this.trackspos = new int[tracks.length];
		this.numPlays  = URLMidiSource.this.numPlays;
		if(seq.getDivisionType() != Sequence.PPQ)
			throw new IOException("Only PPQ Sequence supported");

		Track[] tracks    = seq.getTracks();
		long    frameCount  = 0;
		int[]   trackspos = new int[tracks.length];
		long    lasttick   = -1;
		int     msgs       = 0;
		curtime            = 0;
		
		frameLoop:
			for(;;) {
				for(;;) {
					MidiEvent selevent = null;
					int       seltrack = -1;
					for (int i = 0; i < tracks.length; i++) {
						int trackpos = trackspos[i];
						Track track = tracks[i];
						if (trackpos < track.size()) {
							MidiEvent event = track.get(trackpos);
							if (selevent == null || event.getTick() < selevent.getTick()) {
								selevent = event;
								seltrack = i;
							}
						}
					}
					if (seltrack == -1)
						break frameLoop;

					trackspos[seltrack]++;
					long tick = selevent.getTick();
					
					if(lasttick < 0)
						lasttick = tick;
					if (divtype == Sequence.PPQ)
						curtime += ((tick - lasttick) * mpq) / seqres;
					else
						curtime = tick;
					boolean setFrame = lasttick != tick;
					lasttick = tick;
					
					MidiMessage msg = selevent.getMessage();
					if(!(msg instanceof MetaMessage))
						msgs++;
					if(setFrame && msgs > 0)
						break;
				}
				frameCount++;
				msgs = 0;
			}

		this.frameCount = frameCount;
		this.length     = curtime;
		this.curtime    = 0;
	}

	@Override
	protected void run(IRenderTarget<?> target) throws RenderCommandException {
		if(numPlays <= 0) return;

		if(startTime < 0)
			startTime = target.getTime();

		for(;;) {
			MidiEvent selevent = null;
			int       seltrack = -1;
			for (int i = 0; i < tracks.length; i++) {
				int trackpos = trackspos[i];
				Track track = tracks[i];
				if (trackpos < track.size()) {
					MidiEvent event = track.get(trackpos);
					if (selevent == null || event.getTick() < selevent.getTick()) {
						selevent = event;
						seltrack = i;
					}
				}
			}
			if (seltrack == -1) {
				Arrays.fill(trackspos, 0);
				numPlays--;
				startTime = -1;
				count = 0;
				break;
			}
			trackspos[seltrack]++;
			long tick = selevent.getTick();
			if(lasttick < 0)
				lasttick = tick;
			if (divtype == Sequence.PPQ)
				curtime += ((tick - lasttick) * mpq) / seqres;
			else
				curtime = tick;
			boolean setFrame = lasttick != tick;
			lasttick = tick;
			MidiMessage msg = selevent.getMessage();
			if (msg instanceof MetaMessage) {
				if (divtype == Sequence.PPQ)
					if (((MetaMessage) msg).getType() == 0x51) {
						byte[] data = ((MetaMessage) msg).getData();
						mpq = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
					}
			} else
				msgs.add(msg);
			if(setFrame && !msgs.isEmpty())
				break;
		}
		MidiFrame frame = new MidiFrame(startTime + (curtime / AbstractMediaTarget.SEC2US), msgs.toArray(new MidiMessage[msgs.size()]));
		((IMidiRenderTarget)target).setFrame(this, frame);
		frame.setLast(numPlays <= 0);
		msgs.clear();
	}

	@Override
	public String toString() {
		return url.toString();
	}

	@Override
	public long getLengthInFrames() {
		return frameCount;
	}
	
	@Override
	public double getLengthInSeconds() {
		return length;
	}
	
	@Override
	public float getFrameRate() {
		double result = getLengthInFrames() / getLengthInSeconds();
		return (float)result;
	}
}
