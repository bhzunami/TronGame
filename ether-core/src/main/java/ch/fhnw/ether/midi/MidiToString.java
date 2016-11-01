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

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import ch.fhnw.util.TextUtilities;

public class MidiToString {
	private static final String[] KEY_NAMES = { "C", "C#", "D", "D#",
		"E", "F", "F#", "G", "G#", "A", "A#", "B" };
	
	private static final String[] KEY_SIGNATURES = { "Cb", "Gb", "Db",
		"Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#", "C#" };
	private static final String[] SYSTEM_MESSAGES = {
		"System Exclusive (should not be in ShortMessage!)",
		"MTC Quarter Frame: ", "Song Position: ", "Song Select: ",
		"Undefined", "Undefined", "Tune Request",
		"End of SysEx (should not be in ShortMessage!)", "Timing clock",
		"Undefined", "Start", "Continue", "Stop", "Undefined",
		"Active Sensing", "System Reset" };

	private static final String[] QUARTER_FRAME_MESSAGES = {
		"frame count LS: ", "frame count MS: ", "seconds count LS: ",
		"seconds count MS: ", "minutes count LS: ", "minutes count MS: ",
		"hours count LS: ", "hours count MS: " };

	private static final String[] FRAME_TYPE = { "24 frames/second",
		"25 frames/second", "30 frames/second (drop)",
		"30 frames/second (non-drop)", };
	
	public static String toString(MidiMessage message) {
		String result = "unknown message type";
		if (message instanceof ShortMessage) 
			result = decodeMessage((ShortMessage) message);
		else if (message instanceof SysexMessage) 
			result = decodeMessage((SysexMessage) message);
		else if (message instanceof MetaMessage) 
			result = decodeMessage((MetaMessage) message);
		return result;
	}

	private static String decodeMessage(ShortMessage message) {
		String result = null;

		switch (message.getCommand()) {
		case ShortMessage.NOTE_OFF:
			result = "note Off " + keyName(message.getData1()) + " velocity: " + message.getData2();
			break;
		case ShortMessage.NOTE_ON:
			result = "note On " + keyName(message.getData1()) + " velocity: " + message.getData2();
			break;
		case ShortMessage.POLY_PRESSURE:
			result = "polyphonic key pressure " + keyName(message.getData1()) + " pressure: " + message.getData2();
			break;
		case ShortMessage.CONTROL_CHANGE:
			if(message.getData1() == 1) result = "Using channel " + (message.getData2() >> 3);
			else                        result = "control change " + message.getData1() + " value: " + message.getData2();
			break;
		case ShortMessage.PROGRAM_CHANGE:
			result = "program change " + message.getData1();
			break;
		case 0xd0:
			result = "key pressure " + keyName(message.getData1()) + " pressure: " + message.getData2();
			break;
		case ShortMessage.PITCH_BEND:
			result = "pitch wheel change " + MidiIO.toInt14(message.getData1(), message.getData2());
			break;
		case 0xF0:
			result = SYSTEM_MESSAGES[message.getChannel()];
			switch (message.getChannel()) {
			case 0x1:
				int nQType = (message.getData1() & 0x70) >> 4;
				int nQData = message.getData1() & 0x0F;
				if (nQType == 7)
					nQData = nQData & 0x1;
				result += QUARTER_FRAME_MESSAGES[nQType] + nQData;
				if (nQType == 7) {
					int nFrameType = (message.getData1() & 0x06) >> 1;
					result += ", frame type: " + FRAME_TYPE[nFrameType];
				}
				break;
			case 0x2:
				result += MidiIO.toInt14(message.getData1(), message.getData2());
				break;
			case 0x3:
				result += message.getData1();
				break;
			}
			break;
		default:
			result = "unknown message: status = " + message.getStatus() + ", byte1 = " + message.getData1() + ", byte2 = " + message.getData2();
			break;
		}
		if (message.getCommand() != 0xF0)
			result = "channel " + (message.getChannel() + 1) + ": " + result;
		return "["+toHexString(message)+"] "+ result;
	}

	private static String decodeMessage(SysexMessage message) {
		byte[] abData     = message.getData();
		String result = null;
		if (message.getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) 
			result = "Sysex message: F0" + TextUtilities.toHex(abData);
		 else if (message.getStatus() == SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE)
			result = "Continued Sysex message F7" + TextUtilities.toHex(abData);
		return result;
	}

