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

package ch.fhnw.ether.render.gl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.fhnw.util.FileUtilities;

public class GLSLReader {
	private final Class<?> libraryBase;
	private final String libraryPath;
	private final URL shader;
	
	private int lineNumber = 1;
	
	private static final Pattern LOCAL_INCLUDE = Pattern.compile("\"(.+?)\"");
	private static final Pattern LIB_INCLUDE = Pattern.compile("<(.+?)>");

	public GLSLReader(Class<?> libraryBase, String libraryPath, URL shader, StringBuilder code, PrintStream out) throws IOException {
		this.libraryBase = libraryBase;
		this.libraryPath = libraryPath;
		this.shader = shader;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(shader.openStream()))) {
			String line;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("#include")) {
					code.append("#line 1\n");
					lineNumber++;
					include(line, code, out);
					code.append("#line ").append(lineNumber).append("\n");
				} else {
					code.append(line);
					code.append('\n');
					lineNumber++;
				}
			}
		} catch (FileNotFoundException e) {
			out.println("include file not found: " + shader);
			throw new IOException();
		}
	}

	private void include(String line, StringBuilder code, PrintStream out) throws IOException {
		Matcher matcher;
		
		matcher = LIB_INCLUDE.matcher(line);
		if (matcher.find()) {
			URL url = libraryBase.getResource(libraryPath + "/" + matcher.group(1));
			new GLSLReader(libraryBase, libraryPath, url, code, out);
			return;
		}
		matcher = LOCAL_INCLUDE.matcher(line);
		if (matcher.find()) {
			URL url = new URL(FileUtilities.getBasePath(shader.toString()) + "/" + matcher.group(1));
			new GLSLReader(libraryBase, libraryPath, url, code, out);
			return;
		}
	}
}
