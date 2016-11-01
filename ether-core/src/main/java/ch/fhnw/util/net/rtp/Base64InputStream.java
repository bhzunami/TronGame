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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Base64InputStream extends InputStream {
    /**
     * This array is a lookup table that translates 6-bit positive integer
     * index values into their "Base64 Alphabet" equivalents as specified
     * in "Table 1: The Base64 Alphabet" of RFC 2045 (and RFC 4648).
     */
    private static final char[] toBase64 = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };
	
	/**
	 * Lookup table for decoding unicode characters drawn from the
	 * "Base64 Alphabet" (as specified in Table 1 of RFC 2045) into
	 * their 6-bit positive integer equivalents.  Characters that
	 * are not in the Base64 alphabet but fall within the bounds of
	 * the array are encoded to -1.
	 *
	 */
	private static final int[] fromBase64 = new int[256];
	static {
		Arrays.fill(fromBase64, -1);
		for (int i = 0; i < toBase64.length; i++)
			fromBase64[toBase64[i]] = i;
		fromBase64['='] = -2;
	}

	private final InputStream is;
	private int bits = 0;            // 24-bit buffer for decoding
	private int nextin = 18;         // next available "off" in "bits" for input;
	// -> 18, 12, 6, 0
	private int nextout = -8;        // next available "off" in "bits" for output;
	// -> 8, 0, -8 (no byte for output)
	private boolean eof = false;
	private boolean eom = false;
	private boolean closed = false;

	Base64InputStream(InputStream is) {
		this.is = is;
	}

	private byte[] sbBuf = new byte[1];

	@Override
	public int read() throws IOException {
		return read(sbBuf, 0, 1) == -1 ? -1 : sbBuf[0] & 0xff;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (closed)
			throw new IOException("Stream is closed");
		if (eom && nextout < 0) {
			bits    = 0;      
			nextin  = 18;        
			nextout = -8;      
			eom     = false;
		}
		if (eof && nextout < 0)    // eof and no leftover
			return -1;
		if (off < 0 || len < 0 || len > b.length - off)
			throw new IndexOutOfBoundsException();
		int oldOff = off;
		if (nextout >= 0) {       // leftover output byte(s) in bits buf
			do {
				if (len == 0)
					return off - oldOff;
				b[off++] = (byte)(bits >> nextout);
				len--;
				nextout -= 8;
			} while (nextout >= 0);
			bits = 0;
		}
		while (len > 0) {
			int v = is.read();
			if (v == -1) {
				eof = true;
				if (nextin != 18) {
					if (nextin == 12)
						throw new IOException("Base64 stream has one un-decoded dangling byte.");
					// treat ending xx/xxx without padding character legal.
					// same logic as v == '=' below
					b[off++] = (byte)(bits >> (16));
					len--;
					if (nextin == 0) {           // only one padding byte
						if (len == 0) {          // no enough output space
							bits >>= 8;          // shift to lowest byte
							nextout = 0;
						} else {
							b[off++] = (byte) (bits >>  8);
						}
					}
				}
				if (off == oldOff)
					return -1;
				return off - oldOff;
			}
			if (v == '=') {                  // padding byte(s)
				// =     shiftto==18 unnecessary padding
				// x=    shiftto==12 dangling x, invalid unit
				// xx=   shiftto==6 && missing last '='
				// xx=y  or last is not '='
				if (nextin == 18 || nextin == 12 || nextin == 6 && is.read() != '=') {
					throw new IOException("Illegal base64 ending sequence:" + nextin);
				}
				b[off++] = (byte)(bits >> (16));
				len--;
				if (nextin == 0) {           // only one padding byte
					if (len == 0) {          // no enough output space
						bits >>= 8;          // shift to lowest byte
						nextout = 0;
					} else {
						b[off++] = (byte) (bits >>  8);
					}
				}
				eom = true;
				break;
			}
			if ((v = fromBase64[v]) == -1) {
				throw new IOException("Illegal base64 character " +
						Integer.toString(v, 16));
			}
			bits |= (v << nextin);
			if (nextin == 0) {
				nextin = 18;    // clear for next
				nextout = 16;
				while (nextout >= 0) {
					b[off++] = (byte)(bits >> nextout);
					len--;
					nextout -= 8;
					if (len == 0 && nextout >= 0) {  // don't clean "bits"
						return off - oldOff;
					}
				}
				bits = 0;
			} else {
				nextin -= 6;
			}
		}
		return off - oldOff;
	}

	@Override
	public int available() throws IOException {
		if (closed)
			throw new IOException("Stream is closed");
		return is.available();   // TBD:
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			is.close();
		}
	}
}
