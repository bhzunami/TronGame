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

package ch.fhnw.util.net.osc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.net.NetworkUtilities;

public final class OSCServer extends OSCDispatcher implements OSCSender {
	private static final Log log = Log.create();

	private static final int RECEIVE_BUFFER_SIZE = 1024 * 1024;
	private static final int SEND_BUFFER_SIZE = 1024 * 1024;

	private final InetSocketAddress address;
	private final DatagramSocket socket;

	private final BlockingQueue<DatagramPacket> txQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<DatagramPacket> rxQueue = new LinkedBlockingQueue<>();

	private final Map<String, SocketAddress> remotePeers = new ConcurrentHashMap<>();

	private final Thread rxThread;
	private final Thread processThread;
	private final Thread txThread;
	
	public OSCServer(int port) throws IOException {
		this(port, null);
	}

	public OSCServer(int port, String multicastAddress) throws IOException {
		address = new InetSocketAddress(NetworkUtilities.getDefaultInterface(), port);
		if (multicastAddress == null) {
			socket = new DatagramSocket(address.getPort());
		} else {
			MulticastSocket multicastSocket = new MulticastSocket(address.getPort());
			multicastSocket.joinGroup(InetAddress.getByName(multicastAddress));
			socket = multicastSocket;
		}
		int dec = socket.getReceiveBufferSize();
		for (int size = RECEIVE_BUFFER_SIZE; socket.getReceiveBufferSize() < size; size -= dec) {
			socket.setReceiveBufferSize(size);
		}
		dec = socket.getSendBufferSize();
		for (int size = SEND_BUFFER_SIZE; socket.getSendBufferSize() < size; size -= dec) {
			socket.setSendBufferSize(size);
		}

		StringBuilder ifaddrs = new StringBuilder();
		for(InetAddress ifaddr : NetworkUtilities.getLocalAddresses(true))
			ifaddrs.append(ifaddr.getHostName()).append(',');
		ifaddrs.setLength(ifaddrs.length()-1);
		
		rxThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					log.info(Thread.currentThread().getName() + " started (" + ifaddrs + ":" + socket.getLocalPort() + ")");
					for (;;) {
						try {
							byte[] buffer = new byte[socket.getReceiveBufferSize()];
							DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
							socket.receive(packet);
							try {
								rxQueue.put(packet);
							} catch (Exception e) {
								OSCCommon.handleException(e, this);							
							}
						} catch (Throwable t) {
							log.warning(t);
						}
					}
				} catch (Throwable e) {
					log.warning(e);
				}
			}
		}, "OSC receiver");
		rxThread.setDaemon(true);
		rxThread.setPriority(Thread.MAX_PRIORITY);

		txThread = new Thread(new Runnable() {
			@Override
			public void run() {
				log.info(Thread.currentThread().getName() + " started (" + ifaddrs + ")");
				// every 10 packets wait 10ms to avoid UDP packet drops.
				int count = 10;
				int i = count;
				for (;;) {
					try {
						DatagramPacket packet = txQueue.take();
						socket.send(packet);
						if (i-- <= 0) {
							i = count;
							Thread.sleep(10);
						}
					} catch (Throwable t) {
						log.warning(t);
					}
				}
			}
		}, "OSC sender");
		txThread.setDaemon(true);

		processThread = new Thread(new Runnable() {
			@Override
			public void run() {
				log.info(Thread.currentThread().getName() + " started)");
				for (;;) {
					try {
						DatagramPacket packet = rxQueue.take();
						process(packet.getSocketAddress(), ByteBuffer.wrap(packet.getData(), 0, packet.getLength()), OSCCommon.TIMETAG_IMMEDIATE, OSCServer.this);
					} catch (Throwable t) {
						log.warning(t);
					}
				}
			}
		}, "OSC processor");
		processThread.setDaemon(true);
		
		OSCCommon.setExceptionHandler(new ExceptionHandler() {
			@Override
			public void exception(Throwable t, Object source) {
				log.warning(source == null ? "OSC Exception (without source)" : "OSC Exception: " + source.toString(), t);
			}
		});
	}

	public void start() {
		rxThread.start();
		txThread.start();
		processThread.start();
	}

	public void addPeer(String id, SocketAddress addr) {
		remotePeers.put(id, addr);
		log.info("OSC peer added:" + addr);
	}

	public Collection<SocketAddress> getPeers() {
		return remotePeers.values();
	}

	public void send(String address, Collection<?> args) {
		send(address, args.toArray(new Object[args.size()]));
	}

	public void send(String address, Object... args) {
		ByteBuffer packet = OSCMessage.getBytes(address, args);
		for (SocketAddress destination : remotePeers.values())
			send(destination, packet);
	}

	@Override
	public void send(SocketAddress destination, ByteBuffer packet) {
		DatagramPacket p = new DatagramPacket(packet.array(), packet.capacity());
		p.setSocketAddress(destination);
		txQueue.add(p);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Platform.get().init();

		OSCServer osc = new OSCServer(55555);

		//		osc.addPeer("self", osc.address);

		osc.addHandler("/hello", new IOSCHandler() {
			@Override
			public Object[] handle(String[] address, int addrIdx, StringBuilder typeString, long timestamp, Object... args) {
				System.out.println(
						TextUtilities.toString("", "/", "", address, TextUtilities.NONE) + " " +
								TextUtilities.toString("(", ",", ")", args)
						);
				return null;
			}
		});

		osc.start();

		Platform.get().run();

		/*

		for(int i = 0; i < 10; i++) {
			Thread.sleep(1000);
			osc.send("/hello", "Hello OSC", i);
		}*/
	}
}
