package ch.fhnw.ether.midi;

import javax.sound.midi.MidiMessage;

public interface IMidiHandler {
	void handle(MidiMessage msg);
}
