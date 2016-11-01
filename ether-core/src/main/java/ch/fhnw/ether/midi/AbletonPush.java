package ch.fhnw.ether.midi;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.Parameter.Type;
import ch.fhnw.ether.media.Parametrizable;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec3;

public class AbletonPush implements IMidiHandler {
	private static final Log log = Log.create();

	private final MidiDevice[] liveDevs = new MidiDevice[2];
	private final MidiDevice[] userDevs = new MidiDevice[2];

	private static final String ABLETON_PUSH = "Ableton Push";
	private static final String LIVE_PORT    = ABLETON_PUSH + " Live Port";
	private static final String USER_PORT    = ABLETON_PUSH + " User Port";
	private static final String MIDIIN       = "MIDIIN2 ("+ABLETON_PUSH+")";
	private static final String MIDIOUT      = "MIDIOUT2 ("+ABLETON_PUSH+")";

	private static final int VELOCITY_THRESHOLD = 10;

	private final Thread MIDIProc;

	public AbletonPush(int deviceIdx) throws MidiUnavailableException, IOException, InvalidMidiDataException {
		MidiIO.init();
		int liveCount = 0;
		int userCount = 0;
		for(MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
			if(ABLETON_PUSH.equals(info.getName())) {
				if(liveCount / 2 == deviceIdx)
					liveDevs[liveCount % 2] = MidiSystem.getMidiDevice(info);
				liveCount++;
			} else if(LIVE_PORT.equals(info.getDescription())) {
				if(liveCount / 2 == deviceIdx)
					liveDevs[liveCount % 2] = MidiSystem.getMidiDevice(info);
				liveCount++;
			}
			else if(USER_PORT.equals(info.getDescription())) {
				if(userCount / 2 == deviceIdx)
					userDevs[userCount % 2] = MidiSystem.getMidiDevice(info);
				userCount++;
			} else if(MIDIIN.equals(info.getName())) {
				if(userCount / 2 == deviceIdx)
					userDevs[0] = MidiSystem.getMidiDevice(info);
				userCount++;
			} else if(MIDIOUT.equals(info.getName())) {
				if(userCount / 2 == deviceIdx)
					userDevs[1] = MidiSystem.getMidiDevice(info);
				userCount++;
			}
		}

		for(MidiDevice dev : liveDevs)
			if(dev == null)
				throw new MidiUnavailableException("No Ableton Push found for index " + deviceIdx);

		for(MidiDevice dev : userDevs)
			if(dev == null)
				throw new MidiUnavailableException("No Ableton Push found for index " + deviceIdx);

		MidiIO.setHandler(userDevs[0], this);

		setBrightness(1f);

		for(int l = 0; l < 4; l++)
			clearLine(l);

		for(PControl c : PControl.values())
			if(!(c.name().startsWith("P_")))
				setColor(c, RGB.BLACK);

		MIDIProc = new Thread(this::processMIDI);
		MIDIProc.start();
	}

	static SysexMessage sysex(int ... data) {
		try {
			byte[] msg = new byte[data.length];
			for(int i = 0; i < msg.length; i++)
				msg[i] = (byte) data[i];
			return new SysexMessage(msg, msg.length);
		} catch(Throwable t) {
			log.warning(t);
			return null;
		}
	}

	static ShortMessage shortm(int cmd, int data0, int data1) {
		try {
			return new ShortMessage(cmd, data0, data1);
		} catch(Throwable t) {
			log.warning(t);
			return null;
		}
	}

	public void setBrightness(float value) {
		send(sysex(SysexMessage.SYSTEM_EXCLUSIVE,71,127,21,124,0,1,MathUtilities.clamp((int)(value*127), 0, 127),SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE));
	}

	public void clearLine(int line) {
		send(sysex(SysexMessage.SYSTEM_EXCLUSIVE,71,127,21,28+line,0,0,SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE));
	}

