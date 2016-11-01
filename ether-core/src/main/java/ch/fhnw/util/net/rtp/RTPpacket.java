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

public class RTPpacket extends AbstractRTPpacket {
	final static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
	/*
	    0                   1                   2                   3
	    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	   |V=2|P|X|  CC   |M|     PT      |       sequence number         |
	   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	   |                           timestamp                           |
	   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	   |           synchronization source (SSRC) identifier            |
	   +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	   |            contributing source (CSRC) identifiers             |
	   |                             ....                              |
	   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 */
	public RTPpacket(int payloadType, int seqNb, int timestamp) {
		add(0x80);
		add(payloadType & 0x7F);
		add(seqNb >> 8);
		add(seqNb);
		add(timestamp >> 24);
		add(timestamp >> 16);
		add(timestamp >> 8);
		add(timestamp);
		int ssrc = 1601061148;
		add(ssrc >> 24);
		add(ssrc >> 16);
		add(ssrc >> 8);
		add(ssrc);
	}

	public void set_marker(boolean state) {
		if(state)
			packet._getArray()[1] |= 0x80;
		else
			packet._getArray()[1] &= ~0x80;
	}
	
	@Override
	public String toString() {
		String result = "[RTP]";
		result += s(0, 4, " V=",     0, 2);
		result += s(0, 4, " P",      2, 1);
		result += s(0, 4, " X",      3, 1);
		result += s(0, 4, " CC=",    4, 4);
		result += s(0, 4, " M",      8, 1);
		result += s(0, 4, " PT=",    9, 7);
		result += s(0, 4, " seqNb=",16,16);
		result += s(4, 4, " ts=",    0,32);
		result += s(8, 4, " ssrc=",  0,32);
		if(bitx(0,4,9,7) == MJPEG_TYPE) {
			result += s(12, 4, " type_spec=", 0, 8);
			result += s(12, 4, " frag_off=",  8, 24);
			result += s(16, 4, " type=",      0, 8);
			result += s(16, 4, " Q=",         8, 8);
			result += s(16, 4, " Width=",    16, 8);
			result += s(16, 4, " Height=",   24, 8);
		}
		return result;
	}
}
