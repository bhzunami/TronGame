package ch.fhnw.ether.video;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.RGB;

import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.util.TextUtilities;

public class GIFAccess extends FrameAccess {
	private List<IHostImage> frames = new ArrayList<>();
	private int              frameNo  = -1;
	private double           duration = 0;
	private final byte[]     rgb      = new byte[4];
	private int              width;
	private int              height;
	private int              nClips;
	private int[]            shotStarts;

	public GIFAccess(URLVideoSource src, int numPlays) throws IOException, URISyntaxException {
		super(src, numPlays);
		ImageLoader loader = new ImageLoader();
		ImageData[] images = loader.load(src.getURL().openStream());
		for(int i = 0; i < images.length; i++) {
			width  = Math.max(width,  images[i].x + images[i].width); 
			height = Math.max(height, images[i].y + images[i].height); 
		}
		nClips = images[0].delayTime - 10;
		if (nClips > 0 && images.length > (nClips * 2 + 1))
			shotStarts = new int[nClips];
		else
			nClips = 1;
		for(int i = 0; i < images.length; i++)
			frames.add(convert(i, i == 0 ? null : frames.get(i-1), images[i]));
		if(shotStarts != null) {
			duration /= nClips * 1000.0;
			duration *= images.length;
		}
		if(shotStarts != null)
			for(int i = 1; i < shotStarts.length; i++)
				shotStarts[i] += shotStarts[i-1];
	}

	private IHostImage convert(int idx, IHostImage previous, ImageData imageData) {
		if(shotStarts != null) {
			int clip = (idx - 1) / 2;
			if(clip >= 0 && clip < nClips) {
				if(((idx - 1) & 1) == 0) 
					shotStarts[clip] = imageData.delayTime;
				else
					duration += imageData.delayTime;
			}
		} else
			duration += imageData.delayTime / 1000.0;
		IHostImage result = previous == null ? IHostImage.create(width, height, ComponentType.BYTE, ComponentFormat.RGB) : previous.copy();
		for(int y = imageData.height; --y >= 0;)
			for(int x = imageData.width; --x >= 0;) {
				int pixel = imageData.getPixel(x, y);
				if(pixel == imageData.transparentPixel) continue;
				RGB srcrgb = imageData.palette.getRGB(pixel);
				rgb[0] = (byte) srcrgb.red;
				rgb[1] = (byte) srcrgb.green;
				rgb[2] = (byte) srcrgb.blue;
				result.setPixel(x+imageData.x, height-(y+imageData.y+1), rgb);
			}
		return result;
	}

	@Override
	public void dispose() {
		frames.clear();
	}

	@Override
	public double getDuration() {
		return duration;
	}

	@Override
	public float getFrameRate() {
		return (float) (frames.size() / getDuration());
	}

	@Override
	public long getFrameCount() {
		return frames.size();
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void rewind() {
		numPlays--;
		frameNo = 0;
	}

	@Override
	public boolean decodeFrame() {
		frameNo++;
		if(frameNo >= frames.size())
			rewind();
		return numPlays > 0;
	}

	@Override
	public double getPlayOutTimeInSec() {
		return frameNo / getFrameRate();
	}

	@Override
	public boolean isKeyframe() {
		return true;
	}

	@Override
	public IHostImage getHostImage(BlockingQueue<float[]> audioData) {
		return frameNo < 0 ? null : frames.get(frameNo % frames.size());
	}

	@Override
	public IGPUImage getGPUImage(BlockingQueue<float[]> audioData) {
		return getHostImage(audioData).createGPUImage();
	}

	@Override
	public String toString() {
		return super.toString() + (shotStarts == null ? "" : TextUtilities.toString(shotStarts));  
	}

	public int[] getShotStarts() {
		return shotStarts == null ? new int[] {frames.size()} : shotStarts;
	}
}
