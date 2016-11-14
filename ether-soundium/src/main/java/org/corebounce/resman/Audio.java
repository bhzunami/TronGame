package org.corebounce.resman;

import java.io.IOException;
import java.net.URL;

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
import ch.fhnw.util.ArrayUtilities;
import ch.fhnw.util.IProgressListener;
import ch.fhnw.util.Log;
import ch.fhnw.util.net.AbletonLink;
import ch.fhnw.util.net.AbletonLinkPacket;
import ch.fhnw.util.net.AbletonLinkPacket.Beats;
import ch.fhnw.util.net.AbletonLinkPacket.Payload;
import ch.fhnw.util.net.AbletonLinkPacket.Timeline;
import ch.fhnw.util.net.IAbletonLinkHandler;

public class Audio extends Subsystem implements IAbletonLinkHandler {
	private static final Log log = Log.create();

	private final IAudioSource     src;
	private final DCRemove         dcrmv = new DCRemove();
	private final AutoGain         gain  = new AutoGain();
	private final BandsButterworth bands = new BandsButterworth(60, 8000, 40, 5, 1);
	private final OnsetDetect      onset = new OnsetDetect(); 
	private final BeatDetect       beatDetect  = new BeatDetect(onset);
	private final MonitorGain      out   = new MonitorGain();
	private final JavaSoundTarget  dst   = new JavaSoundTarget();
	private final RenderProgram<IAudioRenderTarget> audio;
	private final AbletonLink      link  = new AbletonLink();

	public Audio(IProgressListener progress, String ... args) throws RenderCommandException, IOException {
		super(CFG_PREFIX, args);

		URL url = null;
		try {
			url = new URL(configuration.get("in"));
		} catch(Throwable t) {}

		gain.setVal(AutoGain.ATTACK, 0.15f);
		try {
			out.setVal(MonitorGain.GAIN, Float.parseFloat(configuration.get("mon")));
		} catch(Throwable t) {}

		src = url == null ? new JavaSoundSource(2, 44100, 1024) : new URLAudioSource(url);

		audio = new RenderProgram<>(src, dcrmv, gain, bands, onset, beatDetect, out);

		dst.useProgram(audio);
		dst.start();

		if(!("off".equals(configuration.get("link")))) {
			link.addHandler(this);
			link.join(true, progress);
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
				beatDetect.setVal(BeatDetect.BPM, (float)(60 * IScheduler.SEC2US) / (float)t.tempo.microsPerBeat);
			}
		}
	}
}