	public enum TouchStrip {
		Host_Point,
		Host_Bar_Bottom,
		Host_Bar_Center,
		Host_Point_Center,
		Fixed_Point_Center2,
		Autoreturn_Twopoint_Center,
		Bar_bottom,
		Bar_center,
		Point,
	};

	public void setTouchStrip(TouchStrip conf) {
		send(sysex(SysexMessage.SYSTEM_EXCLUSIVE,71,127,21,99,0,1,conf.ordinal(),SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE));
	}

	public void setLine(int line, String text) {
		try {
			byte[] msg = new byte[77];
			Arrays.fill(msg, (byte)' ');
			int idx = 0;
			msg[idx++] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
			msg[idx++] = 71;
			msg[idx++] = 127;
			msg[idx++] = 21;
			msg[idx++] = (byte) (24+line);
			msg[idx++] = 0;
			msg[idx++] = 69;
			msg[idx++] = 0;
			for(int i = 0; i < Math.min(68,  text.length()); i++)
				msg[idx++] = (byte) (text.charAt(i) & 0x7F);
			msg[msg.length-1] = (byte) 247;

			send(new SysexMessage(msg, msg.length));
		} catch(Throwable t) {
			log.warning(t);
		}
	}

	public void setColor(PControl pad, RGB color) {
		if(pad.isKey())
			setColor(pad.x, pad.y, color);
		else if(pad.isClipRow())
			send(shortm(ShortMessage.CONTROL_CHANGE, pad.ordinal(), rgb2clipColor(color)));
		else if(pad != PControl.PITCH)
			send(shortm(ShortMessage.CONTROL_CHANGE, pad.ordinal(), Basic.valueOf(color).ordinal()));
	}

	public void setColor(PControl pad, Basic blink) {
		if(pad.isKey())
			setColor(pad.x, pad.y, blink.color);
		else if(pad != PControl.PITCH)
			send(shortm(ShortMessage.CONTROL_CHANGE, pad.ordinal(), blink.ordinal()));
	}

	public void setColor(PControl pad, BiLed color)  {
		if(pad.isKey())
			setColor(pad.x, pad.y, color.color);
		else if(pad != PControl.PITCH)
			send(shortm(ShortMessage.CONTROL_CHANGE, pad.ordinal(), color.ordinal()));
	}

	public void setColor(int x, int y, RGB color) {
		send(shortm(ShortMessage.NOTE_ON, y*8+36+x, rgb2color(color)));
	}

	private void send(MidiMessage msg) {
		try {
			if(msg == null) return;
			MidiIO.send(userDevs[1], msg);
		} catch(Throwable t) {
			log.warning(t);
		}
	}

	@Override
	public void handle(MidiMessage msg) {
		if(msg instanceof ShortMessage) {
			ShortMessage smsg = (ShortMessage)msg;
			switch(smsg.getCommand()) {
			case ShortMessage.CONTROL_CHANGE:
				int cc = smsg.getData1();
				if(cc2run[cc] != null) cc2run[cc].handle(msg);
				break;
			case ShortMessage.NOTE_ON:
				int key = smsg.getData1();
				if(key2run[key] != null) key2run[key].handle(msg);
				break;
			case ShortMessage.PITCH_BEND:
				pitchBend.set(smsg);
				synchronized (processMidi) {processMidi.notifyAll();}
				break;
			}
		}
		System.out.println(MidiToString.toString(msg));
	}

	private Object                        processMidi = new Object();
	private AtomicReference<ShortMessage> pitchBend   = new AtomicReference<>();
	void processMIDI() {
		for(;;) {
			synchronized (processMidi) {
				try {processMidi.wait(100);} catch (Throwable t) {log.warning(t);}
			}
			for(ShortMessage smsg = pitchBend.getAndSet(null); smsg != null && pitch.get() != null; smsg = pitchBend.getAndSet(null)) {
				IMidiHandler pitch = this.pitch.get();
				if(pitch != null) pitch.handle(smsg); 
			}
		}
	}

