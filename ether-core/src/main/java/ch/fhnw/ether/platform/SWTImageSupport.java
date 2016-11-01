package ch.fhnw.ether.platform;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;

import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage;
import ch.fhnw.ether.image.IImage.AlphaMode;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.util.ArrayUtilities;
import ch.fhnw.util.MIME;

public class SWTImageSupport extends STBImageSupport {
	private static final PaletteData RGB8  = new PaletteData(0xFF0000, 0xFF00, 0xFF);
	private static final PaletteData RGBA8 = new PaletteData(0xFF000000, 0xFF0000, 0xFF00);

	private ThreadLocal<ImageLoader> writer = new ThreadLocal<ImageLoader>() {
		@Override
		protected ImageLoader initialValue() {
			return new ImageLoader();
		}
	};

	@Override
	public void write(IImage image, OutputStream out, FileFormat format) throws IOException {
		if (format == null)
			format = FileFormat.JPEG;

		if(format == FileFormat.BIN) {
			out.write(toRGB(image));
		} else {
			writer.get().data = new ImageData[] { toImageData(image) };
			writer.get().save(out, swtFormat(format));
			out.close();
		}
	}

	private byte[] toRGB(IImage image) throws IOException {
		IHostImage hostImage = null;
		if (image instanceof IHostImage)
			hostImage = (IHostImage)image;
		else if (image instanceof IGPUImage)
			hostImage = ((IGPUImage)image).createHostImage();
		else
			throw new IllegalArgumentException("unsupported image type");

		hostImage = hostImage.convert(ComponentType.BYTE, ComponentFormat.RGB, AlphaMode.POST_MULTIPLIED);
		
		byte[] result = new byte[hostImage.getWidth() * hostImage.getHeight() * 3];
		hostImage.getPixels().get(result);
		return result;
	}

	public static ImageData toImageData(IImage image) throws IOException {
		IHostImage hostImage = null;
		if (image instanceof IHostImage)
			hostImage = (IHostImage)image;
		else if (image instanceof IGPUImage)
			hostImage = ((IGPUImage)image).createHostImage();
		else
			throw new IllegalArgumentException("unsupported image type");

		hostImage = hostImage.convert(ComponentType.BYTE, hostImage.getComponentFormat().hasAlpha() ? ComponentFormat.RGBA : ComponentFormat.RGB, AlphaMode.POST_MULTIPLIED);

		ByteBuffer pixels = hostImage.getPixels();
		int w = hostImage.getWidth();
		int h = hostImage.getHeight();
		ComponentFormat format = hostImage.getComponentFormat();

		pixels.clear();
		byte[] data = new byte[pixels.capacity()];

		// flip image
		pixels.clear();
		int pixelSize = hostImage.getNumBytesPerPixel();
		int linelen = w * pixelSize;
		for (int y = h; --y >= 0;)
			pixels.get(data, y * linelen, linelen);

		ImageData result = new ImageData(w, h, pixelSize * 8, getPaletteData(format), 1, data);
		if (format.hasAlpha()) {
			byte[] alpha = new byte[w * h];
			int idx = 3;
			for (int y = h; --y >= 0;) {
				for (int x = 0; x < w; x++, idx += pixelSize)
					alpha[y * w + x] = pixels.get(idx);
			}
			result.alphaData = alpha;
		}
		return result;
	}

	private static PaletteData getPaletteData(ComponentFormat format) throws IOException {
		switch (format) {
		case RGB:
			return RGB8;
		case RGBA:
			return RGBA8;
		default:
			throw new IllegalArgumentException("unsupported component format: " + format);
		}
	}

	private int swtFormat(FileFormat format) {
		switch (format) {
		case JPEG:
			return SWT.IMAGE_JPEG;
		case PNG:
			return SWT.IMAGE_PNG;
		case TIFF:
			return SWT.IMAGE_TIFF;
		case BMP:
			return SWT.IMAGE_BMP;
		case BIN:
			return SWT.IMAGE_COPY;
		}
		throw new IllegalArgumentException("unsupported file format: " + format);
	}

	private static final String[] TYPES = {
			MIME.MT_PNG, MIME.MT_JPEG
	};

	@Override
	public boolean canWrite(String mimeType) {
		return ArrayUtilities.containsEquals(TYPES, mimeType);
	}
}
