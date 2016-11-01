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

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

import ch.fhnw.ether.media.AbstractMediaTarget;
import ch.fhnw.ether.media.RenderCommandException;

public class JavaMidiSynthesizerTarget extends AbstractMediaTarget<MidiFrame,IMidiRenderTarget> implements IMidiRenderTarget {
	protected final Synthesizer synth;

	protected JavaMidiSynthesizerTarget(Synthesizer synth, int threadPriority, boolean realTime) throws MidiUnavailableException {
		super(threadPriority, realTime);
		this.synth = synth;
		synth.open();
	}

	public JavaMidiSynthesizerTarget() throws MidiUnavailableException {
		this(MidiSystem.getSynthesizer(), Thread.MAX_PRIORITY, true);
	}

	@Override
	public void render() throws RenderCommandException {
		sleepUntil(getFrame().playOutTime);

		for(MidiMessage msg : getFrame().messages) {
			if(msg instanceof ShortMessage) {
				final ShortMessage sm = (ShortMessage)msg;
				final MidiChannel  ch = synth.getChannels()[sm.getChannel()];
				switch(sm.getCommand()) {
				case ShortMessage.NOTE_ON:
					ch.noteOn(sm.getData1(), sm.getData2());
					continue;
				case ShortMessage.NOTE_OFF:
					ch.noteOff(sm.getData1(), sm.getData2());
					continue;
				case ShortMessage.PROGRAM_CHANGE:
					ch.programChange(sm.getData1());
					continue;
				case ShortMessage.CONTROL_CHANGE:
					ch.controlChange(sm.getData1(), sm.getData2());
					continue;
				}
			}
			throw new RenderCommandException("Unknown MIDI Command:" + MidiToString.toString(msg));
		}
	}
}