	private static Vec3 RGB24(int rgb24) {
		return new Vec3(((rgb24 >> 16) &0xFF) / 255f, ((rgb24 >> 8) &0xFF) / 255f, ((rgb24 >> 0) &0xFF) / 255f);
	}

	private static final Vec3[] CLIP_COLOR_TABLE = {
			RGB24(0),
			RGB24(15549221), RGB24(12411136), RGB24(11569920), RGB24(8754719), 
			RGB24(5480241),  RGB24(695438),   RGB24(31421),    RGB24(197631), 
			RGB24(3101346),  RGB24(6441901),  RGB24(8092539),  RGB24(3947580), 
			RGB24(16712965), RGB24(12565097), RGB24(10927616), RGB24(8046132), 
			RGB24(4047616),  RGB24(49071),    RGB24(1090798),  RGB24(5538020), 
			RGB24(8940772),  RGB24(10701741), RGB24(12008809), RGB24(9852725), 
			RGB24(16149507), RGB24(12581632), RGB24(8912743),  RGB24(1769263), 
			RGB24(2490280),  RGB24(6094824),  RGB24(1698303),  RGB24(9160191), 
			RGB24(9611263),  RGB24(12094975), RGB24(14183652), RGB24(16726484), 
			RGB24(16753961), RGB24(16773172), RGB24(14939139), RGB24(14402304), 
			RGB24(12492131), RGB24(9024637),  RGB24(8962746),  RGB24(10204100), 
			RGB24(8758722),  RGB24(13011836), RGB24(15810688), RGB24(16749734), 
			RGB24(16753524), RGB24(16772767), RGB24(13821080), RGB24(12243060), 
			RGB24(11119017), RGB24(13958625), RGB24(13496824), RGB24(12173795), 
			RGB24(13482980), RGB24(13684944), RGB24(14673637), RGB24(16777215)};

	private static final Vec3[] RGB_COLOR_TABLE = {
			RGB24(0x000000), RGB24(0x1E1E1E), RGB24(0x7F7F7F), RGB24(0xFFFFFF), RGB24(0xFF4C4C), RGB24(0xFF0000), RGB24(0x590000), RGB24(0x190000),
			RGB24(0xFFBD6C), RGB24(0xFF5400), RGB24(0x591D00), RGB24(0x271B00), RGB24(0xFFFF4C), RGB24(0xFFFF00), RGB24(0x595900), RGB24(0x191900),
			RGB24(0x88FF4C), RGB24(0x54FF00), RGB24(0x1D5900), RGB24(0x142B00), RGB24(0x4CFF4C), RGB24(0x00FF00), RGB24(0x005900), RGB24(0x001900),
			RGB24(0x4CFF5E), RGB24(0x00FF19), RGB24(0x00590D), RGB24(0x001902), RGB24(0x4CFF88), RGB24(0x00FF55), RGB24(0x00591D), RGB24(0x001F12),
			RGB24(0x4CFFB7), RGB24(0x00FF99), RGB24(0x005935), RGB24(0x001912), RGB24(0x4CC3FF), RGB24(0x00A9FF), RGB24(0x004152), RGB24(0x001019),
			RGB24(0x4C88FF), RGB24(0x0055FF), RGB24(0x001D59), RGB24(0x000819), RGB24(0x4C4CFF), RGB24(0x0000FF), RGB24(0x000059), RGB24(0x000019),
			RGB24(0x874CFF), RGB24(0x5400FF), RGB24(0x190064), RGB24(0x0F0030), RGB24(0xFF4CFF), RGB24(0xFF00FF), RGB24(0x590059), RGB24(0x190019),
			RGB24(0xFF4C87), RGB24(0xFF0054), RGB24(0x59001D), RGB24(0x220013), RGB24(0xFF1500), RGB24(0x993500), RGB24(0x795100), RGB24(0x436400),
			RGB24(0x033900), RGB24(0x005735), RGB24(0x00547F), RGB24(0x0000FF), RGB24(0x00454F), RGB24(0x2500CC), RGB24(0x7F7F7F), RGB24(0x202020),
			RGB24(0xFF0000), RGB24(0xBDFF2D), RGB24(0xAFED06), RGB24(0x64FF09), RGB24(0x108B00), RGB24(0x00FF87), RGB24(0x00A9FF), RGB24(0x002AFF),
			RGB24(0x3F00FF), RGB24(0x7A00FF), RGB24(0xB21A7D), RGB24(0x402100), RGB24(0xFF4A00), RGB24(0x88E106), RGB24(0x72FF15), RGB24(0x00FF00),
			RGB24(0x3BFF26), RGB24(0x59FF71), RGB24(0x38FFCC), RGB24(0x5B8AFF), RGB24(0x3151C6), RGB24(0x877FE9), RGB24(0xD31DFF), RGB24(0xFF005D),
			RGB24(0xFF7F00), RGB24(0xB9B000), RGB24(0x90FF00), RGB24(0x835D07), RGB24(0x392B00), RGB24(0x144C10), RGB24(0x0D5038), RGB24(0x15152A),
			RGB24(0x16205A), RGB24(0x693C1C), RGB24(0xA8000A), RGB24(0xDE513D), RGB24(0xD86A1C), RGB24(0xFFE126), RGB24(0x9EE12F), RGB24(0x67B50F),
			RGB24(0x1E1E30), RGB24(0xDCFF6B), RGB24(0x80FFBD), RGB24(0x9A99FF), RGB24(0x8E66FF), RGB24(0x404040), RGB24(0x757575), RGB24(0xE0FFFF),
			RGB24(0xA00000), RGB24(0x350000), RGB24(0x1AD000), RGB24(0x074200), RGB24(0xB9B000), RGB24(0x3F3100), RGB24(0xB35F00), RGB24(0x4B1502),
	};

