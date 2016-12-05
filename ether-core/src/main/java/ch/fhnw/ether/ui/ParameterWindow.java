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

package ch.fhnw.ether.ui;

import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;

import ch.fhnw.ether.audio.IAudioSource;
import ch.fhnw.ether.media.AbstractFrame;
import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.Parameter.Type;
import ch.fhnw.ether.media.Parametrizable;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.video.IVideoSource;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;

public class ParameterWindow {
	private static final Log log = Log.create();

	public enum Flag {EXIT_ON_CLOSE,CLOSE_ON_STOP,HIDE_ON_STOP}

	static final float   S            = 100000f;
	static final int     NUM_TICKS    = 5;
	static final int     POLL_DELAY   = 40;

	private static NumberFormat FMT = TextUtilities.decimalFormat(2);

	AtomicReference<Shell> frame = new AtomicReference<>();
	SourceInfoUI           srcInfo;
	RenderProgram<?>       program;

	static class SourceInfoUI {
		private Label            frameRateUI;
		private Label            actualRateUI;
		private Label            realTimeUI;
		private Label            targetTimeUI;
		private Label            frameTimeUI;
		private Label            relativeFramesUI;
		private Label            totalFramesUI;
		private Label            lenFramesUI;
		private Label            lenSecsUI;
		private Label            channelsUI;
		private Label            sRateUI;
		private Label            widthUI;
		private Label            heightUI;
		private Group            composite;
		private long             startTime;
		private long             startFrames;
		private RenderProgram<?> program;

		public SourceInfoUI(Composite parent, RenderProgram<?> program) {
			this.program = program;
			AbstractFrameSource src = program.getFrameSource();

			composite        = new Group(parent, SWT.NULL);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(hfill());
			frameRateUI      = add("Nominal Frame Rate");
			actualRateUI     = add("Actual Frame Rate [FPS]");
			realTimeUI       = add("Real Time [secs]");
			targetTimeUI     = add("Target Time [secs]");
			frameTimeUI      = add("Frame Time [secs]");
			relativeFramesUI = add("Relative Frames");
			totalFramesUI    = add("Total Frames");
			lenFramesUI      = add("Length [frames]");
			lenSecsUI        = add("Length [secs]");
			if(src instanceof IAudioSource) {
				channelsUI = add("Channels");
				sRateUI    = add("Sampling Rate");
			}
			if(src instanceof IVideoSource) {
				widthUI   = add("Width");
				heightUI  = add("Height");
			}
			update(true);
		}

		void update(boolean programChange) {
			AbstractFrameSource src = program.getFrameSource();
			if(programChange) {
				composite.setText(src.toString());
				frameRateUI.setText(FMT.format(src.getFrameRate()));
				lenFramesUI.setText(Long.toString(src.getLengthInFrames()));
				lenSecsUI.setText(FMT.format(src.getLengthInSeconds()));
				if(src instanceof IAudioSource) {
					channelsUI.setText(Integer.toString(((IAudioSource)src).getNumChannels()));
					sRateUI.setText(FMT.format(((IAudioSource)src).getSampleRate()));
				}
				if(src instanceof IVideoSource) {
					widthUI.setText(Integer.toString(((IVideoSource)src).getWidth()));
					heightUI.setText(Integer.toString(((IVideoSource)src).getHeight()));
				}
			}
			IRenderTarget<?> target = program.getTarget(); 
			if(target != null) {
				if(target.getTotalElapsedFrames() == 0 || programChange) {
					startFrames = target.getTotalElapsedFrames();
					startTime   = System.nanoTime();
				} else {
					long frames  = target.getTotalElapsedFrames() - startFrames;
					long elapsed = System.nanoTime() - startTime;
					realTimeUI.setText(FMT.format(elapsed / IScheduler.SEC2NS));
					actualRateUI.setText(FMT.format((IScheduler.SEC2NS * frames) / elapsed));
				}

				targetTimeUI.setText(FMT.format(target.getTime()));

				AbstractFrame frame = target.getFrame();
				if(frame != null) {
					frameTimeUI.setText(FMT.format(frame.playOutTime));
				}

				relativeFramesUI.setText(Long.toString(target.getRelativeElapsedFrames()));
				totalFramesUI.setText(Long.toString(target.getTotalElapsedFrames()));
			}
		}

