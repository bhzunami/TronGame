package org.corebounce.resman;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import ch.fhnw.util.IDisposable;
import ch.fhnw.util.IProgressListener;

public class Splash implements IDisposable, IProgressListener {
	final Image       image;
	final ProgressBar progressUI;
	final Shell       shell;

	public Splash(Display display, Rectangle displayRect, boolean showProgress)  {
		image = new Image(display, getClass().getResourceAsStream("/splash.png"));
		shell = new Shell(SWT.ON_TOP);
		progressUI    = new ProgressBar(shell, SWT.NONE);
		progressUI.setMaximum(100);
		
		Label label = new Label(shell, SWT.NONE);
		label.setImage(image);

		FormLayout layout = new FormLayout();
		shell.setLayout(layout);

		FormData labelData = new FormData();
		labelData.right = new FormAttachment(100, 0);
		labelData.bottom = new FormAttachment(100, 0);
		label.setLayoutData(labelData);

		FormData progressData = new FormData();
		progressData.left = new FormAttachment(0, -5);
		progressData.right = new FormAttachment(100, 0);
		progressData.bottom = new FormAttachment(100, 0);
		progressUI.setLayoutData(progressData);
		progressUI.setVisible(showProgress);
		shell.pack();

		Rectangle splashRect = shell.getBounds();

		progressUI.setBounds(2, splashRect.height-14, splashRect.width-6, 10);

		int x = displayRect.x + (displayRect.width - splashRect.width) / 2;
		int y = displayRect.y + (displayRect.height - splashRect.height) / 2;
		shell.setLocation(x, y);
		
		shell.open();		
		setProgress(0);
	}

	public void open() {
	}
	
	public void setProgress(float f) {
		progressUI.setSelection((int) (f * 100));
		long timeout = System.currentTimeMillis() + 100;
		while(System.currentTimeMillis() < timeout)
			Display.getDefault().readAndDispatch();		
	}

	@Override
	public void dispose() {
		shell.dispose();
		image.dispose();
	}

	public static class SplashAction implements IAction, Listener, SelectionListener {
		private Splash splash;
		private Shell  parent;

		@Override
		public void run() {
			Display display = Display.getDefault();
			splash = new Splash(display, (parent == null ? display.getPrimaryMonitor() : parent.getMonitor()).getBounds(), false);
			display.addFilter(SWT.MouseDown, this);
			display.addFilter(SWT.KeyDown, this);
		}
		
		@Override
		public void handleEvent(Event event) {
			Display display = event.display;
			display.removeFilter(SWT.MouseDown, this);
			display.removeFilter(SWT.KeyDown, this);
			splash.dispose();
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

	@Override
	public void done() {}

	@Override
	public float getProgress() {
		return progressUI.getSelection() / 100f;
	}
}