	private static final Map<RGB, Integer> RGB2COLOR      = new HashMap<>();
	private static final Map<RGB, Integer> RGB2CLIP_COLOR = new HashMap<>();

	private static int rgb2color(RGB rgb) {
		Integer result = RGB2COLOR.get(rgb);
		Vec3  col     = rgb.toVec3();
		int   minIdx  = 0;
		float minDist = col.distance(RGB_COLOR_TABLE[minIdx]); 
		if(result == null) {
			for(int i = RGB_COLOR_TABLE.length; --i >= 0;) {
				float d = col.distance(RGB_COLOR_TABLE[i]); 
				if(d < minDist) {
					minDist = d;
					minIdx  = i;
				}
			}
			result = Integer.valueOf(minIdx);
			RGB2COLOR.put(rgb, result);
		}
		return result.intValue();
	}

	private static int rgb2clipColor(RGB rgb) {
		Integer result = RGB2CLIP_COLOR.get(rgb);
		int   minIdx  = 0;
		Vec3  col     = rgb.toVec3();
		float minDist = col.distance(CLIP_COLOR_TABLE[minIdx]); 
		if(result == null) {
			for(int i = CLIP_COLOR_TABLE.length; --i >= 0;) {
				float d = col.distance(CLIP_COLOR_TABLE[i]); 
				if(d < minDist) {
					minDist = d;
					minIdx  = i;
				}
			}
			result = Integer.valueOf(minIdx == 0 ? 0 : minIdx + 59);
			RGB2CLIP_COLOR.put(rgb, result);
		}
		return result.intValue();
	}

	public enum Basic {
		OFF(RGB.BLACK),
		HALF(RGB.GRAY),
		HALF_BLINK_SLOW(RGB.GRAY),
		HALF_BLINK_FAST(RGB.GRAY),
		FULL(RGB.WHITE),
		FULL_BLINK(RGB.WHITE),
		FULL_BLINK_FAST(RGB.WHITE);

		final RGB color;

		Basic(RGB color) {this.color = color;}

		static Basic valueOf(RGB rgb) {
			float value = rgb.r + rgb.g + rgb.b;
			if(value < 1)      return OFF;
			else if(value < 2) return HALF;
			else               return FULL;
		}
	}

