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

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class NetworkUtilities {
	public static final int IPTOS_LOWCOST     = 0x02;
	public static final int IPTOS_RELIABILITY = 0x04;
	public static final int IPTOS_THROUGHPUT  = 0x08;
	public static final int IPTOS_LOWDELAY    = 0x10;
	
	private static final int[][] PRIVATE_ADDRS = { { 10 }, { 192, 168 }, { 172, 16 }, { 172, 17 }, { 172, 18 }, { 172, 19 }, { 172, 20 }, { 172, 21 },
			{ 172, 22 }, { 172, 23 }, { 172, 24 }, { 172, 25 }, { 172, 26 }, { 172, 27 }, { 172, 28 }, { 172, 29 }, { 172, 30 }, { 172, 31 }, };

	public static InetAddress getDefaultInterface() throws UnknownHostException, SocketException {
		InetAddress addr = getFirstNonLoopbackAddress(true);
		if (addr != null)
			return addr;
		return getLocalHost(true);
	}

	private static InetAddress getFirstNonLoopbackAddress(boolean ipv4only) throws SocketException {
		for (InetAddress addr : NetworkUtilities.getLocalAddresses(ipv4only)) {
			if (!addr.isLoopbackAddress()) {
				if (isPrivate(addr))
					return addr;
			}
		}
		return null;
	}

	private static InetAddress getLocalHost(boolean ipv4only) throws UnknownHostException, SocketException {
		InetAddress result = InetAddress.getLocalHost();
		if (ipv4only && !(result instanceof Inet4Address)) {
			for (InetAddress addr : NetworkUtilities.getLocalAddresses(true)) {
				if (isPrivate(addr)) {
					result = addr;
					break;
				}
			}
		}
		return result;
	}

	public static List<InetAddress> getLocalAddresses(boolean ipv4only) throws SocketException {
		List<InetAddress> result = new ArrayList<>();
		for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
			NetworkInterface nif = e.nextElement();
			for (Enumeration<InetAddress> en = nif.getInetAddresses(); en.hasMoreElements();) {
				InetAddress addr = en.nextElement();
				if (ipv4only && !(addr instanceof Inet4Address))
					continue;
				result.add(addr);
			}
		}
		return result;
	}

	private static boolean isPrivate(InetAddress addr) {
		byte[] addrb = addr.getAddress();
		for (int i = PRIVATE_ADDRS.length; --i >= 0;) {
			boolean valid = true;
			for (int j = 0; j < PRIVATE_ADDRS[i].length; j++)
				if (PRIVATE_ADDRS[i][j] != (addrb[j] & 0xFF)) {
					valid = false;
					break;
				}
			if (valid) {
				return true;
			}
		}
		return false;
	}
	
	public static String URLEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}

	public static String URLDecode(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}

	public static InetAddress multicastAddress(int b, int c, int d) throws UnknownHostException {
		return InetAddress.getByAddress(new byte[] {(byte)224, (byte)b, (byte)c, (byte)d});
	}
}
