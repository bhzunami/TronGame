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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.ether.platform.IImageSupport.FileFormat;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.util.ArrayUtilities;
import ch.fhnw.util.ByteList;
import ch.fhnw.util.Log;
import ch.fhnw.util.Log.Level;

public class RTPmjpg {
	private static final Log LOG = Log.create(Level.SEVERE, Level.WARN);
	
	/**
	 * Payload encode JPEG pictures into RTP packets according to RFC 2435.
	 * For detailed information see: http://www.rfc-editor.org/rfc/rfc2435.txt
	 *
	 * The payloader takes a JPEG picture, scans the header for quantization
	 * tables (if needed) and constructs the RTP packet header followed by
	 * the actual JPEG entropy scan.
	 *
	 * The payloader assumes that correct width and height is found in the caps.
	 * 
	 * Implementation based gstreamer/gstrtpjpegpay.c
	 */

	/*
	 * RtpJpegMarker:
	 * @JPEG_MARKER: Prefix for JPEG marker
	 * @JPEG_MARKER_SOI: Start of Image marker
	 * @JPEG_MARKER_JFIF: JFIF marker
	 * @JPEG_MARKER_CMT: Comment marker
	 * @JPEG_MARKER_DQT: Define Quantization Table marker
	 * @JPEG_MARKER_SOF: Start of Frame marker
	 * @JPEG_MARKER_DHT: Define Huffman Table marker
	 * @JPEG_MARKER_SOS: Start of Scan marker
	 * @JPEG_MARKER_EOI: End of Image marker
	 * @JPEG_MARKER_DRI: Define Restart Interval marker
	 * @JPEG_MARKER_H264: H264 marker
	 *
	 * Identifers for markers in JPEG header
	 */
	enum JPEG_MARKER {
		MARKER(0xFF),
		SOI(0xD8),
		JFIF(0xE0),
		CMT(0xFE),
		DQT(0xDB),
		SOF(0xC0),
		DHT(0xC4),
		SOS(0xDA),
		EOI(0xD9),
		DRI(0xDD),
		H264(0xE4);

		byte v;
		JPEG_MARKER(int val) {this.v = (byte)val;}
		public static JPEG_MARKER valueOf(byte val) {
			for(JPEG_MARKER m : values())
				if(m.v == val)
					return m;
			return null;
		}
	}

	private static final int DEFAULT_JPEG_QUANT = 255;
	private static final int DEFAULT_JPEG_TYPE  = 1;

	enum PROP {
		PROP_0,
		JPEG_QUALITY,
		JPEG_TYPE,
		LAST
	}

	enum Q_TABLE {
		Q_TABLE_0,
		Q_TABLE_1,
		Q_TABLE_MAX                   /* only support for two tables at the moment */
	}

	/*
	 * RtpJpegHeader:
	 * @type_spec: type specific
	 * @offset: fragment offset
	 * @type: type field
	 * @q: quantization table for this frame
	 * @width: width of image in 8-pixel multiples
	 * @height: height of image in 8-pixel multiples
	 *
	 *  0                   1                   2                   3
	 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * | Type-specific |              Fragment Offset                  |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |      Type     |       Q       |     Width     |     Height    |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 */
	static class RtpJpegHeader {
		int  type_spec_offset;
		int  type;
		int  q;
		int  width;
		int  height;
		public int sizeof() {
			return 8;
		}
		public byte[] toArray() {
			byte[] result = new byte[sizeof()];
			result[0] = (byte) (type_spec_offset >> 24);
			result[1] = (byte) (type_spec_offset >> 16);
			result[2] = (byte) (type_spec_offset >> 8);
			result[3] = (byte) (type_spec_offset);
			result[4] = (byte) (type);
			result[5] = (byte) q;
			result[6] = (byte) width;
			result[7] = (byte) height;
			return result;
		}
	}

	/*
	 * RtpQuantHeader
	 * @mbz: must be zero
	 * @precision: specify size of quantization tables
	 * @length: length of quantization data
	 *
	 * 0                   1                   2                   3
	 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |      MBZ      |   Precision   |             Length            |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |                    Quantization Table Data                    |
	 * |                              ...                              |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 */
	static class RtpQuantHeader {
		byte mbz;
		byte precision;
		int  length;
		public int sizeof() {
			return 4;
		}
		public byte[] toArray() {
			byte[] result = new byte[4];
			result[0] = mbz;
			result[1] = precision;
			result[2] = (byte) (length >> 8);
			result[3] = (byte) (length);
			return result;
		}
	}

