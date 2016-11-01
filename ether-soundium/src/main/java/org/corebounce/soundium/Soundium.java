package org.corebounce.soundium;


import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.corebounce.audio.Audio;
import org.corebounce.engine.Engine;
import org.corebounce.io.MIDI;
import org.corebounce.io.OSC;
import org.corebounce.io.PushPanel;
import org.corebounce.resman.MetaDB;
import org.corebounce.resman.PreviewFactory;
import org.corebounce.resman.Resman;
import org.corebounce.resman.Resources;
import org.corebounce.soundium.Splash.SplashAction;
import org.corebounce.ui.CocoaUIEnhancer;
import org.corebounce.ui.GridDataFactory;
import org.corebounce.video.Monitors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Table;

import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.midi.AbletonPush;
import ch.fhnw.ether.midi.AbletonPush.Basic;
import ch.fhnw.ether.midi.AbletonPush.PControl;
import ch.fhnw.ether.platform.IMonitor;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.platform.Platform.OS;
import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.Log;
import ch.fhnw.util.Subprogress;
import ch.fhnw.util.TextUtilities;

public class Soundium {
	private static final Log log = Log.create();

	static private final Class<?>[] SUBSYSTEMS = {
			Monitors.class,
			Audio.class,
			MetaDB.class,
			OSC.class,
			MIDI.class,
			Resources.class,
	};

	public static final String VERSION = "Soundium Nouveau";

	private final Monitors          monitors;
	private final Audio             audio;
	private final OSC               osc;
	private final MetaDB            db;
	private final PreviewFactory    pf;
	private final MIDI              midi;
	private final Shell             shell;
	private final Resman            resman;
	private final Engine            engine;
	private final PushPanel         push;
	private final TabFolder         tabFolder;

	public Soundium(String ... args) throws InterruptedException, RenderCommandException, IOException, NoSuchAlgorithmException, LineUnavailableException, UnsupportedAudioFileException {
		Display display = Display.getDefault();

		Preferences  prefs;
		SplashAction about;
		if(Platform.getOS() == OS.MACOSX) {
			CocoaUIEnhancer enhancer = new CocoaUIEnhancer(VERSION);
			enhancer.hookApplicationMenu(display, about = new Splash.SplashAction(), prefs = new Preferences());
		} else {
			about = new SplashAction();
			prefs = new Preferences();
		}		

		monitors       = new Monitors(args);
		IMonitor  sndmMon  = monitors.getSoundiumMonitor();
		Rectangle sndmMonR = new Rectangle(sndmMon.getX(), sndmMon.getY(), sndmMon.getWidth(), sndmMon.getHeight());
		Splash splash = new Splash(display, sndmMonR, true);
		splash.open();

		int cam = 0;
		for(IMonitor mon : monitors.getEngineMonitors()) {
			log.info("Engine camera " + cam++ + " assigned to monitor " + mon.getIndex() + " '" + mon + "'");
		}

		float step       = 0;
		float audioSteps = 3;
		float numSteps   = 6 + audioSteps;
		MultiProgress progress = new MultiProgress(splash);
		progress.setProgress(++step/numSteps);
		midi      = new MIDI(args);
		progress.add(midi);
		progress.setProgress(++step/numSteps);
		audio      = new Audio(new Subprogress(progress, audioSteps / numSteps), midi, args);
		step += audioSteps;
		db         = new MetaDB(args);
		progress.setProgress(++step/numSteps);
		osc        = new OSC(args, audio, monitors);
		progress.setProgress(++step/numSteps);
		pf         = new PreviewFactory(db);
		progress.setProgress(++step/numSteps);
		new Resources(db, pf, args);
		progress.setProgress(++step/numSteps);
		progress.done();
		splash.dispose();

		shell = new Shell(display);
		shell.setLayout(new GridLayout());

		tabFolder = new TabFolder(shell, SWT.BORDER);
		tabFolder.setLayoutData(GridDataFactory.fill(true, true));

		engine = new Engine(db, audio, osc, midi);
		engine.createTabPanel(tabFolder);

		if(midi.getPush() != null) {
			push = new PushPanel(engine);
			push.createTabPanel(tabFolder);
		} else push = null;

		resman = new Resman(engine, audio, osc, db, pf);
		resman.createTabPanel(tabFolder);

		tabFolder.setSelection(resman.getTabIndex());

		shell.setText(VERSION);
		shell.setLocation(sndmMonR.x, sndmMonR.y);
		shell.setMaximized(true);
		//shell.setSize(1920, 500);

		if(Platform.getOS() != OS.MACOSX) {
			final Menu m = new Menu(shell, SWT.BAR);

			final MenuItem file = new MenuItem(m, SWT.CASCADE);
			file.setText("&File");
			final Menu filemenu = new Menu(shell, SWT.DROP_DOWN);
			file.setMenu(filemenu);
			final MenuItem exitItem = new MenuItem(filemenu, SWT.PUSH);
			exitItem.setAccelerator(SWT.CTRL + 'Q');
			exitItem.setText("&Quit\tCTRL+Q");
			exitItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {widgetDefaultSelected(e);}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {System.exit(0);}
			});

