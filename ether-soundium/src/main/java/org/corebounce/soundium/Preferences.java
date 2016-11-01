package org.corebounce.soundium;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class Preferences implements IAction, SelectionListener {
	private Shell parent;
	private Shell prefs;

	@Override
	public void run() {
		Display display = Display.getDefault();
		
		prefs = parent == null ? new Shell(display) : new Shell(parent);
		prefs.setText(Soundium.VERSION + " command line arguments");
		prefs.setLayout(new FillLayout());

		Text t = new Text(prefs, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
		t.setText(Soundium.help());
				
		prefs.pack();
		
		Rectangle prefsRect   = prefs.getBounds();
		Rectangle displayRect = (parent == null ? display.getPrimaryMonitor() : parent.getMonitor()).getBounds();
		
		int x = displayRect.x + (displayRect.width - prefsRect.width) / 2;
		int y = displayRect.y + (displayRect.height - prefsRect.height) / 2;
		prefs.setLocation(x, y);

		prefs.open();		
	}	
	
	public void setParent(Shell parent) {
		this.parent = parent;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		widgetDefaultSelected(e);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		run();
	}
}
