package ch.fhnw.util;

public class Subprogress implements IProgressListener {
	private final IProgressListener parentProgress;
	private final float             amount;
	private final float             start;
	private       float             progress;
	
	public Subprogress(IProgressListener parentProgress, float amount) {
		this.parentProgress = parentProgress;
		this.amount         = amount;
		this.start          = parentProgress.getProgress();
	}

	@Override
	public void setProgress(float progress) {
		this.progress = progress;
		parentProgress.setProgress(start + progress * amount);
	}

	@Override
	public void done() {}

	@Override
	public float getProgress() {
		return progress;
	}
}
