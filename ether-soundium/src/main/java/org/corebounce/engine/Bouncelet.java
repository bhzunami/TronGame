package org.corebounce.engine;

import java.text.NumberFormat;
import java.util.Arrays;

import org.corebounce.audio.IBeatListener;
import org.corebounce.io.MIDI;
import org.corebounce.io.OSC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.Parametrizable;
import ch.fhnw.ether.midi.AbletonPush;
import ch.fhnw.ether.midi.IMidiHandler;
import ch.fhnw.ether.midi.AbletonPush.BiLed;
import ch.fhnw.ether.midi.AbletonPush.PControl;
import ch.fhnw.util.ArrayUtilities;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.color.RGB;

public class Bouncelet implements IDisposable, IBeatListener {
	// keep in sync with Decklight.cs
	public enum PatternStyle {TOGGLE, FLASH, LINEAR, SMOOTH}

	public static  final String ACTIVE        = "active";
	public static  final String BPM_DOWNER    = "BPM_downer";
	public static  final String PATTERN       = "pattern";
	public static  final String PATTERN_STYLE = "patternStyle";
	private static final String INSPECTOR     = "Inspector";

	private static final Parametrizable NO_PARAMS = new Parametrizable(INSPECTOR);

	private static final PControl[] CTRLS = {
			PControl.KNOB_0,	
			PControl.KNOB_1,	
			PControl.KNOB_2,	
			PControl.KNOB_3,	
			PControl.KNOB_4,	
			PControl.KNOB_5,	
			PControl.KNOB_6,	
			PControl.KNOB_7,	
	};

	private static  Bouncelet        lastOnPush;
	public  final   int              id; 
	private final   String           type; 
	private         String           label; 
	private         RGB              color = RGB.BLACK;
	protected       long             lastUpdate;
	private         Object[]         names;
	private         Object[]         mins;
	private         Object[]         maxs;
	private         Object[]         vals;
	private         Parametrizable   params = new Parametrizable(INSPECTOR, new Parameter(ACTIVE, ACTIVE, 0, 1, 1));
	protected final IBounceletUpdate update;
	protected final Engine           engine;
	protected final OSC              osc;
	private   final AbletonPush      push;
	private         Image            ctrlImage;
	private         float            lastActive;
	private   final IMidiHandler     padHandler;

	public Bouncelet(Engine engine, OSC osc, MIDI midi, int id, String type, String label, float active, RGB color, IBounceletUpdate updateOnUi) {
		this.id         = id;
		this.type       = type;
		this.label      = label;
		this.lastUpdate = System.currentTimeMillis();
		this.update     = updateOnUi;
		this.engine     = engine;
		this.osc        = osc;
		this.push       = midi.getPush();
		if(id >= 0 && push != null)
			padHandler = push.set(pControl(), ()->{Display.getDefault().asyncExec(()->engine.select(this, false));});
		else 
			padHandler = null;
		setActive(active);
		setColor(color);
	}

	public boolean update(String label, float active, RGB color) {
		boolean changed = 
				!(this.label.equals(label)) ||
				!(this.color.equals(color)) ||
				Math.abs(lastActive - active) > 0.05;

				this.label      = label;
				setActive(active);
				setColor(color);
				this.lastUpdate = System.currentTimeMillis();
				this.lastActive = active;
				return changed;
	}

	private void setColor(RGB color) {
		if(!(this.color.equals(color)) && ctrlImage != null) {
			ctrlImage.dispose();
			ctrlImage = null;
		}
		this.color = color;
		if(id >= 0 && push != null)
			push.setColor(pControl(), color);
	}

	public String getType() {
		return type;
	}

	public boolean olderThan(long time) {
		return lastUpdate <= time;
	}

	public String getLabel() {
		return label;
	}

	public RGB getColor() {
		return color;
	}