	public enum BiLed {
		OFF(RGB.BLACK),//0
		RED_HALF(RGB.RED.scaleRGB(0.5f)),//1
		RED_HALF_BLINK_SLOW(RGB.RED.scaleRGB(0.5f)),//2
		RED_HALF_BLINK_FAST(RGB.RED.scaleRGB(0.5f)),//3
		RED(RGB.RED),//4
		RED_BLINK_SLOW(RGB.RED),//5
		RED_BLINK_FAST(RGB.RED),//6
		AMBER_HALF(RGB.ORANGE.scaleRGB(0.5f)),//7
		AMBER_HALF_BLINK_SLOW(RGB.ORANGE.scaleRGB(0.5f)),//8
		AMBER_HALF_BLINK_FAST(RGB.ORANGE.scaleRGB(0.5f)),//9
		AMBER(RGB.ORANGE),//10
		AMBER_BLINK_SLOW(RGB.ORANGE),//11
		AMBER_BLINK_FAST(RGB.ORANGE),//12
		YELLOW_HALF(RGB.YELLOW.scaleRGB(0.5f)),//13
		YELLOW_HALF_BLINK_SLOW(RGB.YELLOW.scaleRGB(0.5f)),//14
		YELLOW_HALF_BLINK_FAST(RGB.YELLOW.scaleRGB(0.5f)),//15
		YELLOW(RGB.YELLOW),//16
		YELLOW_BLINK_SLOW(RGB.YELLOW),//17
		YELLOW_BLINK_FAST(RGB.YELLOW),//18
		GREEN_HALF(RGB.GREEN.scaleRGB(0.5f)),//19
		GREEN_HALF_BLINK_SLOW(RGB.GREEN.scaleRGB(0.5f)),//20
		GREEN_HALF_BLINK_FAST(RGB.GREEN.scaleRGB(0.5f)),//21
		GREEN(RGB.GREEN),//22
		GREEN_BLINK_SLOW(RGB.GREEN),//23
		GREEN_BLINK_FAST(RGB.GREEN);//24

		public final RGB color;

		BiLed(RGB color) {this.color = color;}

	}

	public enum PControlType {
		BUTTON,
		KEY,
		KNOB_RELATIVE,
		KNOB_RELATIVE_DISCRETE,
		PITCH_WHEEL,
		CLIP_ROW,
	}

	public enum PControl {
		P_00,
		P_01,
		P_02,
		TAP,
		P_04,
		P_05,
		P_06,
		P_07,
		P_08,
		METRONOME,
		P_0A,
		P_0B,
		P_0C,
		P_0D,
		TEMPO(PControlType.KNOB_RELATIVE_DISCRETE),
		MONITOR(PControlType.KNOB_RELATIVE),

		P_10,
		P_11,
		P_12,
		P_13,
		ROW0_0,
		ROW0_1,
		ROW0_2,
		ROW0_3,
		ROW0_4,
		ROW0_5,
		ROW0_6,
		ROW0_7,
		MASTER,
		STEP,
		P_1E,
		P_1F,

		P_20,
		P_21,
		P_22,
		P_23,
		BEAT_4,
		BEAT_4t,
		BEAT_8,
		BEAT_8t,
		BEAT_16,
		BEAT_16t,
		BEAT_32,
		BEAT_32t,
		LEFT,
		RIGHT,
		UP,
		DOWN,

		SELECT,
		SHIFT,
		NOTE,
		SESSION,
		ADD_EFFECT,
		ADD_TRACK,
		OCTAVE_DOWN,
		OCTAVE_UP,
		REPEAT,
		ACCENT,
		SCALES,
		USER,
		MUTE,
		SOLO,
		ENTER,
		BACK,

