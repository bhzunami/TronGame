package org.corebounce.soundium;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.util.IProgressListener;

public class MultiProgress implements IProgressListener {
	private       float                   progress;
	private final List<IProgressListener> listeners = new ArrayList<>();
	
	public MultiProgress(IProgressListener ... listeners) {
		for(IProgressListener p : listeners)
			add(p);
	}

	public void add(IProgressListener listener) {
		listeners.add(listener);
	}

	@Override
	public void setProgress(float progress) {
		this.progress = progress;
		for(IProgressListener p : listeners)
			p.setProgress(progress);
	}

	@Override
	public void done() {
		for(IProgressListener p : listeners)
			p.done();
	}

	@Override
	public float getProgress() {
		return progress;
	}

}