		private Label add(String label) {
			Label l = new Label(composite, SWT.LEFT);
			l.setText(label);
			l.setLayoutData(cfill());
			Label result = new Label(composite, SWT.RIGHT);
			result.setText("?");
			result.setLayoutData(cfill());
			return result;
		}
	}

	static class ParamUI implements SelectionListener, IDisposable, Runnable {
		Label          label;
		Slider         slider;
		Combo          combo;
		Button         check;
		Parametrizable cmd;
		Parameter      p;
		float          def;
		Composite      parent;

		ParamUI(Composite parent, Parametrizable cmd, Parameter param) {
			this.parent = parent;
			this.cmd    = cmd;
			this.p      = param;
			this.def    = cmd.getVal(param);
			this.label  = new Label(parent, SWT.NONE);
			updateLabel();
			this.label.setLayoutData(cfill());
			switch(p.getType()) {
			case BITMAP:
			case RANGE:
				try {
					this.slider = new Slider(parent, SWT.NONE);
					this.slider.setMinimum(0);
					this.slider.setMaximum(((int)S)+10);
					this.slider.setSelection(par2sel(cmd.getVal(p), p));
					this.slider.addSelectionListener(this);
					this.slider.setLayoutData(cfill());
					this.slider.setData("paramui", this);
					Display.getDefault().timerExec(POLL_DELAY, this);
				} catch(Throwable t) {
					log.warning(t);
				}
				break;
			case ITEMS:
				this.combo = new Combo(parent, SWT.READ_ONLY);
				this.combo.setItems(param.getItems());
				this.combo.addSelectionListener(this);
				this.combo.select((int)(cmd.getVal(p)));
				this.combo.setLayoutData(cfill());
				Display.getDefault().timerExec(POLL_DELAY, this);
				break;
			case VALUES:
				this.combo = new Combo(parent, SWT.READ_ONLY);
				this.combo.setItems(param.getItems());
				this.combo.addSelectionListener(this);
				this.combo.select(p.getValuesIndexFor(cmd.getVal(p)));
				this.combo.setLayoutData(cfill());
				Display.getDefault().timerExec(POLL_DELAY, this);
				break;
			case BOOL:
				this.check = new Button(parent, SWT.CHECK);
				this.check.addSelectionListener(this);
				this.check.setSelection(cmd.getVal(p) != 0);
				this.check.setLayoutData(cfill());
				Display.getDefault().timerExec(POLL_DELAY, this);
				break;
			}
		}

		public void reset() {
			if(slider != null) cmd.setVal(p, def);
			if(combo != null)  cmd.setVal(p, (int) def);
			if(check != null)  cmd.setVal(p, def);
		}

		public void zero() {
			cmd.setVal(p, 0);
		}

		@Override
		public void run() {
			if(label.isDisposed()) return;

			if(slider != null) {
				float val =  sel2par(slider.getSelection(), p);
				if(cmd.getVal(p) != val) {
					slider.setSelection(par2sel(cmd.getVal(p), p));
					updateLabel();
				}
			}
			if(combo != null) {
				if(p.getType() == Type.VALUES) {
					float val = p.getValues()[combo.getSelectionIndex()];
					if(cmd.getVal(p) != val)
						combo.select(p.getValuesIndexFor(cmd.getVal(p)));
				} else {
					float val =  combo.getSelectionIndex();
					if(cmd.getVal(p) != val)
						combo.select((int) cmd.getVal(p));
				}
			}
			if(check != null) {
				float val =  check.getSelection() ? 1 : 0;
				if(cmd.getVal(p) != val) {
					check.setSelection(cmd.getVal(p) != 0);
					cmd.setVal(p, check.getSelection() ? 1 : 0);
				}
			}

			if(parent instanceof Group) {
				Group g = (Group)parent;
				if(!(cmd.getGroupLabel().equals(g.getText())))
					g.setText(cmd.getGroupLabel());
			}

			Display.getDefault().timerExec(POLL_DELAY, this);
		}

		private int par2sel(float val, Parameter p) {
			return (int)(((cmd.getVal(p) - cmd.getMin(p)) / (cmd.getMax(p) - cmd.getMin(p))) * S);
		}

