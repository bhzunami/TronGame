package org.corebounce.io;

import org.corebounce.soundium.Soundium;
import org.corebounce.soundium.Subsystem;

import ch.fhnw.ether.midi.AbletonPush;
import ch.fhnw.ether.midi.AbletonPush.TouchStrip;
import ch.fhnw.util.IProgressListener;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;

public class MIDI extends Subsystem implements IProgressListener {
	private static final Log log = Log.create();

	private AbletonPush push;
	private float       progress;

	@SuppressWarnings("unused")
	public MIDI(String[] args) {
		super(CFG_PREFIX, args);

		try {
			push = new AbletonPush(0);

			push.setTouchStrip(TouchStrip.Host_Bar_Bottom);
			setProgress(0);

			if(true) {
				push.setLine(0, Soundium.VERSION);
				push.setLine(1, "(c) 2000-2016    corebounce.org   scheinwerfer.li");
				push.clearLine(2);
				push.setLine(3, "Pascal Mueller   Stefan Arisona   Simon Schubiger  Matthias Specht");
			} else {
				StringBuilder b = new StringBuilder(TextUtilities.repeat(' ', 64));
				for(int i = 0; i < 64; i++) b.setCharAt(i, (char)i);
				push.setLine(0, b.toString());
				for(int i = 0; i < 64; i++) b.setCharAt(i, (char)(i+64));
				push.setLine(1, b.toString());
			}
			
		} catch(Throwable t) {
			log.warning(t.getMessage());
		}
	}

	public static String   CFG_PREFIX  = "midi";
	public static String[] CFG_OPTIONS = {};

	public AbletonPush getPush() {
		return push;
	}

	@Override
	public void setProgress(float progress) {
		this.progress = progress;
		try {
			push.setTouchStrip(progress);
		} catch (Throwable t) {
			log.warning(t.getMessage());
		}
	}

	@Override
	public void done() {}

	@Override
	public float getProgress() {
		return progress;
	}
}
