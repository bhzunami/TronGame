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

package ch.fhnw.util;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;

public class Log implements Serializable {
	public enum Level {
		SEVERE, WARN, INFO, DEBUG,
	}

	private static final long    serialVersionUID = -4288206500724445427L;
	public static  final Level[] ALL              = {Level.SEVERE, Level.WARN, Level.INFO, Level.DEBUG};
	
	private final transient PrintStream    out = System.err;
	private final transient String         id;  
	private       transient EnumSet<Level> levels = EnumSet.allOf(Level.class);

	private Log(String id) {
		this.id = id;
	}

	public static Log create(Level ... levels) {
		Log result = new Log(ClassUtilities.getCallerClassName());
		if(levels.length > 0)
			result.setLevels(levels);
		return result;
	}

	public void setLevels(Level ... levels) {
		this.levels = EnumSet.noneOf(Level.class);
		Collections.addAll(this.levels, levels);
	}

	private String format(Level lvl, String msg) {
		return new Date() + ":" + lvl.toString() + '(' + id + ')' + ':' + msg;
	}

	public void info(String msg) {
		if(!(levels.contains(Level.INFO))) return;
		out.println(format(Level.INFO, msg));
	}

	public void info(String msg, Throwable t) {
		if(!(levels.contains(Level.INFO))) return;
		out.println(format(Level.INFO, msg));
		t.printStackTrace(out);
	}

	public void warning(Throwable t) {
		if(!(levels.contains(Level.WARN))) return;
		out.print(format(Level.WARN, ClassUtilities.EMPTY_String));
		t.printStackTrace(out);
	}

	public void warning(String msg) {
		if(!(levels.contains(Level.WARN))) return;
		out.println(format(Level.WARN, msg));
	}

	public void warning(String msg, Throwable t) {
		if(!(levels.contains(Level.WARN))) return;
		out.println(format(Level.WARN, msg));
		t.printStackTrace(out);
	}

	public void severe(Throwable t) {
		if(!(levels.contains(Level.SEVERE))) return;
		out.println(format(Level.SEVERE, ClassUtilities.EMPTY_String));
		t.printStackTrace(out);
	}

	public void severe(String msg) {
		if(!(levels.contains(Level.SEVERE))) return;
		out.println(format(Level.SEVERE, msg));
	}

	public void severe(String msg, Throwable t) {
		if(!(levels.contains(Level.SEVERE))) return;
		out.println(format(Level.SEVERE, msg));
		t.printStackTrace(out);
	}

	public void debug(String msg) {
		if(!(levels.contains(Level.DEBUG))) return;
		out.println(format(Level.DEBUG, msg));
	}
}