		private float sel2par(float sel, Parameter p) {
			return (((cmd.getMax(p)-cmd.getMin(p)) * sel) / S) + cmd.getMin(p);
		}

		@Override
		public void dispose() {
			Display.getDefault().timerExec(-1, this);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			if(e.getSource() == slider) {
				cmd.setVal(p, sel2par(slider.getSelection(), p));
				updateLabel();
			}
			else if(e.getSource() == combo) {
				if(p.getValues() == null)
					cmd.setVal(p, combo.getSelectionIndex());
				else
					cmd.setVal(p, p.getValues()[combo.getSelectionIndex()]);
			}
			else if(e.getSource() == check)
				cmd.setVal(p, check.getSelection() ? 1 : 0);
		}

		void updateLabel() {
			String label = p.getDescription();
			float  val   = cmd.getVal(p);
			switch(p.getType()) {
			case BITMAP:
				if(p.getItems() == Parameter.BITMAP8)
					label += ": " + TextUtilities.intToBits((int)(val), 8);
				else if(p.getItems() == Parameter.BITMAP16)
					label += ": " + TextUtilities.shortToHex((int)(val));
				else
					label += ": " + TextUtilities.intToHex((int)(val));
				break;
			case RANGE:
				label += ": " + FMT.format(val);
				break;
			case BOOL:
			case ITEMS:
			case VALUES:
			default:
				break;
			}
			this.label.setText(label);
		}
	}

	public ParameterWindow(final AbstractRenderCommand<?> src, Flag ... flags) {
		this(null, src, flags);
	}

	private boolean hasFlag(Flag flag, Flag[] flags) {
		for(Flag f : flags)
			if(f == flag)
				return true;
		return false;
	}