	private static final PaletteData RGB_PALETTE = new PaletteData(0xFF0000, 0x00FF00, 0x0000FF);
	public Image getControllerImage(Display display) {
		if(ctrlImage == null) {
			ImageData img = new ImageData(48, 48, 32, RGB_PALETTE);
			for(int y = 0; y < 40; y++)
				for(int x = 0; x < 48; x++) {
					int col = x / 6;
					int row = y / 5;
					int id  = (row * 8) + col; 
					if(x % 6 < 5 && y % 5 < 4) {
						img.setAlpha(x, y+4, 255);
						if(id == this.id)	img.setPixel(x, y+4, color.toARGB32());
						else				img.setPixel(x, y+4, 0x444444);
					} else img.setAlpha(x, y+4, 0);
				}
			ctrlImage = new Image(display, img);
		}
		return ctrlImage;
	}

	public Parametrizable getParameters() {
		return params;
	}

	public int getId() {
		return id;
	}

	public void setNames(Object ... args) {names = args; updateParams();}
	public void setMins(Object ... args)  {mins  = args; updateParams();}
	public void setMaxs(Object ... args)  {maxs  = args; updateParams();}
	public void setVals(Object ... args)  {vals  = args; updateParams();}

	private static final NumberFormat FMT = TextUtilities.decimalFormat(2);
	private void updateParams() {
		if(names != null && mins != null && maxs != null && vals != null) {
			if(params == null || params.getParameters().length < 4) {
				Parameter[] ps = new Parameter[names.length];
				for(int i = 0; i < names.length; i++) {
					String name = names[i].toString();
					if(BPM_DOWNER.equals(name)) {
						int count = (int)(toFloat(maxs[i]) - toFloat(mins[i]));
						float[] dVals    = new float[count];
						String[] dLabels = new String[count];
						int val = (int)toFloat(mins[i]);
						for(int v = 0; v < count; v++, val++) {
							if(val == 0) val++;
							dVals[v]   = val;
							dLabels[v] = Integer.toString(val);
						}
						ps[i] = new Parameter(name, format(name), (int)toFloat(vals[i]), dVals, dLabels);
					} else if(PATTERN_STYLE.equals(name)) {
						ps[i] = new Parameter(name, format(name), (int)toFloat(vals[i]), PatternStyle.class);
					} else if(PATTERN.equals(name)) {
						ps[i] = new Parameter(name, format(name), (int)toFloat(vals[i]), Parameter.BITMAP8);
					} else
						ps[i] = new Parameter(name, format(name), toFloat(mins[i]), toFloat(maxs[i]), toFloat(vals[i]));
				}

				ArrayUtilities.reverseArrayRange(ps, 0, ps.length);

				params = createParams(ps);
			} else {
				for(int i = 0; i < names.length; i++)
					params.setVal(names[i].toString(), ((Number)vals[i]).floatValue());
			}

			names = null;
			mins  = null;
			maxs  = null;
			vals  = null;
			Display.getDefault().asyncExec(()->{
				update.update(this);
				setPush();
			});
		}		
	}

	protected Parametrizable createParams(Parameter[] ps) {
		return new Parametrizable(INSPECTOR, ps) {
			@Override
			public void setVal(Parameter p, float val) {
				lastUpdate = System.currentTimeMillis();
				super.setVal(p, val);
				updateDecklight(p, val);
				updatePush();
			}
		};
	}

	protected void updateDecklight(Parameter p, float val) {
		osc.send("/" + Engine.BOUNCELET + "/" + id + "/" + p.getName(), val);
	}

	protected void updateDecklight(String endpoint) {
		osc.send("/" + Engine.BOUNCELET + "/" + id + "/" + endpoint);
	}

