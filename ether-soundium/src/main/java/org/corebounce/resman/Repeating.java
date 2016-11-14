package org.corebounce.resman;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;

public class Repeating extends TimerTask {
	private static final Timer timer = new Timer(true);
	
	final AtomicReference<Runnable> r;
	
	public Repeating(int initialDelay, int delay, Runnable r) {
		timer.schedule(this, initialDelay, delay);
		this.r     = new AtomicReference<>(r);
	}
	
	@Override
	public void run() {
		Runnable r = this.r.get();
		if(r == null) stop();
		Display.getDefault().asyncExec(r);
	}

	public synchronized void stop() {
		r.set(null);
		cancel();
	}
}