	public static Control createUI(Composite parent, Parametrizable cmd, boolean asGroup) {
		if(cmd.getClass().getName().equals(cmd.toString()) && cmd.getParameters().length == 0) {
			Label result = new Label(parent, SWT.BORDER);
			result.setText(cmd.getGroupLabel());
			result.setLayoutData(hfill());
			return result;
		}

		Control result;

		Parameter[] params = cmd.getParameters();
		ParamUI[]   uis    = new ParamUI[params.length];
		if(params.length > 0) {
			Composite comp;
			if(asGroup) {
				Group group = new Group(parent, SWT.NULL);
				group.setText(cmd.getGroupLabel());
				comp = group;
			} else {
				comp = new Composite(parent, SWT.NULL);
			}

			comp.setLayout(new GridLayout(2, false));
			comp.setLayoutData(hfill());

			for(int i = 0; i < uis.length; i++)
				uis[i] = new ParamUI(comp, cmd, params[i]);

			result = comp;
		} else {
			Label text = new Label(parent, SWT.NONE);
			text.setText(cmd.toString());
			text.setLayoutData(hfill());
			result = text;
		}

		Menu menu   = new Menu(parent);
		if(cmd instanceof AbstractRenderCommand<?>) {
			AbstractRenderCommand<?> rcmd = (AbstractRenderCommand<?>)cmd;
			addMenuItem(menu, SWT.CHECK, "Enabled", rcmd.isEnabled(), new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					rcmd.setEnable(((MenuItem)e.getSource()).getSelection());
					setEnablded(result, rcmd.isEnabled());
				}
			});
		}

		if(params.length > 0) {
			addMenuItem(menu, SWT.NONE, "Reset", false, new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					for (ParamUI p : uis)
						p.reset();
				}
			});
			addMenuItem(menu, SWT.NONE, "Zero", false, new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					for (ParamUI p : uis)
						p.zero();
				}
			});
		}

		result.setMenu(menu);

		return result;
	}

	private static void addMenuItem(Menu menu, int style, String label, boolean state, SelectionListener listener) {
		MenuItem item = new MenuItem(menu, style);
		item.setText(label);
		item.setSelection(state);
		item.addSelectionListener(listener);
	}


	private static void setEnablded(Control cmp, boolean state) {
		cmp.setForeground(state ? null : cmp.getBackground());
		if(cmp instanceof Composite) {
			for(Control c : ((Composite) cmp).getChildren())
				setEnablded(c, state);
		}
	}

	public ParameterWindow(final IParameterWindowAddOn addOn, final AbstractRenderCommand<? extends IRenderTarget<?>> src, Flag ... flags) {
		Platform.get().runOnMainThread(new Runnable() {
			@Override
			public void run() {
				Shell f = new Shell(Display.getDefault());
				f.setText("Parameters");

				if(hasFlag(Flag.EXIT_ON_CLOSE, flags))
					f.addDisposeListener(e->Platform.get().exit());
				f.setLayout(new GridLayout(2, false));
				if(addOn != null)
					addOn.add(f);

				ScrolledComposite sc = new ScrolledComposite(f, SWT.H_SCROLL | SWT.V_SCROLL);
				sc.setLayoutData(hvfill());
				createUIRecr(sc, src);

				pack(sc);
				f.pack();
				f.setLocation(Display.getDefault().getBounds().width - f.getBounds().width, f.getLocation().y);
				f.setVisible(true);
				frame.set(f);
			}

			Composite createUIRecr(Composite parent, AbstractRenderCommand<?> rcmd) {
				Composite result = new Composite(parent, SWT.NONE);
				if(parent instanceof ScrolledComposite) {
					ScrolledComposite sc = (ScrolledComposite)parent;
					sc.setContent(result);
					sc.setExpandVertical(true);
					sc.setExpandHorizontal(true);
				}
				result.setLayout(new GridLayout(2, false));
				result.setLayoutData(hfill());
				if(rcmd instanceof RenderProgram<?>) {
					program = (RenderProgram<?>)rcmd;

					Display.getDefault().timerExec(40, new Runnable() {
						AbstractRenderCommand<?>[] lastProgram = program.getProgram();

						@Override
						public void run() {
							if(!(program.getTarget().isRendering())) {
								if(hasFlag(Flag.HIDE_ON_STOP, flags))
									frame.get().setVisible(false);
								if(hasFlag(Flag.CLOSE_ON_STOP, flags)) {
									dispose(result);
									frame.get().dispose();
								}
								stopped();
								return;
							}
							boolean programChange = false;
							if(lastProgram != program.getProgram()) {
								programChange = true;
								lastProgram = program.getProgram();
								for(Control c : result.getChildren())
									dispose(c);
								for(AbstractRenderCommand<?> cmd : lastProgram)
									createUIRecr(result, cmd);
								pack(result);
							}
							if(!(srcInfo.composite.isDisposed()))
								srcInfo.update(programChange);

							Display.getDefault().timerExec(40, this);
						}
					});
					for(AbstractRenderCommand<?> cmd : program.getProgram())
						createUIRecr(result, cmd);
				} else if(rcmd instanceof AbstractFrameSource) {
					srcInfo = new SourceInfoUI(result, program);
				} else {
					createUI(parent, rcmd, true);
				}
				return result;
			}
		});
	}

	protected void stopped() {
	}

	private static void dispose(Control c) {
		if(c instanceof Composite) {
			for(Control cmp : ((Composite)c).getChildren()) {
				Object parmui = cmp.getData("paramui");
				if(parmui instanceof IDisposable)
					((IDisposable)parmui).dispose();
				if(cmp instanceof Composite)
					dispose(cmp);
			}
		}
		c.dispose();
	}

	private static void pack(Control panel) {
		if(panel == null) return;
		if(panel instanceof ScrolledComposite) {
			ScrolledComposite sc = (ScrolledComposite)panel;
			((Composite)sc.getContent()).layout();
			Point sz = sc.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT);
			sc.getContent().setSize(sz);
			sc.setMinSize(sz);
		} else pack(panel.getParent());
	}

	public boolean isVisible() {
		while(frame.get() == null) {
			try {
				Thread.sleep(2);
			} catch (Throwable e) {}
		}
		return frame.get().isVisible();
	}

	public static GridData cfill() {
		GridData result                  = new GridData();
		result.grabExcessHorizontalSpace = true;
		result.horizontalAlignment       = SWT.FILL;
		result.verticalAlignment         = SWT.CENTER;
		return result;
	}

	public static GridData hfill() {
		GridData result = cfill();
		result.horizontalSpan = 2;
		return result;
	}

	public static GridData hvfill() {
		GridData result = hfill();
		result.grabExcessVerticalSpace = true;
		result.verticalAlignment       = SWT.FILL;
		return result;
	}

}