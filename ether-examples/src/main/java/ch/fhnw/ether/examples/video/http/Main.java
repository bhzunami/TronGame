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

package ch.fhnw.ether.examples.video.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import ch.fhnw.util.TextUtilities;

public class Main {
	static File file;

	public static void main(String[] args) throws Exception {
		file = new File(args[0]);
		if(!(file.exists())) throw new FileNotFoundException(file.getAbsolutePath());
		int port = 9000;
		if(args.length > 1) port = Integer.parseInt(args[1]);
		HttpServer server = HttpServer.create(new InetSocketAddress(9000), 0);
		System.out.println("HTTP Server: http://" + InetAddress.getLocalHost().getHostName() + ":" + port + "/");
		server.createContext("/", new WebCam());
		server.setExecutor(null);
		server.start();
	}

	static class WebCam implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			URI uri = t.getRequestURI();
			if(uri.getPath().equals("/")) {
				byte[] response = (
						"<!DOCTYPE html><html><head><title>[tvver] Simple Video Server</title></head>" +
								"<body>"+
								"<video id=\"video\">"+
								"<source src=\"video.webm\" type=\"video/webm\">"+
								"</video>"+
								"<div id=\"buttonbar\">" +
								"<button id=\"play\" onclick=\"video.play()\">Play</button>" +
								"<button id=\"restart\" onclick=\"video.pause()\">Pause</button>" +
								"</div>" +
								"</body>" +
						"</html>").getBytes();
				t.sendResponseHeaders(200, response.length);
				try(OutputStream os = t.getResponseBody()) {
					os.write(response);
				}
			} else if(uri.getPath().startsWith("/video.")) {
				String type = TextUtilities.getFileExtensionWithoutDot(uri.getPath());
				File video = new File(file.getParentFile(), TextUtilities.getFileNameWithoutExtension(file) + "." + type);
				t.sendResponseHeaders(200, (int)video.length());
				try(OutputStream os = t.getResponseBody()) {
					Files.copy(video.toPath(), os);
				}
			}
		}
	}
}
