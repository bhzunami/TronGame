package org.corebounce.resman;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.BeatDetect;
import ch.fhnw.ether.audio.fx.BeatDetect.BeatType;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.ui.ParameterWindow;
import ch.fhnw.util.Log;
import ch.fhnw.util.math.MathUtilities;

public class AudioPanel implements PaintListener, ControlListener {
	private static final Log log = Log.create();
	
	private final Audio                      audio;
	private 	  AtomicReference<ImageData> buffer = new AtomicReference<>(new ImageData(1, 100, 24, new PaletteData(0xFF0000, 0x00FF00, 0x0000FF)));
	private       Canvas                     canvasUI;
	private       Label                      bpmUI;
	private       int                        x;
	private       int                        lastCount;
	private       int                        lastCountPLL;
	private       Color                      FLASH;
	private       Color                      COL_BEAT;

	public AudioPanel(Audio audio) {
		this.audio = audio;

		audio.addLast(new AbstractRenderCommand<IAudioRenderTarget>() {
			boolean      trigger = true;
			final byte[] beatColor     = new byte[3];
			@Override
			protected void run(IAudioRenderTarget target) throws RenderCommandException {
				ImageData  buffer = AudioPanel.this.buffer.get();
				BeatDetect beat   = audio.getBeat();
				for(int loopcount = Math.max(1,  target.getFrame().getMonoSamples().length / 128); --loopcount >= 0;) {

					if(beat.beatCountPLL() != lastCountPLL && trigger) {
						trigger = false;
						x       = 5;
					}

					if(x >= buffer.width || trigger) {
						trigger = true;
						x = 0;
					}

					final int   line  = (buffer.bytesPerLine) - 3;
					final float scale = buffer.height;

					final int value  = (int)(beat.value() * scale);
					final int thresh = (int)(beat.threshold() * scale);

					final float[] fluxes  = audio.getOnset().fluxBands();
					final float[] threshs = audio.getOnset().thresholds();
					final int     y2bands = buffer.height / fluxes.length;

					int off = x * 3;
					for(int y = buffer.height; --y >= 0;) {
						final int band = MathUtilities.clamp((buffer.height-y) / y2bands, 0, fluxes.length -1);
												
						byte r = 0;
						byte g = 0;
						byte b = (byte)MathUtilities.clamp(fluxes[band] > threshs[band] ? fluxes[band] * 1000 : 0, 0, 255);

						if(y < value)
							g = (byte) 100;
						if(y == thresh)
							r = (byte) 200;

						buffer.data[off++] = r;
						buffer.data[off++] = g;
						buffer.data[off++] = b;
						off += line;
					}
					
					boolean vline = false;

					if(beat.beatCountPLL() != lastCountPLL) {
						lastCountPLL = beat.beatCountPLL();
						beatColor[0] = -1;
						beatColor[1] = -1;
						beatColor[2] = -1;
						vline  = true;
					} 

					if(beat.beatCount() != lastCount) {
						lastCount = beat.beatCount();
						beatColor[0] = -1;
						beatColor[1] = -1;
						beatColor[2] = 0;
						vline  = true;
					}

					off = x * 3;
					for(int y = 0; y < 4; y++) {
						buffer.data[off++] = beatColor[0];
						buffer.data[off++] = beatColor[1];
						buffer.data[off++] = beatColor[2];
						off += line;
					}

					if(vline) {					
						for(int y = 0; y < buffer.height-4; y++) {
							buffer.data[off++] = beatColor[0];
							buffer.data[off++] = beatColor[1];
							buffer.data[off++] = beatColor[2];
							off += line;
						}
					}

					decBeatColor(0);
					decBeatColor(1);
					decBeatColor(2);

					x++;
				}
			}

			private void decBeatColor(int i) {
				int c = (beatColor[i] & 0xFF) - 2;
				beatColor[i] = (byte)(c < 0 ? 0 : c);
			}
		});
	}

	public Composite createPartControl(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);

		FLASH    = parent.getDisplay().getSystemColor(SWT.COLOR_YELLOW);
		COL_BEAT = parent.getDisplay().getSystemColor(SWT.COLOR_YELLOW);

		result.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));	
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth  = 0;
		layout.marginHeight = 0;
		result.setLayout(layout);

		Composite ui = new Composite(result, SWT.NONE);
		ui.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));		
		layout = new GridLayout(2, false);
		layout.marginWidth  = 0;
		layout.marginHeight = 0;
		ui.setLayout(layout);
		bpmUI = new Label(ui, SWT.CENTER);
		bpmUI.setLayoutData(GridDataFactory.fill(true, false));
		bpmUI.addMouseListener(new MouseAdapter() {
			long lastTap;
			@Override
			public void mouseDown(MouseEvent e) {
				long now = System.currentTimeMillis();

				long tapTime    = now - lastTap;
				BeatDetect beat = audio.getBeat();
				beat.setBeat(beat.beatCountPLL()+1, BeatType.TAP, beat.frameTime());
				if(tapTime < 1200) {
					beat.setBeat(beat.beatCountPLL()+2, BeatType.ESTIMATED, beat.frameTime() + tapTime / IScheduler.SEC2MS);
					beat.setVal(BeatDetect.BPM, (float)(60.0 / (tapTime / IScheduler.SEC2MS)));
				} else
					beat.setBeat(beat.beatCountPLL()+2, BeatType.ESTIMATED, beat.frameTime() + 60.0 / beat.getVal(BeatDetect.BPM));

				lastTap = now;
			}
		});

		ParameterWindow.createUI(ui, audio.getBeat(), false);
		ParameterWindow.createUI(ui, audio.getOutGain(), false);

		canvasUI = new Canvas(result,  SWT.NO_REDRAW_RESIZE | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		canvasUI.addPaintListener(this);
		canvasUI.addControlListener(this);
		canvasUI.setSize(SWT.DEFAULT, buffer.get().height);
		canvasUI.setLayoutData(GridDataFactory.fill(true, false, SWT.DEFAULT, buffer.get().height));

		new Repeating(500, 16, ()->{if(valid()) canvasUI.redraw();});

		return result;
	}

	@Override
	public void controlMoved(ControlEvent e) {}

	@Override
	public void controlResized(ControlEvent e) {
		if(valid()) {
			ImageData buffer = AudioPanel.this.buffer.get();
			this.buffer.set(buffer.scaledTo(canvasUI.getSize().x, buffer.height));
			x = 0;
		}
	}

	int  lastBar;
	int  lastBeat;
	long flashTimeout;
	@Override
	public void paintControl(PaintEvent e) {
		try {
			long now = System.currentTimeMillis();

			ImageData buffer = this.buffer.get();
			int       x      = this.x;
			Image image = new Image(e.display, buffer);

			e.gc.drawImage(image, 0, 0);
			image.dispose();
			e.gc.setForeground(FLASH);
			e.gc.drawLine(x, 0, x, 16);

			int beat = audio.getBeat().beatCountPLL();
			if(lastBeat != beat) {
				bpmUI.setBackground(COL_BEAT);
				flashTimeout = now + 100;
				lastBeat     = beat;
				bpmUI.setText(audio.getBeat().beatType().toString());
			}
			if(now > flashTimeout) {
				bpmUI.setBackground(null);
				flashTimeout = Long.MAX_VALUE;
			}
		} catch(Throwable t) {
			log.severe(t);
		}
	}

	private boolean valid() {
		return canvasUI != null && !canvasUI.isDisposed() && canvasUI.getSize().x > 0;
	}
}