	private void setPush() {
		lastOnPush = this;
		if(push != null) {
			int idx = 0;
			Parametrizable p      = getParameters();
			StringBuilder labels  = new StringBuilder();
			for(Parameter param : p.getParameters()) {
				String name = param.getName();
				if(ACTIVE.equals(name)) {
					push.set(PControl.PITCH, p, param);
				} else if(BPM_DOWNER.equals(name)) {
					push.set(PControl.BEAT_4,   p, param,  1f);
					push.set(PControl.BEAT_4t,  p, param, -1f);
					push.set(PControl.BEAT_8,   p, param,  2f);
					push.set(PControl.BEAT_8t,  p, param, -2f);
					push.set(PControl.BEAT_16,  p, param,  4f);
					push.set(PControl.BEAT_16t, p, param, -4f);
					push.set(PControl.BEAT_32,  p, param,  8f);
					push.set(PControl.BEAT_32t, p, param, -8f);
				} else if(PATTERN.equals(name)) {
					push.set(PControl.ROW1_0, p, param);
					push.set(PControl.ROW1_1, p, param);
					push.set(PControl.ROW1_2, p, param);
					push.set(PControl.ROW1_3, p, param);
					push.set(PControl.ROW1_4, p, param);
					push.set(PControl.ROW1_5, p, param);
					push.set(PControl.ROW1_6, p, param);
					push.set(PControl.ROW1_7, p, param);
					updatePattern();
				} else if(PATTERN_STYLE.equals(name)) {
					push.set(PControl.ROW0_4, p, param, 0f);
					push.set(PControl.ROW0_5, p, param, 1f);
					push.set(PControl.ROW0_6, p, param, 2f);
					push.set(PControl.ROW0_7, p, param, 3f);
				} else if(idx < CTRLS.length) {
					push.set(CTRLS[idx], p, param);
					labels.append(trunc(param.getDescription(), 8));
					if((idx % 2) == 0) labels.append(' ');
					idx++;
				}
			}
			push.setLine(0, labels.toString());

			push.setLine(3, center(getType(), 17)
					.append((char)2)
					.append(center(getLabel().toUpperCase(), 15)
							.append((char)2)
							.toString()) +  "[Toggle] [Flash ][Linear] [Smooth]");
			push.set(PControl.UNDO, this::reset);

			updatePush();
		}
	}

	protected void updatePush() {
		if(push != null) {
			int idx = 0;
			Parametrizable p = getParameters();
			StringBuilder values  = new StringBuilder();
			StringBuilder sliders = new StringBuilder();
			for(Parameter param : p.getParameters()) {
				float  absVal     = p.getVal(param);
				float  normalized = (absVal - p.getMin(param)) / (p.getMax(param)-p.getMin(param));
				String name       = param.getName();
				if(ACTIVE.equals(name)) {
					push.setTouchStrip(normalized);
				} else if(BPM_DOWNER.equals(name)) {
					float val = 0.25f / absVal;
					if(val < 0) {
						push.setBeat(-val, true);
						push.setBeat(0,    false);
					} else {
						push.setBeat(0,   true);
						push.setBeat(val, false);
					}
				} else if(PATTERN.equals(name)) {
					updatePattern();
				} else if(PATTERN_STYLE.equals(name)) {
					push.setColor(PControl.ROW0_4, BiLed.AMBER);
					push.setColor(PControl.ROW0_5, BiLed.AMBER);
					push.setColor(PControl.ROW0_6, BiLed.AMBER);
					push.setColor(PControl.ROW0_7, BiLed.AMBER);
					switch((int)absVal) {
					case 0: push.setColor(PControl.ROW0_4, BiLed.GREEN); break;
					case 1: push.setColor(PControl.ROW0_5, BiLed.GREEN); break;
					case 2: push.setColor(PControl.ROW0_6, BiLed.GREEN); break;
					case 3: push.setColor(PControl.ROW0_7, BiLed.GREEN); break;
					}
				} else if(idx < CTRLS.length) {
					values.append(trunc(FMT.format(absVal), 8));
					sliders.append(push.sliderText(normalized));
					if((idx % 2) == 0) {
						values.append(' ');
						sliders.append(' ');
					}
					idx++;
				}
			}
			push.setLine(1, values.toString());
			push.setLine(2, sliders.toString());
		}
	}

	private static final RGB TRIGGER_BEAT    = RGB.BLUE;
	private static final RGB TRIGGER         = RGB.BLUE.scaleRGB(0.5f);
	private static final RGB NO_TRIGGER_BEAT = RGB.GRAY50;
	private static final RGB NO_TRIGGER      = RGB.GRAY20;
	private RGB[] patCols = new RGB[8];
	int           beatNo;
	@Override
	public void beat(int beatNo) {
		this.beatNo = beatNo;
		updatePattern();
	}

