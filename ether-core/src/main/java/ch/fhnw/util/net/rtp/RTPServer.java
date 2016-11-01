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

package ch.fhnw.util.net.rtp;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.util.Log;

public class RTPServer extends Thread {
	private static final Log LOG = Log.create();

	private final Map<Integer, RTPSession>  sessions = new ConcurrentHashMap<>();
	private final Map<String,  RTSPRequest> channels = new ConcurrentHashMap<>();

	private final AtomicReference<IHostImage> currentImage = new AtomicReference<>(
			IHostImage.create(1, 1, ComponentType.BYTE, ComponentFormat.RGB)
			);

	private final int port;

	public RTPServer(int port) {
		this(port, true);
	}

	public RTPServer(int port, boolean start) {
		super(RTPServer.class.getName());
		this.port = port;
		if(start) {
			setPriority(Thread.MIN_PRIORITY);
			setDaemon(true);
			start();
		}
	}

	@Override
	public void run() {
		try(ServerSocket listenSocket = new ServerSocket(port)) {
			System.out.println("# RTSPServer running " +getContentBase(InetAddress.getLocalHost(), port));
			for(;;)
				new RTSPRequest(this, listenSocket.accept());
		} catch (Exception e) {
			LOG.severe(e);
		}
	}

	public RTPSession getSession(RTSPRequest req) {
		return sessions.get(Integer.valueOf(req.getSessionKey()));
	}

	public void addSession(int sessionKey, RTPSession session) {
		sessions.put(sessionKey, session);
	}

	public RTSPRequest getChannel(String key) {
		return channels.get(key);
	}

	public void addChannel(String key, RTSPRequest channel) {
		channels.put(key, channel);
	}

	public static String getContentBase(InetAddress addr, int port) {
		return "rtsp://" + addr.getHostName() + ":" + port + "/video.mjpg";
	}

	public static void main(String args[]) {
		new RTPServer(Integer.parseInt(args[0]), false).run();
	}

	public IHostImage getImage() {
		return currentImage.get();
	}

	public void setImage(IHostImage image) {
		currentImage.set(image);
	}

	public void closeSession(int sessionKey) {
		RTPSession session = sessions.remove(Integer.valueOf(sessionKey));
		if(session != null) session.close();
	}
}