		P_40,
		P_41,
		P_42,
		P_43,
		P_44,
		P_45,
		P_46,
		KNOB_0(PControlType.KNOB_RELATIVE),
		KNOB_1(PControlType.KNOB_RELATIVE),
		KNOB_2(PControlType.KNOB_RELATIVE),
		KNOB_3(PControlType.KNOB_RELATIVE),
		KNOB_4(PControlType.KNOB_RELATIVE),
		KNOB_5(PControlType.KNOB_RELATIVE),
		KNOB_6(PControlType.KNOB_RELATIVE),
		KNOB_7(PControlType.KNOB_RELATIVE),
		KNOB_MASTER(PControlType.KNOB_RELATIVE),

		P_50,
		P_51,
		P_52,
		P_53,
		P_54,
		PLAY,
		RECORD,
		NEW,
		DUPLICATE,
		AUTOMATION,
		FIXED_LENGTH,
		P_5B,
		P_5C,
		P_5D,
		P_5E,
		P_5F,

		P_60,
		P_61,
		P_62,
		P_63,
		P_64,
		P_65,
		ROW1_0(PControlType.CLIP_ROW),
		ROW1_1(PControlType.CLIP_ROW),
		ROW1_2(PControlType.CLIP_ROW),
		ROW1_3(PControlType.CLIP_ROW),
		ROW1_4(PControlType.CLIP_ROW),
		ROW1_5(PControlType.CLIP_ROW),
		ROW1_6(PControlType.CLIP_ROW),
		ROW1_7(PControlType.CLIP_ROW),
		DEVICE,
		BROWSE,

		TRACK,
		CLIP,
		VOLUME,
		PAN_SEND,
		QUANTIZE,
		DOUBLE,
		DELETE,
		UNDO,
		P_78,
		P_79,
		P_7A,
		P_7B,
		P_7C,
		P_7D,
		P_7E,
		P_7F,

		PITCH(PControlType.PITCH_WHEEL),

		PAD_0_0(0,0),PAD_1_0(1,0),PAD_2_0(2,0),PAD_3_0(3,0),PAD_4_0(4,0),PAD_5_0(5,0),PAD_6_0(6,0),PAD_7_0(7,0),
		PAD_0_1(0,1),PAD_1_1(1,1),PAD_2_1(2,1),PAD_3_1(3,1),PAD_4_1(4,1),PAD_5_1(5,1),PAD_6_1(6,1),PAD_7_1(7,1),
		PAD_0_2(0,2),PAD_1_2(1,2),PAD_2_2(2,2),PAD_3_2(3,2),PAD_4_2(4,2),PAD_5_2(5,2),PAD_6_2(6,2),PAD_7_2(7,2),
		PAD_0_3(0,3),PAD_1_3(1,3),PAD_2_3(2,3),PAD_3_3(3,3),PAD_4_3(4,3),PAD_5_3(5,3),PAD_6_3(6,3),PAD_7_3(7,3),
		PAD_0_4(0,4),PAD_1_4(1,4),PAD_2_4(2,4),PAD_3_4(3,4),PAD_4_4(4,4),PAD_5_4(5,4),PAD_6_4(6,4),PAD_7_4(7,4),
		PAD_0_5(0,5),PAD_1_5(1,5),PAD_2_5(2,5),PAD_3_5(3,5),PAD_4_5(4,5),PAD_5_5(5,5),PAD_6_5(6,5),PAD_7_5(7,5),
		PAD_0_6(0,6),PAD_1_6(1,6),PAD_2_6(2,6),PAD_3_6(3,6),PAD_4_6(4,6),PAD_5_6(5,6),PAD_6_6(6,6),PAD_7_6(7,6),
		PAD_0_7(0,7),PAD_1_7(1,7),PAD_2_7(2,7),PAD_3_7(3,7),PAD_4_7(4,7),PAD_5_7(5,7),PAD_6_7(6,7),PAD_7_7(7,7),

		TOUCH_KNOB_0(0x00),
		TOUCH_KNOB_1(0x01),
		TOUCH_KNOB_2(0x02),
		TOUCH_KNOB_3(0x03),
		TOUCH_KNOB_4(0x04),
		TOUCH_KNOB_5(0x05),
		TOUCH_KNOB_6(0x06),
		TOUCH_KNOB_7(0x07),
		TOUCH_MASTER(0x08),
		TOUCH_MONITOR(0x09),
		TOUCH_TEMPO(0x0A),