	/*
	 * RtpRestartMarkerHeader:
	 * @restartInterval: number of MCUs that appear between restart markers
	 * @restartFirstLastCount: a combination of the first packet mark in the chunk
	 *                         last packet mark in the chunk and the position of the
	 *                         first restart interval in the current "chunk"
	 *
	 *  0                   1                   2                   3
	 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  |       Restart Interval        |F|L|       Restart Count       |
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *
	 *  The restart marker header is implemented according to the following
	 *  methodology specified in section 3.1.7 of rfc2435.txt.
	 *
	 *  "If the restart intervals in a frame are not guaranteed to be aligned
	 *  with packet boundaries, the F (first) and L (last) bits MUST be set
	 *  to 1 and the Restart Count MUST be set to 0x3FFF.  This indicates
	 *  that a receiver MUST reassemble the entire frame before decoding it."
	 *
	 */

	/* FIXME: restart marker header currently unsupported */
	static class RtpRestartMarkerHeader {
		int restart_interval;
		int restart_count;
		public int sizeof() {
			return 4;
		}
		public byte[] toArray() {
			byte[] result = new byte[4];
			result[0] = (byte)(restart_interval >>8);
			result[1] = (byte)(restart_interval);
			result[2] = (byte)(restart_count>>8);
			result[3] = (byte)(restart_count);
			return result;
		}
	}

	static class CompInfo {
		byte id;
		byte samp;
		byte qt;
		CompInfo(CompInfo o) {
			id   = o.id;
			samp = o.samp;
			qt   = o.qt;
		}
		CompInfo() {}
	}

	private int                 type;
	private int                 height;
	private int                 width;
	private int                 quant;
	private final int           seqNb;
	private final int           timestamp;
	private int                 mtu = 1450;
	private final IHostImage    image;
	
	public RTPmjpg(IHostImage img, int seqNb, int timestamp) {
		quant          = DEFAULT_JPEG_QUANT;
		type           = DEFAULT_JPEG_TYPE;
		width          = -1;
		height         = -1;
		
		this.image     = img;
		this.seqNb     = seqNb;
		this.timestamp = timestamp;
	}

