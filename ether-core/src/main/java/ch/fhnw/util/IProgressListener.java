package ch.fhnw.util;

public interface IProgressListener {
	void  setProgress(float progress);
	void  done();
	float getProgress();
}
