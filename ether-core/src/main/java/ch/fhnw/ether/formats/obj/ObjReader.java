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

package ch.fhnw.ether.formats.obj;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;

import ch.fhnw.ether.formats.AbstractModelReader;

public final class ObjReader extends AbstractModelReader {
	public ObjReader(File file) throws IOException {
		this(file.toURI().toURL());
	}
	
	public ObjReader(File file, Options first, Options... rest) throws IOException {
		this(file.toURI().toURL(), first, rest);
	}
	
	public ObjReader(File file, EnumSet<Options> options) throws IOException {
		this(file.toURI().toURL(), options);
	}

	public ObjReader(URL resource) throws IOException {
		this(resource, EnumSet.noneOf(Options.class));
	}
	
	public ObjReader(URL resource, Options first, Options... rest) throws IOException {
		this(resource, EnumSet.of(first, rest));
	}

	public ObjReader(URL resource, EnumSet<Options> options) throws IOException {
		super(resource, options);
		new ObjParser(getObject(), options.contains(Options.CONVERT_TO_Z_UP)).parse();
	}
}
