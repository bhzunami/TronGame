package org.corebounce.io;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;

import org.corebounce.audio.Audio;
import org.corebounce.soundium.Subsystem;
import org.corebounce.video.Monitors;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.BandsButterworth;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.platform.IMonitor;
import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.net.osc.IOSCHandler;
import ch.fhnw.util.net.osc.OSCServer;

public class OSC extends Subsystem {
	private static final Log log = Log.create();

	private final OSCServer server;
	private       String[]  slots  = ClassUtilities.EMPTY_StringA;
	private       float[]   power  = ClassUtilities.EMPTY_floatA;
	private       Object[]  powera = ClassUtilities.EMPTY_ObjectA;
	private final Monitors  mons;

	public OSC(String[] args, Audio audio, Monitors mons) throws NumberFormatException, IOException {
		super(CFG_PREFIX, args);
		this.mons = mons;
		int port = 55556;
		try {
			port = Integer.parseInt(configuration.get("server"));
		} catch(Throwable t) {}
		server = new OSCServer(port);
		for(int i = -1; ; i++) {
			try {
				String peerKey = i < 0 ? "p" : "p"+i;
				String[] peer = TextUtilities.split(configuration.get(peerKey), ':');
				server.addPeer(peerKey, new InetSocketAddress(peer[0], Integer.parseInt(peer[1])));
			} catch(Throwable t) {
				if(i > 0) break;
			}
		}
		if(server.getPeers().isEmpty())
			server.addPeer("default", new InetSocketAddress(InetAddress.getLocalHost(), port-1));
		audio.addLast(new AbstractRenderCommand<IAudioRenderTarget>() {
			int lastBeatCount;
			int lastBeatCountPLL;

			@Override
			protected void run(IAudioRenderTarget target) throws RenderCommandException {
				BandsButterworth bands = audio.getBands();
				if(power.length != bands.numBands()) {
					power  = new float[bands.numBands()];
					powera = new Object[bands.numBands()];
				}
				bands.power(power);
				for(int i = 0; i < power.length; i++)
					powera[i] = Float.valueOf(power[i]);
				send("/audio/bands", powera);
				int counter = audio.getBeat().beatCount();
				if(counter != lastBeatCount) {
					send("/audio/beatCount", counter);
					lastBeatCount = counter;
				}
				counter = audio.getBeat().beatCountPLL();
				if(counter != lastBeatCountPLL) {
					send("/audio/beatCountPLL", counter);
					send("/audio/bpm", (float)audio.getBeat().bpm());
					lastBeatCountPLL = counter;
				}
			}
		});

		Thread t = new Thread(()->{
			for(;;) {
				try {
					queryState();
					Thread.sleep(1000);
				} catch(Throwable e) {
					log.warning(e);
				}
			}
		}, "OSC Ping");
		t.setPriority(Thread.MIN_PRIORITY);
		t.setDaemon(true);
		t.start();		
	}

	public synchronized void queryState() {
		try {
			Object[] monitors  = new Object[mons.getEngineMonitors().length];
			int i = 0;
			for(IMonitor mon : mons.getEngineMonitors())
				monitors[i++] = mon.getIndex();
			send("/state", monitors);
		} catch(Throwable e) {
			log.warning(e);
		}
	}

	public void send(String oscAddr, Collection<?> values) {
		server.send(oscAddr, values);
	}

	public void send(String oscAddr, Object ... values) {
		server.send(oscAddr, values);
	}

	public synchronized String[] getSlots() {
		return slots;
	}

	public long lastMessageTime() {
		return server.getLastMessageTime();
	}

	public void addHandler(String address, IOSCHandler handler) {
		server.addHandler(address, handler);
	}

	public void start() {
		server.start();
	}

	public static String   CFG_PREFIX = "osc";
	public static String[] CFG_OPTIONS = {
			"server=<port>", "UDP port of OSC server",
			"p<n>=<ip>:<port>", "IP address/port to which OSC messages will be sent (peer)",
	};
}
