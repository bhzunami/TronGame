package org.corebounce.resman;

import org.corebounce.audio.Audio;
import org.corebounce.audio.AudioPanel;
import org.corebounce.engine.Engine;
import org.corebounce.io.OSC;
import org.corebounce.soundium.TabPanel;
import org.eclipse.swt.widgets.Composite;

import ch.fhnw.util.Log;

public class Resman extends TabPanel {
	private static final Log log = Log.create();

	private final Engine            engine;
	private final Audio             audio;
	private final OSC               osc;
	private final MetaDB            db;
	private final PreviewFactory    pf;

	public Resman(Engine engine, Audio audio, OSC osc, MetaDB db, PreviewFactory pf) {
		super("Resman");
		this.engine = engine;
		this.audio  = audio;
		this.osc    = osc;
		this.db     = db;
		this.pf     = pf;
	}

	@Override
	protected void fillContent(Composite panel) {
		new BrowserPanel(engine, pf, osc, db).createPartControl(panel);
		try {
			new AudioPanel(audio).createPartControl(panel);
		} catch(Throwable t) {
			log.warning(t);
		}
	}
}