		;

		final PControlType type;
		final int          x;
		final int          y;
		PControl() {this(PControlType.BUTTON);}
		PControl(PControlType type) {
			this.type     = type;
			this.x        = -1;
			this.y        = -1;
		}
		PControl(int x, int y) {
			this.type     = PControlType.KEY;
			this.x        = x;
			this.y        = y;
		}
		PControl(int key) {
			this.type     = PControlType.KEY;
			this.x        = key-36;
			this.y        = 0;
		}
		public boolean isKnob()     {return type == PControlType.KNOB_RELATIVE || type == PControlType.KNOB_RELATIVE_DISCRETE;}
		public boolean isDiscrete() {return type == PControlType.BUTTON || type == PControlType.KNOB_RELATIVE_DISCRETE;}
		public boolean isKey()      {return type == PControlType.KEY;}
		public boolean isClipRow()  {return type == PControlType.CLIP_ROW;}
		public int     key()        {return y*8+36+x;}
		public static PControl valueOf(int x, int y) {return values()[PControl.PAD_0_0.ordinal()+(y*8+x)];}
	}

	private IMidiHandler[] cc2run               = new IMidiHandler[128];
	private IMidiHandler[] key2run              = new IMidiHandler[128];
	private AtomicReference<IMidiHandler> pitch = new AtomicReference<>();

	public IMidiHandler set(PControl pad, Parametrizable cmd, Parameter p) {
		return set(pad, cmd, p, null);
	}

	private static final int PITCH_ZERO_ZONE = 700;
	public IMidiHandler set(PControl pad, Parametrizable cmd, Parameter p, Float constVal) {
		setColor(pad, Basic.FULL);
		return set(pad, msg->{
			if(msg instanceof ShortMessage) {
				ShortMessage smsg = (ShortMessage)msg;
				if(smsg.getCommand() == ShortMessage.PITCH_BEND) {
					int   val14 = MidiIO.toInt14(smsg.getData1(), smsg.getData2()) - PITCH_ZERO_ZONE;
					if(val14 < 0) val14 = 0;
					float val   = val14 / (MidiIO.MAX_14BIT -PITCH_ZERO_ZONE);
					cmd.setVal(p, ((cmd.getMax(p) - cmd.getMin(p)) * val) + cmd.getMin(p));
				} else {
					switch(pad) {
					case ROW1_0:   pattern(cmd, p, smsg.getData2(), 0x80); break;
					case ROW1_1:   pattern(cmd, p, smsg.getData2(), 0x40); break;
					case ROW1_2:   pattern(cmd, p, smsg.getData2(), 0x20); break;
					case ROW1_3:   pattern(cmd, p, smsg.getData2(), 0x10); break;
					case ROW1_4:   pattern(cmd, p, smsg.getData2(), 0x08); break;
					case ROW1_5:   pattern(cmd, p, smsg.getData2(), 0x04); break;
					case ROW1_6:   pattern(cmd, p, smsg.getData2(), 0x02); break;
					case ROW1_7:   pattern(cmd, p, smsg.getData2(), 0x01); break;

					default:
						int cc  = smsg.getData1();
						int val = smsg.getData2();
						if(constVal != null && val > VELOCITY_THRESHOLD) {
							cmd.setVal(p, constVal.floatValue());
						} else {
							if(p.getType() == Type.BOOL && val > VELOCITY_THRESHOLD)
								cmd.setVal(p, cmd.getVal(p) == 0 ? cmd.getMax(p) : cmd.getMin(p));
							else if(PControl.values()[cc].isKnob()) {
								if(PControl.values()[cc].isDiscrete()) {
									cmd.setVal(p, Math.round(cmd.getVal(p)));
									cmd.incVal(p, val < 64 ? val : val-128, 1);
								} else
									cmd.incVal(p, val < 64 ? val : val-128);
							}
						}
						break;
					}
				}
			}
		});
	}

