package ch.fhnw.util.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;

public class AbletonLinkPacket {
	private static final Log log = Log.create();
	
	private final ByteBuffer buffer;

	public final long           timestampMicros;
	public final String         header;
	public final MessageType    messageType;
	public final byte           ttl;
	public final SessionGroupId groupId;
	public final NodeId         ident;
	public final List<Payload>  payload = new ArrayList<>();

	public AbletonLinkPacket(long timestampMicros, DatagramPacket packet) throws IOException {
		this.timestampMicros = timestampMicros;
		buffer = ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());
		buffer.order(ByteOrder.BIG_ENDIAN);
		header = string(buffer, 8);
		if(!(header.startsWith("_asdp_v"))) 
			throw new IOException("Unknonw packet: " + header);
		messageType = MessageType.valueOf(buffer);
		ttl         = buffer.get();
		groupId     = new SessionGroupId(buffer);
		ident       = new NodeId(buffer);
		while(buffer.position() < buffer.limit())
			payload.add(payload(buffer));
	}

	private Payload payload(ByteBuffer buffer) throws IOException {
		String key  = string(buffer, 4);
		int    size = buffer.getInt(); 
		switch(key) {
		case "tmln": return new Timeline(buffer); 
		case "sess": return new SessionMembership(buffer);
		case "mep4": return new MeasurementEndpointV4(buffer);
		default:     return new Unknown(buffer, key, size);
		}
	}

	@Override
	public String toString() {
		return header + ", messageType:" + messageType + ", ttl:" + ttl + ", groupId;" + groupId + ", ident:'" + ident + "', payload:" + payload; 
	}

	private static String string(ByteBuffer buffer, int count) {
		StringBuilder result = new StringBuilder();
		for(int i = 0; i < count; i++)
			result.append((char)buffer.get());
		return result.toString();
	}

	public enum MessageType {
		kInvalid,kAlive,kResponse,kByeBye;

		public static MessageType valueOf(ByteBuffer buffer) {
			return values()[buffer.get()];
		}
	}	

	public static class SessionGroupId {
		public final short id;
		public SessionGroupId(ByteBuffer buffer) {id = buffer.getShort();}
		@Override
		public String toString() {return Short.toString(id);}
	}

	public static class NodeId {
		public final String id;
		public NodeId(ByteBuffer buffer) {id = string(buffer, 8);}
		@Override
		public String toString() {return id;}
	}

	public static class Payload {
		@Override
		public String toString() {
			return TextUtilities.getShortClassName(this) + "{";
		}
	}

	public static class Timeline extends Payload {
		public final Tempo tempo;
		public final Beats beatOrigin;
		public final long  timeOrigin;

		public Timeline(ByteBuffer buffer) {
			tempo      = new Tempo(buffer);
			beatOrigin = new Beats(buffer);
			timeOrigin = buffer.getLong();
		}
		
		public long fromBeats(final Beats beats) {return timeOrigin + tempo.beatsToMicros(beats.sub(beatOrigin));}
		
		@Override
		public String toString() {return super.toString() + "tempo:" + tempo + ", beatOrigin:" + beatOrigin + ", timeOrigin:" + timeOrigin + "}";}
	}

	public static class Tempo {
		public long microsPerBeat;
		public Tempo(ByteBuffer buffer) {microsPerBeat = buffer.getLong();}
		
		public long beatsToMicros(final Beats beats) {return Math.round(beats.floating() * microsPerBeat);}
		@Override
		public String toString() {return "microsPerBeat:"+microsPerBeat;}
	}

	public static class Beats {
		public long microBeats;
		public Beats(ByteBuffer buffer) {microBeats = buffer.getLong();}
		public Beats(long microBeats)   {this.microBeats = microBeats;}
		public Beats(double beats)      {microBeats = Math.round(beats*IScheduler.SEC2US);}
		public Beats sub(Beats beat)    {return new Beats(microBeats-beat.microBeats);}
		public double floating()        {return microBeats / IScheduler.SEC2US;}
		
		@Override
		public String toString() {return "microBeats:"+microBeats;}
	}

	public static class SessionMembership extends Payload {
		public final String sessionId;
		
		public SessionMembership(ByteBuffer buffer) {
			sessionId = string(buffer, 8);
		}
		
		@Override
		public String toString() {return super.toString() + "sessionId:'" + sessionId +"'}";}
	}
	
	public static final class endpoint {
		public final InetSocketAddress addr;
		
		public endpoint(ByteBuffer buffer) throws UnknownHostException {
			byte[] addr = new byte[4];
			buffer.get(addr);
			this.addr = new InetSocketAddress(InetAddress.getByAddress(addr), buffer.getShort() & 0xFFFF);
		}
		
		@Override
		public String toString() {
			return addr.getHostName() + ":" + addr.getPort();
		}
	}
	
	public static class MeasurementEndpointV4 extends Payload {
		public endpoint ep;
		
		public MeasurementEndpointV4(ByteBuffer buffer) {
			try {
				ep = new endpoint(buffer);
			} catch (Throwable t) {
				log.warning(t);
			}
		}
		
		@Override
		public String toString() { return super.toString() + "ep:" + ep + "}";}
	}
	
	public static class Unknown extends Payload {
		public final String key;
		public final byte[] data;

		public Unknown(ByteBuffer buffer, String key, int size) {
			this.key  = key;
			this.data = new byte[size];
			buffer.get(this.data);
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder(key + "{ ");
			for(int i = 0; i < data.length; i++)
				result.append(TextUtilities.byteToHex(data[i])).append(':').append(Character.isLetterOrDigit(data[i]) ? (char)data[i] : '?').append(' ');
			return result.toString() + "}";
		}
	}
}
