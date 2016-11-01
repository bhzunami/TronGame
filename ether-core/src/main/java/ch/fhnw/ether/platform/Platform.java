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

package ch.fhnw.ether.platform;

public final class Platform {
	private static final boolean USE_SWT = true;
	
	private static final IPlatform PLATFORM = USE_SWT ? new SWTPlatform() : new GLFWPlatform();
	
	public static IPlatform get() {
		return PLATFORM;
	}
	
	public static final int WINDOWS_POST_XP_MAJOR = 6;
	public static final int WINDOWS_VISTA_MINOR   = 0;
	public static final int WINDOWS_7_MINOR       = 1;
	public static final int WINDOWS_8_MINOR       = 2;

	public enum OS {
		UNKNOWN("?"), LINUX("linux"), WINDOWS("win32"), MACOSX("macosx");

		private String shortString;

		OS(String shortString) {
			this.shortString = shortString;
		}

		public String shortString() {
			return shortString;
		}

		private static OS[] valuesro;
		public static OS[] valuesro() {
			if(valuesro == null) valuesro = values();
			return valuesro;
		}
	}

	public enum Architecture {
		UNKNOWN(32), X86(32), X86_64(64);
		int wordSize;
		Architecture(int wordSize) {
			this.wordSize = wordSize;
		}
	}

	private static OS  os;
	private static int osVersionMajor = -1;
	private static int osVersionMinor = -1;
	private static Architecture architecture;
	private static final int WORD_SIZE = System.getProperty("os.arch").endsWith("64") ? 64 : 32; 


	public static OS getOS() {
		if(os == null) {
			if(System.getProperty("os.name").toLowerCase().startsWith("linux"))
				os = OS.LINUX;
			else if(System.getProperty("os.name").toLowerCase().startsWith("win"))
				os = OS.WINDOWS;
			else if(System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
				os = OS.MACOSX;
			else
				os = OS.UNKNOWN;
		}
		return os;
	}

	public static Architecture getArchitecture() {
		if (architecture == null) {
			String arch = "";
			arch = System.getProperty("os.arch").replace("amd","x86-");
			if (arch.equals("x86")) architecture = Architecture.X86;
			else if (arch.equals("x86-64")) architecture = Architecture.X86_64;
			else architecture = Architecture.UNKNOWN;
		}
		return architecture;
	}

	public static int getWordSize() {
		return WORD_SIZE;
	}

	public static int getOSMajor() {
		if(osVersionMajor == -1) {
			try {
				String[] version = System.getProperty("os.version").split("[.,/;]");
				osVersionMajor   = Integer.parseInt(version[0].trim());
			} catch(Throwable t) {
				osVersionMajor = 0;
			}
		}
		return osVersionMajor;
	}

	public static int getOSMinor() {
		if(osVersionMinor == -1) {
			try {
				String[] version = System.getProperty("os.version").split("[.,/;]");
				osVersionMinor   = Integer.parseInt(version[1].trim());
			} catch(Throwable t) {
				osVersionMinor = 0;
			}
		}
		return osVersionMinor;
	}

	public static boolean greaterOrEqual(int major, int minor) {
		long current = getOSMajor();
		current <<= 32L;
		current |= getOSMinor();

		long majMin = major;
		majMin <<= 32L;
		majMin |= minor;


		return current >= majMin;
	}

	public static boolean isWindowsVista() {
		return getOS() == OS.WINDOWS && getOSMajor() == WINDOWS_POST_XP_MAJOR && getOSMinor() == WINDOWS_VISTA_MINOR;
	}

	public static boolean isWindows7() {
		return getOS() == OS.WINDOWS && getOSMajor() == WINDOWS_POST_XP_MAJOR && getOSMinor() == WINDOWS_7_MINOR;
	}
}