	private void pattern(Parametrizable cmd, Parameter p, int val, int mask) {
		if(val > VELOCITY_THRESHOLD) cmd.setVal(p, ((int) cmd.getVal(p)) ^ mask);
	}

	public IMidiHandler set(PControl pad, IMidiHandler r) {
		return set(pad, r, false);
	}

	public IMidiHandler set(PControl pad, IMidiHandler r, boolean remove) {
		if(pad.type == PControlType.PITCH_WHEEL) {
			pitch.set(remove ? null : r);
		} else if(pad.isKey()) {
			if(remove && key2run[pad.key()] != r) return key2run[pad.key()]; 
			key2run[pad.key()] = r;
			setColor(pad, (r == null || remove) ? Basic.OFF : Basic.FULL);
		} else {
			if(remove && cc2run[pad.ordinal()] != r) return cc2run[pad.ordinal()]; 
			cc2run[pad.ordinal()] = r;
			setColor(pad, (r == null || remove) ? Basic.OFF : Basic.FULL);
		}
		return r;
	}

	public IMidiHandler set(PControl pad, Runnable r) {
		return set(pad,  msg->{if(msg.getMessage()[2] > VELOCITY_THRESHOLD) r.run();});
	}

	public void remove(PControl pad) {
		remove(pad, null);
	}

	public void remove(PControl pad, IMidiHandler handler) {
		set(pad, handler, true);
	}

	public String sliderText(float v) {
		StringBuilder result = new StringBuilder(TextUtilities.repeat((char)6, 8));
		int i = (int) (v * 15);
		if(i < 16) result.setCharAt(i/2, (char)(i % 2 == 0 ? 3 : 4));
		return result.toString();
	}

	public void setTouchStrip(float value) {
		send(shortm(ShortMessage.PITCH_BEND, 0, (int) (value * 127)));
	}

	private static final PControl[] BEATS   = {PControl.BEAT_4,  PControl.BEAT_8,  PControl.BEAT_16,  PControl.BEAT_32};
	private static final PControl[] BEATS_T = {PControl.BEAT_4t, PControl.BEAT_8t, PControl.BEAT_16t, PControl.BEAT_32t};

	public void setBeat(float beatValue, boolean t) {
		PControl[] beats = t ? BEATS_T : BEATS;
		if(beatValue < 1/64f) {
			for(int i = 0; i < 4; i++) 
				setColor(beats[i], Basic.OFF);
		} else {
			float beat = 1f/4f;
			for(int i = 0; i < 4; i++, beat /= 2f) {
				if(Math.abs(beat-beatValue) < beat/4f) setColor(beats[i], BiLed.GREEN);
				else								   setColor(beats[i], BiLed.AMBER_HALF);
			}
		}
	}


	public void setRow0(BiLed c0, BiLed c1, BiLed c2, BiLed c3, BiLed c4, BiLed c5, BiLed c6, BiLed c7) {
		setColor(PControl.ROW0_0, c0);
		setColor(PControl.ROW0_1, c1);
		setColor(PControl.ROW0_2, c2);
		setColor(PControl.ROW0_3, c3);
		setColor(PControl.ROW0_4, c4);
		setColor(PControl.ROW0_5, c5);
		setColor(PControl.ROW0_6, c6);
		setColor(PControl.ROW0_7, c7);
	}

	public void setRow1(RGB c0, RGB c1, RGB c2, RGB c3, RGB c4, RGB c5, RGB c6, RGB c7) {
		setColor(PControl.ROW1_0, c0);
		setColor(PControl.ROW1_1, c1);
		setColor(PControl.ROW1_2, c2);
		setColor(PControl.ROW1_3, c3);
		setColor(PControl.ROW1_4, c4);
		setColor(PControl.ROW1_5, c5);
		setColor(PControl.ROW1_6, c6);
		setColor(PControl.ROW1_7, c7);
	}
}
