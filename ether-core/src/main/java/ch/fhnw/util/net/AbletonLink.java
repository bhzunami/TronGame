package ch.fhnw.util.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.util.CollectionUtilities;
import ch.fhnw.util.IProgressListener;
import ch.fhnw.util.Log;

public class AbletonLink {
	private static final Log log = Log.create();

	private final MulticastSocket           socket;
	private final InetAddress               addr;
	private       AtomicBoolean             joined = new AtomicBoolean();
	private       List<IAbletonLinkHandler> handlers = new ArrayList<>();

	public AbletonLink() throws IOException {
		socket = new MulticastSocket(20808);
		addr   = NetworkUtilities.multicastAddress('L','N','K');

		final Thread receiveThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (;;) {
					try {
						if(joined.get()) {
							byte[] buffer = new byte[socket.getReceiveBufferSize()];
							DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
							socket.receive(packet);
							process(new AbletonLinkPacket(System.nanoTime() / 1000, packet));
						} else {
							Thread.sleep(100);
						}
					} catch (Throwable t) {
						log.warning(t);
					}
				}
			}

		}, "Ableton Link");
		receiveThread.setDaemon(true);
		receiveThread.setPriority(Thread.MAX_PRIORITY);
		receiveThread.start();		
	}

	private void process(AbletonLinkPacket linkPacket) {
		IAbletonLinkHandler[] handlers = null; 
		synchronized (this) {
			handlers = this.handlers.toArray(new IAbletonLinkHandler[this.handlers.size()]);
		}
		for(IAbletonLinkHandler handler : handlers)
			handler.handle(linkPacket);
	}

	public synchronized void addHandler(IAbletonLinkHandler handler) {
		handlers.add(handler);
	}

	public synchronized void removeHandler(IAbletonLinkHandler handler) {
		CollectionUtilities.removeAll(handlers, handler);
	}

	public void join(boolean leafOnShutdown, IProgressListener progress) throws UnknownHostException, IOException {
		if(!(joined.get())) {
			List<InetAddress> ifaddrs = NetworkUtilities.getLocalAddresses(true);
			float idx = 0;
			for(InetAddress ifaddr : ifaddrs) {
				idx += 1;
				if(progress != null) progress.setProgress(idx / ifaddrs.size());
				try {
					socket.setInterface(ifaddr);
					socket.setReuseAddress(true);
					socket.setLoopbackMode(true);
					socket.joinGroup(addr);
					log.info("Ableton Link group joined " + ifaddr.getHostName() + ":" + socket.getLocalPort() + " " + addr);
				} catch(Throwable t) {}
			}
			joined.set(true);
		}

		if(leafOnShutdown) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {leave();} catch (IOException e) {log.warning(e);}
				}
			});	
		}
		
		if(progress != null) progress.done();
	}

	public void leave() throws IOException {
		if(joined.getAndSet(false)) {
			for(InetAddress ifaddr : NetworkUtilities.getLocalAddresses(false)) {
				try {
					socket.setInterface(ifaddr);
					socket.leaveGroup(addr);
				} catch(Throwable t) {}
			}
			log.info("Ableton Link group left");
		}
	}

	public boolean isJoined() {
		return joined.get();
	}

	public static void main(String[] args) throws IOException {
		Platform.get().init();

		AbletonLink link = new AbletonLink();
		link.join(true, null);
		link.addHandler(packet->{
			System.out.println(packet);
		});
		
		Platform.get().run();
	}
}
