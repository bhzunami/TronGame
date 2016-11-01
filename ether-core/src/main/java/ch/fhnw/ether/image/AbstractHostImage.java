/*
 * Copyright (c) 2013 - 2016 Stefan Muller Arisona, Simon Schubiger
 * Copyright (c) 2013 - 2016 FHNW & ETH Zurich
 * All rights reserved.
 *
 * Contributions by: Filip Schramka, Samuel von Stachelski
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *  Neither the name of FHNW / ETH Zurich nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ch.fhnw.ether.image;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.util.BufferUtilities;

abstract class AbstractHostImage extends AbstractImage implements IHostImage {
	private ByteBuffer pixels;
	
	protected AbstractHostImage(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) {
		this(width, height, componentType, componentFormat, alphaMode, null);
	}

	protected AbstractHostImage(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode, ByteBuffer pixels) {
		super(width, height, componentType, componentFormat, alphaMode);

		if (pixels != null) {
			if (!pixels.isDirect())
				throw new IllegalArgumentException("direct buffer required");
			
			if (pixels.capacity() < width * height * getNumBytesPerPixel()) {
				System.out.println(pixels.capacity() + " " + width + " " + height + " " + getNumBytesPerPixel());
				throw new IllegalArgumentException("pixel buffer too small");
			}
		} else {
			pixels = BufferUtils.createByteBuffer(width * height * getNumBytesPerPixel());
		}
		
		this.pixels = pixels;
	}
	
	@Override
	public final void clear() {
		BufferUtilities.fill(pixels, 0, pixels.capacity(), (byte)0);
	}
	
	@Override
	public final IHostImage copy() {
		IHostImage image = allocate();
		BufferUtilities.arraycopy(getPixels(), 0, image.getPixels(), 0, getPixels().capacity());
		return image;
	}

	@Override
	public final IHostImage allocate() {
		return allocate(getWidth(), getHeight());
	}
	
	@Override
	public final IHostImage allocate(int width, int height) {
		return create(width, height, getComponentType(), getComponentFormat(), getAlphaMode(), null);
	}
	
	@Override
	public final IHostImage scale(int width, int height) {
		return Platform.get().getImageSupport().scale(this, width, height);
	}
	
	// TODO test & optimize
	@Override
	public final IHostImage convert(ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) {
		if (getComponentType() == componentType && getComponentFormat() == componentFormat && getAlphaMode() == alphaMode)
			return this;
		
		boolean convertFormat = false;
		boolean convertAlpha = false;
		
		if (componentType == null)
			componentType = getComponentType();
		
		if (componentFormat == null)
			componentFormat = getComponentFormat();
		else if (componentFormat != getComponentFormat())
			convertFormat = true;
		
		if (alphaMode == null)
			alphaMode = getAlphaMode();
		else if (alphaMode != getAlphaMode())
			convertAlpha = true;
		
		IHostImage result = create(getWidth(), getHeight(), componentType, componentFormat, alphaMode, null);
		
		float[] pixel = new float[4];
		for (int y = 0; y < getHeight(); ++y) {
			for (int x = 0; x < getWidth(); ++x) {
				getPixel(x, y, pixel);
				if (convertFormat) {
					convertFormatToRGBA(getComponentFormat(), pixel);
					convertRGBAToFormat(componentFormat, pixel);
				}
				if (convertAlpha) {
					if (alphaMode == AlphaMode.POST_MULTIPLIED)
						convertAlphaToPostMultiplied(componentFormat, pixel);
					else
						convertAlphaToPreMultiplied(componentFormat, pixel);
				}
				result.setPixel(x, y, pixel);
			}
		}
		
		return result;
	}
	
	@Override
	public final IHostImage getSubImage(int x, int y, int width, int height) {
		IHostImage image = allocate(width, height);
		if (x + width > getWidth())
			throw new IllegalArgumentException("sub-image too wide");
		if (y + height > getHeight())
			throw new IllegalArgumentException("sub-image too high");
		int pixelSize = getNumBytesPerPixel();
		int srcRowSize = getWidth();
		int dstRowSize = width;
		for (int i = 0; i < height; ++i)
			BufferUtilities.arraycopy(getPixels(), ((y + i) * srcRowSize + x) * pixelSize, image.getPixels(), i * dstRowSize * pixelSize, dstRowSize * pixelSize);
		return image;
	}

	@Override
	public final void setSubImage(int x, int y, IHostImage image) {
		image = image.convert(getComponentType(), getComponentFormat(), getAlphaMode());
		if (x + image.getWidth() > getWidth())
			throw new IllegalArgumentException("sub-image too wide");
		if (y + image.getHeight() > getHeight())
			throw new IllegalArgumentException("sub-image too high");
		int pixelSize  = getNumBytesPerPixel();
		int srcRowSize = image.getWidth();
		int dstRowSize = getWidth();
		for (int i = 0; i < image.getHeight(); ++i)
			BufferUtilities.arraycopy(image.getPixels(), i * srcRowSize * pixelSize, pixels, ((y + i) * dstRowSize + x) * pixelSize, srcRowSize * pixelSize);
	}

	@Override
	public final ByteBuffer getPixels() {
		ByteBuffer result = pixels.duplicate();
		result.clear();
		return result;
	}
	
	@Override
	public final IGPUImage createGPUImage() {
		return IGPUImage.create(getWidth(), getHeight(), getComponentType(), getComponentFormat(), getAlphaMode(), getPixels());
	}
	
	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof IHostImage) {
			IHostImage other = (IHostImage) obj;
			getPixels().rewind();
			other.getPixels().rewind();
			return getWidth() == other.getWidth() && 
				getHeight() == other.getHeight() && 
				getComponentFormat() == other.getComponentFormat() && 
				getComponentType() == other.getComponentType() &&
				getAlphaMode() == other.getAlphaMode() &&
				getPixels().equals(other.getPixels());
		}
		return false;
	}

	protected final int pos(int x, int y) {
		return (y * getWidth() + x) * getNumBytesPerPixel();		
	}

	public static IHostImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode, ByteBuffer pixels) {
		switch (componentType) {
		case BYTE:
			return new ByteImage(width, height, componentFormat, alphaMode, pixels);
		case FLOAT:
			return new FloatImage(width, height, componentFormat, alphaMode, pixels);
		}
		throw new IllegalArgumentException("unsupported component type: " + componentType);
	}
	
	private static void convertFormatToRGBA(ComponentFormat format, float[] pixel) {
		switch (format) {
		case G:
			pixel[3] = 1;
			pixel[2] = pixel[0];
			pixel[1] = pixel[0];
			break;
		case GA:
			pixel[3] = pixel[1];
			pixel[2] = pixel[0];
			pixel[1] = pixel[0];
			break;
		case RGB:
			pixel[3] = 1;
			break;
		case RGBA:
			break;
		}
	}
	
	private static void convertRGBAToFormat(ComponentFormat format, float[] pixel) {
		switch (format) {
		case G:
			pixel[0] = (pixel[0] + pixel[1] + pixel[2]) / 3f;
			break;
		case GA:
			pixel[0] = (pixel[0] + pixel[1] + pixel[2]) / 3f;
			pixel[1] = pixel[3];
			break;
		case RGB:
		case RGBA:
			break;
		}
	}
	
	private static void convertAlphaToPostMultiplied(ComponentFormat format, float[] pixel) {
		switch (format) {
		case G:
			break;
		case GA:
			if (pixel[1] > 0) {
				pixel[0] = pixel[0] / pixel[1];
			} else {
				pixel[0] = pixel[1] = 0;
			}
			break;
		case RGB:
			break;
		case RGBA:
			if (pixel[3] > 0) {
				pixel[0] = pixel[0] / pixel[1];
				pixel[1] = pixel[1] / pixel[1];
				pixel[2] = pixel[2] / pixel[1];
			} else {
				pixel[0] = pixel[1] = pixel[2] = pixel[3] = 0;
			}
		}
	}

	private static void convertAlphaToPreMultiplied(ComponentFormat format, float[] pixel) {
		switch (format) {
		case G:
			break;
		case GA:
			pixel[0] = pixel[0] * pixel[1];
			break;
		case RGB:
			break;
		case RGBA:
			pixel[0] = pixel[0] * pixel[3];
			pixel[1] = pixel[1] * pixel[3];
			pixel[2] = pixel[2] * pixel[3];
		}		
	}
	
	@Override
	public void dispose() {
		pixels = null; // help gc
	}
}
