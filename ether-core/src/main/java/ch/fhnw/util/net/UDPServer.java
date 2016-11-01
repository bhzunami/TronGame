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

package ch.fhnw.util.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class UDPServer {
	public interface UDPHandler {
		void handle(DatagramPacket packet);
	}

	private static final int RECEIVE_BUFFER_SIZE = 1024 * 1024;
	private static final int SEND_BUFFER_SIZE = 1024 * 1024;

	private final InetSocketAddress address;
	private final DatagramSocket    socket;
	private final UDPHandler        handler;

	public UDPServer(int port, UDPHandler handler) throws IOException {
		address = new InetSocketAddress(NetworkUtilities.getDefaultInterface(), port);
		socket = new DatagramSocket(address.getPort());
		this.handler = handler;
		int dec = socket.getReceiveBufferSize();
		for (int size = RECEIVE_BUFFER_SIZE; socket.getReceiveBufferSize() < size; size -= dec) {
			socket.setReceiveBufferSize(size);
		}
		dec = socket.getSendBufferSize();
		for (int size = SEND_BUFFER_SIZE; socket.getSendBufferSize() < size; size -= dec) {
			socket.setSendBufferSize(size);
		}

		final Thread receiveThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (;;) {
					try {
						byte[] buffer = new byte[socket.getReceiveBufferSize()];
						DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
						socket.receive(packet);
						try {
							UDPServer.this.handler.handle(packet);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} catch (Exception ignored) {
					}
				}
			}
		}, "udp server");
		receiveThread.setDaemon(true);
		receiveThread.setPriority(Thread.MAX_PRIORITY);
		receiveThread.start();
	}
}
