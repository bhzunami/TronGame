package org.corebounce.resman;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.TableItem;

public class ItemRepainer implements Runnable {
	private final TableItem item;
	private final int       delay;
	private boolean         running;

	public ItemRepainer(TableItem item, int delay) {
		this.item  = item;
		this.delay = delay;
	}

	public ItemRepainer start() {
		if(!(running)) {
			this.running = true;
			item.getDisplay().timerExec(delay, this);
		}
		return this;
	}

	public void stop() {
		this.running = false;
	}

	@Override
	public void run() {
		if(!(item.isDisposed())) {
			Rectangle r = item.getBounds();
			item.getParent().redraw(r.x, r.y, r.width + 20, r.height, true);
			if(running) item.getDisplay().timerExec(delay, this);
		}
	}
}