	private void updatePattern() {
		if(push != null) {
			Parameter p = params.getParameter(PATTERN);
			if(p != null) {
				int pattern = (int)params.getVal(p);
				for(int i = 0; i < patCols.length; i++) {
					if((pattern & (1 << (patCols.length - 1 - i))) != 0) {
						patCols[i] = beatNo % patCols.length == i ? TRIGGER_BEAT : TRIGGER;
					} else
						patCols[i] = beatNo % patCols.length == i ? NO_TRIGGER_BEAT : NO_TRIGGER;
				}
			} else 
				Arrays.fill(patCols, RGB.BLACK);
			push.setRow1(patCols[0], patCols[1], patCols[2], patCols[3], patCols[4], patCols[5], patCols[6], patCols[7]);
		}
	}

	private float toFloat(Object object) {
		return ((Number)object).floatValue();
	}

	private String format(String string) {
		if(string.length() == 0) return string;

		StringBuilder result = new StringBuilder();
		result.append(Character.toUpperCase(string.charAt(0)));
		int charCount = 0;
		for(int i = 1; i < string.length(); i++) {
			if(string.charAt(i) == '_') {
				result.append(' ');
				continue;
			}
			result.append(string.charAt(i));
			charCount++;
			if(i < string.length()-1) {
				if(charCount > 1) {
					if(Character.isUpperCase(string.charAt(i)) != Character.isUpperCase(string.charAt(i+1))) {
						result.append(' ');
						charCount = 0;
					}
					if(Character.isLetter(string.charAt(i)) != Character.isLetter(string.charAt(i+1))) {
						result.append(' ');
						charCount = 0;
					}
				}
			}
		}

		return result.length() > 8 ? string : result.toString();
	}

	private static StringBuilder trunc(String str, int len) {
		StringBuilder result = new StringBuilder(TextUtilities.repeat(' ', len));
		for(int i = 0; i  < Math.min(str.length(), len); i++)
			result.setCharAt(i, str.charAt(i));
		return result;
	}

	private static StringBuilder center(String str, int len) {
		StringBuilder result = new StringBuilder(TextUtilities.repeat(' ', len));
		int count = Math.min(str.length(), len);
		int off   = (len - count) / 2;
		for(int i = 0; i  < count; i++)
			result.setCharAt(i+off, str.charAt(i));
		return result;
	}

	@Override
	public String toString() {
		return "["+type+"]" + label + " " + getActive();
	}

	private PControl pControl() {
		return PControl.valueOf(id % 8, 7 - (id / 8));
	}

	@Override
	public void dispose() {
		setColor(RGB.BLACK);
		if(id >= 0 && push != null)
			push.remove(pControl(), padHandler);
		lastUpdate = 0;
		params     = NO_PARAMS;
		update.clear(this);
		clearPush();
	}

	private void clearPush() {
		if(push != null && lastOnPush == this) {
			lastOnPush = null;
			clearPush(push);
		}
	}

	public static void clearPush(AbletonPush push) {
		for(int i = 0; i < 4; i++) push.clearLine(i);
		push.setTouchStrip(0);
		push.setBeat(0, false);
		push.setBeat(0, true);
		push.setRow0(BiLed.OFF, BiLed.OFF, BiLed.OFF, BiLed.OFF, BiLed.OFF, BiLed.OFF, BiLed.OFF, BiLed.OFF);
		push.setRow1(RGB.BLACK, RGB.BLACK, RGB.BLACK, RGB.BLACK, RGB.BLACK, RGB.BLACK, RGB.BLACK, RGB.BLACK);
		for(PControl c : CTRLS)
			push.remove(c);
	}

	private void setActive(float active) {
		params.setVal(ACTIVE, active);
	}

	public float getActive() {
		return params.getVal(ACTIVE);
	}

	public boolean isDisposed() {
		return params == NO_PARAMS;
	}

	public void reset() {
		updateDecklight("__reset__");
	}
}
