package org.corebounce.engine;

import org.corebounce.audio.Audio;
import org.corebounce.ui.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import ch.fhnw.ether.media.Parametrizable;
import ch.fhnw.ether.ui.ParameterWindow;

public class Inspector extends Composite implements IBounceletUpdate {
	private final Audio audio;
	
	private Bouncelet current;
	
	public Inspector(Audio audio, Composite parent) {
		super(parent, SWT.NONE);
		this.audio = audio;
		setLayoutData(GridDataFactory.fill(true, true));
		setLayout(new GridLayout(2,false));
	}
	
	@Override
	public void update(Bouncelet b) {
		if(current == b) return;
		clear(current);
		current = b;
		audio.addBeatListener(b);
		Parametrizable p = b.getParameters();
		Composite paramUI = (Composite)ParameterWindow.createUI(this, p, true);;
		new Label(paramUI, SWT.NONE);
		Button resetUI = new Button(paramUI, SWT.NONE);
		resetUI.setText("Reset");
		resetUI.setLayoutData(GridDataFactory.fill(true, false));
		resetUI.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {widgetDefaultSelected(e);}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if(current != null) current.reset();
			}
		});
		layout();
	}

	@Override
	public void clear(Bouncelet b) {
		if(current != null && current == b) {
			audio.removeBeatListener(b);
			for(Control c : getChildren())
				c.dispose();
			current = null;
		}
	}
	
	public Bouncelet getCurrent() {
		return current;
	}
}