	private static  String decodeMessage(MetaMessage message) {
		byte[] abData = message.getData();
		String result = null;
		switch (message.getType()) {
		case 0:
			int nSequenceNumber = ((abData[0] & 0xFF) << 8) | (abData[1] & 0xFF);
			result = "Sequence Number: " + nSequenceNumber;
			break;
		case 1:
			String strText = new String(abData);
			result = "Text Event: " + strText;
			break;
		case 2:
			String strCopyrightText = new String(abData);
			result = "Copyright Notice: " + strCopyrightText;
			break;
		case 3:
			String strTrackName = new String(abData);
			result = "Sequence/Track Name: " + strTrackName;
			break;
		case 4:
			String strInstrumentName = new String(abData);
			result = "Instrument Name: " + strInstrumentName;
			break;
		case 5:
			String strLyrics = new String(abData);
			result = "Lyric: " + strLyrics;
			break;
		case 6:
			String strMarkerText = new String(abData);
			result = "Marker: " + strMarkerText;
			break;
		case 7:
			String strCuePointText = new String(abData);
			result = "Cue Point: " + strCuePointText;
			break;
		case 0x20:
			int nChannelPrefix = abData[0] & 0xFF;
			result = "MIDI Channel Prefix: " + nChannelPrefix;
			break;
		case 0x2F:
			result = "End of Track";
			break;
		case 0x51:
			int nTempo = ((abData[0] & 0xFF) << 16) | ((abData[1] & 0xFF) << 8) | (abData[2] & 0xFF); // tempo in microseconds per beat
			float bpm = convertTempo(nTempo);
			bpm = Math.round(bpm * 100.0f) / 100.0f;
			result = "Set Tempo: "+ bpm +" bpm";
			break;
		case 0x54:
			result = "SMTPE Offset: " + (abData[0] & 0xFF) + ":" + (abData[1] & 0xFF) + ":" + (abData[2] & 0xFF) + "." + (abData[3] & 0xFF) + "." + (abData[4] & 0xFF);
			break;
		case 0x58:
			 result = "Time Signature: " + (abData[0] & 0xFF) + "/" + (1  << (abData[1] & 0xFF)) + ", MIDI clocks per metronome tick: " +
			  (abData[2] & 0xFF) + ", 1/32 per 24 MIDI clocks: " + (abData[3] & 0xFF);
			break;
		case 0x59:
			result = "Key Signature: " + KEY_SIGNATURES[abData[0] + 7] + " " + ((abData[1] == 1) ? "minor" : "major");
			break;
		case 0x7F:
			String strDataDump = TextUtilities.toHex(abData);
			result = "Sequencer-Specific Meta event: " + strDataDump;
			break;
		default:
			String strUnknownDump = TextUtilities.toHex(abData);
			result = "unknown Meta event: " + strUnknownDump;
			break;
		}
		return result;
	}

	private static String toHexString(ShortMessage sm) {
		int status = sm.getStatus();
		String res = TextUtilities.byteToHex(sm.getStatus());
		switch (status) {
		case 0xF6: // Tune Request
		case 0xF7: // EOX
			// System real-time messages
		case 0xF8: // Timing Clock
		case 0xF9: // Undefined
		case 0xFA: // Start
		case 0xFB: // Continue
		case 0xFC: // Stop
		case 0xFD: // Undefined
		case 0xFE: // Active Sensing
		case 0xFF:
			return res;
		}
		res += ' ' + TextUtilities.byteToHex(sm.getData1());
		// if 2-byte message, return
		switch (status) {
		case 0xF1: // MTC Quarter Frame
		case 0xF3: // Song Select
			return res;
		}
		switch (sm.getCommand()) {
		case 0xC0:
		case 0xD0:
			return res;
		}
		// 3-byte messages left
		res += ' ' + TextUtilities.byteToHex(sm.getData2());
		return res;
	}

	private static float convertTempo(float value) {
		if (value <= 0) {
			value = 0.1f;
		}
		return 60000000.0f / value;
	}

	public static String keyName(int nKeyNumber) {
		if (nKeyNumber > 127) { return "illegal value"; }
		int nNote =
				nKeyNumber % 12; int nOctave = nKeyNumber / 12; return
						KEY_NAMES[nNote] + (nOctave - 1);
	}
}