			//create a Window menu and add Child item
			final MenuItem window = new MenuItem(m, SWT.CASCADE);
			window.setText("&Window");
			final Menu windowmenu = new Menu(shell, SWT.DROP_DOWN);
			window.setMenu(windowmenu);
			final MenuItem prefItem = new MenuItem(windowmenu, SWT.PUSH);
			prefItem.setText("&Preferences");
			prefItem.addSelectionListener(prefs);

			// create a Help menu and add an about item
			final MenuItem help = new MenuItem(m, SWT.CASCADE);
			help.setText("&Help");
			final Menu helpmenu = new Menu(shell, SWT.DROP_DOWN);
			help.setMenu(helpmenu);
			final MenuItem aboutItem = new MenuItem(helpmenu, SWT.PUSH);
			aboutItem.setText("&About");
			aboutItem.addSelectionListener(about);

			//setBackground(shell, display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
			shell.setMenuBar(m);
		}

		viewMenu();

		shell.setVisible(true);
		shell.addDisposeListener(event->Platform.get().exit());

		prefs.setParent(shell);
		about.setParent(shell);

		display.timerExec(500, osc::start);
		display.timerExec(500, shell::forceActive);

		setPush();
	}

	private void setPush() {
		AbletonPush push = midi.getPush();
		if(push != null) {
			push.set(PControl.DEVICE, ()->{
				push.setColor(PControl.DEVICE, Basic.FULL);
				push.setColor(PControl.BROWSE, Basic.HALF);
				Display.getDefault().asyncExec(()->tabFolder.setSelection(this.push.getTabIndex()));
			});
			push.set(PControl.BROWSE, ()->{
				push.setColor(PControl.DEVICE, Basic.HALF);
				push.setColor(PControl.BROWSE, Basic.FULL);
				Display.getDefault().asyncExec(()->tabFolder.setSelection(resman.getTabIndex()));
			});
			if(tabFolder.getSelectionIndex() == this.push.getTabIndex()) {
				push.setColor(PControl.DEVICE, Basic.FULL);
				push.setColor(PControl.BROWSE, Basic.HALF);
			} else if(tabFolder.getSelectionIndex() == resman.getTabIndex()) {
				push.setColor(PControl.DEVICE, Basic.HALF);
				push.setColor(PControl.BROWSE, Basic.FULL);
			}
		}
	}

	private void viewMenu() {
		MenuItem view = new MenuItem(shell.getMenuBar() == null ? Display.getDefault().getMenuBar() : shell.getMenuBar(), SWT.CASCADE);
		view.setText("&View");
		Menu viewmenu = new Menu(shell, SWT.DROP_DOWN);
		view.setMenu(viewmenu);

		addItem(viewmenu, engine, 'E');
		if(push != null)
			addItem(viewmenu, push, 'D');
		addItem(viewmenu, resman, 'R');
	}

	private void addItem(Menu viewmenu, TabPanel tab, char accel) {
		MenuItem engineItem = new MenuItem(viewmenu, SWT.PUSH);
		engineItem.setText("&"+tab.getLabel()+"\tCTRL+"+accel);
		engineItem.setAccelerator(SWT.CTRL + accel);
		engineItem.addSelectionListener(tab);
	}

	void setBackground(Composite comp, Color color) {
		for(Control c : comp.getChildren())
			if(c instanceof Composite)
				setBackground((Composite)c, color);
			else if(!(c instanceof Table))
				c.setBackground(color);
		comp.setBackground(color);
	}

	private static String getPrefix(Class<?> cls) {
		try {
			return cls.getField("CFG_PREFIX").get(null).toString();
		} catch(Throwable t) {
			return "??";
		}
	}

	private static String[] getOptions(Class<?> cls) {
		try {
			return (String[])cls.getField("CFG_OPTIONS").get(null);
		} catch(Throwable t) {
			return ClassUtilities.EMPTY_StringA;
		}
	}

	static String help(Class<?>[] subsystems) {
		StringBuilder result = new StringBuilder();
		for(Class<?> cls : subsystems) {
			StringBuilder options = new StringBuilder();
			String[]      optionsA =getOptions(cls);
			for(int i = 0; i < optionsA.length; i += 2) {
				if(i > 0) options.append(',');
				options.append(optionsA[i]);
			}
			result.append("--").append(cls.getName()).append(":\n").append(getPrefix(cls)).append(':').append(options).append('\n');
			for(int i = 0; i< optionsA.length; i+= 2) {
				result.append("  ").append(optionsA[i]).append(":\t").append(optionsA[i+1]).append('\n');
			}
		}
		return result.toString();
	}

	public static void main(String[] args) {
		Display.setAppName(VERSION);
		Display.setAppVersion(VERSION);

		try {
			if(args.length == 0) {
				System.out.println(help());
				System.exit(0);
			}
			Platform.get().init();
			new Soundium(args);
			Platform.get().run();
		} catch(Throwable t) {
			t.printStackTrace();
			System.out.println(TextUtilities.cat(args, ' '));
			System.out.println(help());
		}
	}

	public static String help() {
		return help(SUBSYSTEMS);
	}
}
