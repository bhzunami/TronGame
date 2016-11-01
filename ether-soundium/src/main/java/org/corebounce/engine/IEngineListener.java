package org.corebounce.engine;

public interface IEngineListener {
	void bounceletsChanged(Bouncelet[] bouncelets);
	void selectionChanged(Bouncelet[] selection);
}
