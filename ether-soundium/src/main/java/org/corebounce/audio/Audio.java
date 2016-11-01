package org.corebounce.audio;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.corebounce.io.MIDI;
import org.corebounce.soundium.Subsystem;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.IAudioSource;
import ch.fhnw.ether.audio.JavaSoundSource;
import ch.fhnw.ether.audio.JavaSoundTarget;
import ch.fhnw.ether.audio.URLAudioSource;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.BandsButterworth;
import ch.fhnw.ether.audio.fx.BeatDetect;
import ch.fhnw.ether.audio.fx.BeatDetect.BeatType;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.OnsetDetect;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.midi.AbletonPush;
import ch.fhnw.ether.midi.AbletonPush.PControl;
import ch.fhnw.util.ArrayUtilities;
import ch.fhnw.util.CollectionUtilities;
import ch.fhnw.util.IProgressListener;
import ch.fhnw.util.Log;
import ch.fhnw.util.net.link.AbletonLink;
import ch.fhnw.util.net.link.AbletonLinkPacket;
import ch.fhnw.util.net.link.AbletonLinkPacket.Beats;
import ch.fhnw.util.net.link.AbletonLinkPacket.Payload;
import ch.fhnw.util.net.link.AbletonLinkPacket.Timeline;
import ch.fhnw.util.net.link.IAbletonLinkHandler;

public class Audio extends Subsystem implements IAbletonLinkHandler {
	private static final Log log = Log.create();

	private final IAudioSource        src;
	private final DCRemove            dcrmv = new DCRemove();
	private final AutoGain            gain  = new AutoGain();
	private final BandsButterworth    bands = new BandsButterworth(60, 8000, 40, 5, 1);
	private final OnsetDetect         onset = new OnsetDetect(); 
	private final BeatDetect          beatDetect  = new BeatDetect(onset);
	private final MonitorGain         out   = new MonitorGain();
	private final JavaSoundTarget     dst   = new JavaSoundTarget();
	private final RenderProgram<IAudioRenderTarget> audio;
	private final AbletonLink         link  = new AbletonLink();
	private final List<IBeatListener> s_listeners = new ArrayList<>();

	public Audio(IProgressListener progress, MIDI midi, String ... args) throws RenderCommandException, IOException {
		super(CFG_PREFIX, args);

		URL url = null;
		try {
			File f = new File(configuration.get("in"));
			if(f.isFile() && f.exists())
				url = f.toURI().toURL();
		} catch(Throwable t) {}
		if(url == null) {
			try {
				url = new URL(configuration.get("in"));
			} catch(Throwable t) {}
		}

		gain.setVal(AutoGain.ATTACK,  0.05f);
		gain.setVal(AutoGain.SUSTAIN, 9f);
		gain.setVal(AutoGain.DECAY,   0.01f);
		gain.setVal(AutoGain.TARGET,  0f);

		try {
			out.setVal(MonitorGain.GAIN, Float.parseFloat(configuration.get("mon")));
		} catch(Throwable t) {}

		src = url == null ? new JavaSoundSource(2, 44100, 1024) : new URLAudioSource(url);
		audio = new RenderProgram<>(src, dcrmv, gain, bands, onset, beatDetect, out);
		dst.useProgram(audio);
		dst.start();

		log.info("Audio source:" + src);
		log.info("Audio Monitor: " + dst);

		if(!("off".equals(configuration.get("link")))) {
			link.addHandler(this);
			link.join(true, progress);
		}

		if(midi.getPush() != null) {
			try {
				AbletonPush push = midi.getPush();
				push.set(PControl.METRONOME, beatDetect, BeatDetect.METRONOME);
				push.set(PControl.MONITOR,   beatDetect, BeatDetect.RATIO);
				push.set(PControl.TEMPO,     beatDetect, BeatDetect.BPM);
				push.set(PControl.TAP,       this::tap);
			} catch(Throwable t) {
				log.warning(t);
			}
		}

		log.info("Audio started.");
	}

