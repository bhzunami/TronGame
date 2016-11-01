package org.corebounce.engine;

import java.util.Set;

import org.corebounce.io.MIDI;
import org.corebounce.io.OSC;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.Parametrizable;
import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.IdentityHashSet;
import ch.fhnw.util.Log;
import ch.fhnw.util.color.RGB;

public class MultiSelection extends Bouncelet implements SelectionListener {
	private static final Log log = Log.create();

	private Table          table;
	private Set<Bouncelet> s_selection = new IdentityHashSet<>();

	public MultiSelection(Engine engine, OSC osc, MIDI midi, IBounceletUpdate update) {
		super(engine, osc, midi, -1, "Multi", ClassUtilities.EMPTY_String, 0, RGB.BLACK, update);
	}

	public void setTable(Table table) {
		if(this.table != null)
			this.table.removeSelectionListener(this);
		this.table = table;
		if(table != null) {
			table.addSelectionListener(this);
		}
	}

	@Override
	protected Parametrizable createParams(Parameter[] ps) {
		return new Parametrizable("", ps) {
			@Override
			public void setVal(Parameter p, float val) {
				lastUpdate = System.currentTimeMillis();
				super.setVal(p, val);
				updateDecklight(p, val);
				updatePush();
			}

			@Override
			public String getGroupLabel() {
				return getLabel();
			}
		};
	}

	@Override
	protected void updateDecklight(Parameter p, float val) {
		synchronized (s_selection) {
			for(Bouncelet b : s_selection)
				osc.send("/" + Engine.BOUNCELET + "/" + b.id + "/" + p.getName(), val);
		}
	}

	@Override
	public void widgetSelected(SelectionEvent e) {widgetDefaultSelected(e);}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		try {
			synchronized (s_selection) {
				s_selection.clear();
				if(table != null) {
					TableItem[] selection = table.getSelection();
					String label = selection.length + (selection.length == 1 ? " Bouncelet" : " Bouncelets");
					if(selection.length == 0) {
						update.clear(this);
					} else {
						float active  = 0;
						float red     = 0;
						float green   = 0;
						float blue    = 0;
						for(TableItem item : selection) {
							Bouncelet b = (Bouncelet) item.getData(Engine.BOUNCELET);
							RGB       c = b.getColor();
							red   += c.r;
							green += c.g;
							blue  += c.b;
							active += b.getActive();
							s_selection.add(b);
						}
						update(label, active / selection.length, new RGB(red / selection.length, green / selection.length, blue / selection.length));
					}
				} else {
					update.clear(this);
				}
			}
		} catch(Throwable t) {
			log.warning(t);
		}
	}
}