	public List<RTPpacket> createPackets() throws IOException {
		try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Platform.get().getImageSupport().write(image, out, FileFormat.JPEG);
			byte[] result = out.toByteArray();
			return gst_rtp_jpeg_pay_handle_buffer(result, result.length, seqNb, timestamp);
		}
	}

	int gst_rtp_jpeg_pay_header_size (byte[] data, int offset) {
		return ((data[offset] &0xFF) << 8) | (data[offset + 1] & 0xFF);
	}

	private static byte[] extract(byte[] a, int offset, int len) {
		return Arrays.copyOfRange(a, offset, offset + len);
	}

	private int memcmp(byte[] s1, byte[] s2, int n) {
		byte u1;
		byte u2;
		int s1i = 0;
		int s2i = 0;
		for(;n-- != 0; s1i++, s2i++) {
			u1 = s1[s1i];
			u2 = s2[s2i];
			if ( u1 != u2)
				return (u1&0xFF)-(u2&0xFF);
		}
		return 0;	
	}

	private static int GST_ROUND_UP_8(int num) {
		return (((num)+7)&~7);
	}

	int gst_rtp_jpeg_pay_read_quant_table (byte[] data, int size, int offset, byte[][] tables) {
		try {
			int  quant_size;
			int  tab_size;
			byte prec;
			byte id;

			if (offset + 2 > size)
				throw new Exception("not enough data");

			quant_size = gst_rtp_jpeg_pay_header_size (data, offset);
			if (quant_size < 2)
				throw new Exception("quant_size too small ("+quant_size+" < 2)");

			/* clamp to available data */
			if (offset + quant_size > size)
				quant_size = size - offset;

			offset += 2;
			quant_size -= 2;

			while (quant_size > 0) {
				/* not enough to read the id */
				if (offset + 1 > size)
					break;

				id = (byte) (data[offset] & 0x0f);
				if (id == 15) {
					/* invalid id received - corrupt data */
					size = offset + quant_size;
					throw new Exception("invalid id");
				}

				prec = (byte) ((data[offset] & 0xf0) >> 4);
				if(prec != 0)
					tab_size = 128;
				else
					tab_size = 64;

				/* there is not enough for the table */
				if (quant_size < tab_size + 1) {
					size = offset + quant_size;
					throw new Exception("not enough data for table ("+quant_size+" < "+(tab_size + 1)+")");
				}

				LOG.info("read quant table "+id+", tab_size "+tab_size+", prec "+prec);

				tables[id] = extract(data, offset+1, tab_size);

				tab_size += 1;
				quant_size -= tab_size;
				offset += tab_size;
			}
			return offset + quant_size;
		} catch(Exception e) {
			LOG.warning(e);
			return size;
		}
	}

	boolean gst_rtp_jpeg_pay_read_sof (byte[] data, int size, int[] offset, CompInfo[] info, byte[][] tables, int tables_elements) {
		int sof_size;
		int off;
		int width;
		int height;
		int infolen;
		CompInfo elem = new CompInfo();
		int i;
		int j;

		off = offset[0];

		try {
			/* we need at least 17 bytes for the SOF */
			if (off + 17 > size)
				throw new Exception("Wrong size "+size+" (needed "+(off + 17)+").");

			sof_size = gst_rtp_jpeg_pay_header_size (data, off);
			if (sof_size < 17)
				throw new Exception("Wrong SOF length "+sof_size+".");

			offset[0] += sof_size;

			/* skip size */
			off += 2;

			/* precision should be 8 */
			if (data[off++] != 8)
				throw new Exception("Wrong precision, expecting 8.");

			/* read dimensions */
			height = ((data[off]     & 0xFF) << 8) | (data[off + 1] & 0xFF);
			width  = ((data[off + 2] & 0xFF) << 8) | (data[off + 3] & 0xFF);
			off += 4;

			LOG.info("got dimensions "+ width+"x"+height);

			if (height == 0) {
				throw new Exception("Wrong dimension, size "+width+"x"+height);
			}
			if (height > 2040) {
				this.height = 0;
			}
			if (width == 0) {
				throw new Exception("Wrong dimension, size "+width+"x"+height);
			}
			if (width > 2040) {
				this.width = 0;
			}

			if (height == 0 || width == 0) {
				this.height = 0;
				this.width  = 0;
			} else {
				this.height = GST_ROUND_UP_8 (height) / 8;
				this.width  = GST_ROUND_UP_8 (width) / 8;
			}

			/* we only support 3 components */
			if (data[off++] != 3)
				throw new Exception("Wrong number of components");

			infolen = 0;
			for (i = 0; i < 3; i++) {
				elem.id   = data[off++];
				elem.samp = data[off++];
				elem.qt   = data[off++];
				LOG.info("got comp "+elem.id+", samp "+elem.samp+", qt "+elem.qt);
				/* insertion sort from the last element to the first */
				for (j = infolen; j > 1; j--) {
					if(info[j - 1].id < elem.id)
						break;
					info[j] = info[j - 1];
				}
				info[j] = new CompInfo(elem);
				infolen++;
			}

			/* see that the components are supported */
			if (info[0].samp == 0x21)
				type = 0;
			else if (info[0].samp == 0x22)
				type = 1;
			else
				throw new Exception("Invalid component:"+info[0].samp);

			if (!(info[1].samp == 0x11))
				throw new Exception("Invalid component:"+info[1].samp);

			if (!(info[2].samp == 0x11))
				throw new Exception("Invalid component:"+info[2].samp);

			/* the other components are free to use any quant table but they have to
			 * have the same table id */
			if (info[1].qt != info[2].qt) {
				/* Some MJPG (like the one from the Logitech C-920 camera) uses different
				 * quant tables for component 1 and 2 but both tables contain the exact
				 * same data, so we could consider them as being the same tables */
				if (!(info[1].qt < tables_elements &&
						info[2].qt < tables_elements &&
						tables[info[1].qt].length > 0 &&
						tables[info[1].qt].length == tables[info[2].qt].length &&
						memcmp (tables[info[1].qt], tables[info[2].qt],	tables[info[1].qt].length) == 0))
					throw new Exception("Invalid component");
			}

			return true;
		} catch(Exception e) {
			LOG.warning(e);
			return false;
		}
	}

	boolean gst_rtp_jpeg_pay_read_dri(byte[] data, int size, int[] offset, RtpRestartMarkerHeader dri) {
		int dri_size;
		int off;

		off = offset[0];

		try {
			/* we need at least 4 bytes for the DRI */
			if (off + 4 > size) {
				offset[0] = size;
				throw new Exception("not enough data for DRI");
			}

			dri_size = gst_rtp_jpeg_pay_header_size (data, off);
			if (dri_size < 4) {
				offset[0] += dri_size;
				throw new Exception("DRI size too small ("+dri_size+")");
			}

			offset[0] += dri_size;
			off += 2;

			dri.restart_interval = ((data[off] & 0xFF) << 8) | (data[off + 1] & 0xFF);
			dri.restart_count    = 0xFFFF;

			return dri.restart_interval > 0;
		}  catch(Exception e) {
			LOG.warning(e);
			return false;
		}
	}

	JPEG_MARKER gst_rtp_jpeg_pay_scan_marker (byte[] data, int size, int[] offset) {
		while ((data[(offset[0])++] != JPEG_MARKER.MARKER.v) && ((offset[0]) < size));

		if((offset[0]) >= size) {
			LOG.info("found EOI marker");
			return JPEG_MARKER.EOI;
		}
		JPEG_MARKER marker;

		marker = JPEG_MARKER.valueOf(data[offset[0]]);
		LOG.info("found "+marker+" marker at offset "+offset[0]);
		offset[0]++;
		return marker;
	}

	private static final int RTP_HEADER_LEN           = 12;

	List<RTPpacket> gst_rtp_jpeg_pay_handle_buffer(byte[] data, int size, int seqNb, int timestamp) {
		List<RTPpacket> result = new ArrayList<>();
		RtpJpegHeader jpeg_header = new RtpJpegHeader();
		RtpQuantHeader quant_header = new RtpQuantHeader();
		RtpRestartMarkerHeader restart_marker_header = new RtpRestartMarkerHeader();
		byte[][] tables = new byte[15][];
		CompInfo info[] = new CompInfo[3];
		int quant_data_size;
		int bytes_left;
		int jpeg_header_size = 0;
		int[] offset = new int[1];
		boolean frame_done;
		boolean sos_found, sof_found, dqt_found, dri_found;
		int i;

		LOG.info("got buffer size "+size);

		/* parse the jpeg header for 'start of scan' and read quant tables if needed */
		sos_found = false;
		dqt_found = false;
		sof_found = false;
		dri_found = false;

		try {
			while (!sos_found && (offset[0] < size)) {
				LOG.info("checking from offset "+offset[0]);
				switch (gst_rtp_jpeg_pay_scan_marker (data, size, offset)) {
				case JFIF:
				case CMT:
				case DHT:
				case H264:
					LOG.info("skipping marker");
					offset[0] += gst_rtp_jpeg_pay_header_size(data, offset[0]);
					break;
				case SOF:
					if (!gst_rtp_jpeg_pay_read_sof (data, size, offset, info, tables, tables.length))
						throw new Exception("invalid format");
					sof_found = true;
					break;
				case DQT:
					LOG.info("DQT found");
					offset[0] = gst_rtp_jpeg_pay_read_quant_table(data, size, offset[0], tables);
					dqt_found = true;
					break;
				case SOS:
					sos_found = true;
					LOG.info("SOS found");
					jpeg_header_size = offset[0] + gst_rtp_jpeg_pay_header_size(data, offset[0]);
					break;
				case EOI:
					LOG.warning("EOI reached before SOS!");
					break;
				case SOI:
					LOG.info("SOI found");
					break;
				case DRI:
					LOG.info("DRI found");
					if (gst_rtp_jpeg_pay_read_dri (data, size, offset, restart_marker_header))
						dri_found = true;
					break;
				default:
					break;
				}
			}
			if (!dqt_found || !sof_found)
				throw new Exception("Unsupported JPEG");

			/* by now we should either have negotiated the width/height or the SOF header
			 * should have filled us in */
			if (width < 0 || height < 0)
				throw new Exception("No size given");

			LOG.info("header size "+jpeg_header_size);

			size -= jpeg_header_size;
			data = ArrayUtilities.dropFirst(jpeg_header_size, data);
			offset[0] = 0;

			if (dri_found)
				type += 64;

			/* prepare stuff for the jpeg header */
			jpeg_header.type_spec_offset = 0;
			jpeg_header.type             = type;
			jpeg_header.q                = quant;
			jpeg_header.width            = width;
			jpeg_header.height           = height;

			/* collect the quant headers sizes */
			quant_header.mbz = 0;
			quant_header.precision = 0;
			quant_header.length = 0;
			quant_data_size = 0;

			if(quant > 127) {
				/* for the Y and U component, look up the quant table and its size. quant
				 * tables for U and V should be the same */
				for (i = 0; i < 2; i++) {
					int qsize;
					int qt;

					qt = info[i].qt;
					if (qt >= tables.length)
						throw new Exception("Invalid quant tables");

					qsize = tables[qt].length;
					if (qsize == 0)
						throw new Exception("Invalid quant tables");

					quant_header.precision |= (qsize == 64 ? 0 : (1 << i));
					quant_data_size += qsize;
				}
				quant_header.length = quant_data_size;
				quant_data_size += quant_header.sizeof();
			}

			LOG.info("quant_data size"+quant_data_size);

			bytes_left = jpeg_header.sizeof() + quant_data_size + size;

			if (dri_found)
				bytes_left += restart_marker_header.sizeof();

			frame_done = false;
			for(;;) {
				ByteList payload;
				int payload_size;
				RTPpacket rtp = new RTPpacket(RTPpacket.MJPEG_TYPE, seqNb, timestamp);
				int rtp_header_size = RTP_HEADER_LEN;

				/* The available room is the packet MTU, minus the RTP header length. */
				payload_size = (bytes_left < (mtu - rtp_header_size) ? bytes_left : (mtu - rtp_header_size));

				if (payload_size == bytes_left) {
					LOG.info("last packet of frame");
					frame_done = true;
					rtp.set_marker(true);
				}

				payload = rtp.getPayload();

				/* update offset */
				jpeg_header.type_spec_offset = offset[0];
				payload.addAll(jpeg_header.toArray());
				payload_size -= jpeg_header.sizeof();

				if (dri_found) {
					payload.addAll(restart_marker_header.toArray());
					payload_size -= restart_marker_header.sizeof();
				}

				/* only send quant table with first packet */
				if (quant_data_size > 0) {
					payload.addAll(quant_header.toArray());

					/* copy the quant tables for luma and chrominance */
					for (i = 0; i < 2; i++) {
						int qsize;
						int qt;

						qt = info[i].qt;
						qsize = tables[qt].length;
						payload.addAll(tables[qt]);

						LOG.info("component "+i+" using quant "+qt+", size "+qsize);
					}
					payload_size -= quant_data_size;
					bytes_left -= quant_data_size;
					quant_data_size = 0;
				}
				LOG.info("adding payload size "+ payload_size);
				payload.addAll(data, 0, payload_size);
				result.add(rtp);
				if(frame_done) break;

				bytes_left -= payload_size;
				offset[0]  += payload_size;
				data = ArrayUtilities.dropFirst(payload_size, data);
			}
			while (!frame_done);
			return result;
		} catch(Exception e) {
			LOG.warning(e);
			return result;
		}
	}

	public void setMTU(int mtu) {
		this.mtu = mtu;
	}

	// for testing
	public static void main(String[] args) throws IOException {
		LOG.setLevels(Log.ALL);
		IHostImage img = IHostImage.create(500,  300, ComponentType.BYTE, ComponentFormat.RGB);
		for(int y = img.getHeight(); --y >= 0;) {
			for(int x = img.getWidth(); --x >= 0;) {
				img.setPixel(x, y, new byte[] { (byte)(Math.random() * 255), (byte)(Math.random() * 255), (byte)(Math.random() * 255) });
			}
		}
		for(RTPpacket p : new RTPmjpg(img, 1000, 2000).createPackets())
			System.out.println(p);
	}
}