	public BandsButterworth getBands() {
		return bands;
	}

	public OnsetDetect getOnset() {
		return onset;
	}

	public MonitorGain getOutGain() {
		return out;
	}

	public void addLast(AbstractRenderCommand<IAudioRenderTarget> cmd) {
		audio.addLast(cmd);
	}

	public static String   CFG_PREFIX = "au";
	public static String[] CFG_OPTIONS = {
			"param", "Display parameter window",
			"in=<url>","URL of audio file",
			"link=off", "Disable Ableton Link",
			"mon=<gain>","Initial gain of audio monitor",
	};

	static {
		int i = 0;
		for(String source : JavaSoundSource.getSources())
			CFG_OPTIONS = ArrayUtilities.cat(CFG_OPTIONS, new String[] {"in="+i++, source});
	}

	public BeatDetect getBeat() {
		return beatDetect;
	}

	@Override
	public void handle(AbletonLinkPacket linkPacket) {
		for(Payload p : linkPacket.payload) {
			if(p instanceof Timeline) {
				final Timeline t       = (Timeline)p;
				final double   beat    = t.beatOrigin.floating();
				double frameTime       = beatDetect.frameTime();
				long   nowMicros       = System.nanoTime() / 1000;
				double originFrameTime = frameTime - ((nowMicros - linkPacket.timestampMicros) / IScheduler.SEC2US);
				double beatNo          = Math.floor(beat);
				beatDetect.setBeat((int)beatNo+1, BeatType.EXTERNAL,  originFrameTime + (t.fromBeats(new Beats(beatNo+1)) - t.timeOrigin) / IScheduler.SEC2US);
				beatDetect.setBeat((int)beatNo+2, BeatType.ESTIMATED, originFrameTime + (t.fromBeats(new Beats(beatNo+2)) - t.timeOrigin) / IScheduler.SEC2US);
				beatDetect.setVal(BeatDetect.BPM, (float)(60 * IScheduler.SEC2US) / t.tempo.microsPerBeat);
			}
		}
	}

	long lastTap;
	public void tap() {
		long now = System.currentTimeMillis();

		long tapTime    = now - lastTap;
		BeatDetect beat = getBeat();
		beat.setBeat(beat.beatCountPLL()+1, BeatType.TAP, beat.frameTime());
		if(tapTime < 1200) {
			beat.setBeat(beat.beatCountPLL()+2, BeatType.ESTIMATED, beat.frameTime() + tapTime / IScheduler.SEC2MS);
			beat.setVal(BeatDetect.BPM, (float)(60.0 / (tapTime / IScheduler.SEC2MS)));
		} else
			beat.setBeat(beat.beatCountPLL()+2, BeatType.ESTIMATED, beat.frameTime() + 60.0 / beat.getVal(BeatDetect.BPM));

		lastTap = now;
	}

	public void addBeatListener(IBeatListener listener) {
		synchronized (s_listeners) {
			s_listeners.add(listener);
		}
	}

	public void removeBeatListener(IBeatListener listener) {
		synchronized (s_listeners) {
			CollectionUtilities.removeAll(s_listeners, listener);
		}
	}

	void fireBeat(int beatCount) {
		IBeatListener[] blisteners = null;
		synchronized (s_listeners) {
			blisteners = s_listeners.toArray(new IBeatListener[s_listeners.size()]);
		}
		for(IBeatListener l : blisteners) {
			try {
				l.beat(beatCount);
			} catch(Throwable t) {
				log.warning(t);
			}
		}
	}

	public boolean isPaused() {
		return !(dst.isRendering());
	}

	public void setPaused(boolean state) {
		try {
			if(state) {
				if(dst.isRendering())
					dst.stop();
			} else {
				if(!(dst.isRendering()))
					dst.start();
			}
		} catch(Throwable t) {
			log.warning(t);
		}
	}	
}
