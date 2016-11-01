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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.util.IDisposable;

/**
 * An IHostImage refers to an image whose pixel data remains in host memory. A host
 * image is mutable.
 * 
 * @author radar
 *
 */
public interface IHostImage extends IImage, IDisposable {

	/**
	 * Clears this image, set all pixel data to zero.
	 */
	void clear();
	
	/**
	 * Returns a copy of this image.
	 */
	IHostImage copy();

	/**
	 * Allocates a new image with same size, type, format and alpha mode.
	 */
	IHostImage allocate();
	
	/**
	 * Allocates a new image with specified size, but same type format and alpha mode.
	 */
	IHostImage allocate(int width, int height);
	
	/**
	 * Returns a new, resized instance of this image.
	 */
	IHostImage scale(int width, int height);

	/**
	 * Returns a new instance of this image, with type, format and alpha mode converted to requested values.
	 */
	IHostImage convert(ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode);

	/**
	 * Returns a sub-image of this image, as specified by the provided coordinates and size.
	 */
	IHostImage getSubImage(int x, int y, int width, int height);

	/**
	 * Sets a sub-image within this image. If the provided sub-image has a
	 * differen type / format, it is automatically converted to the target type
	 * / format. Beware that this may cause extra overhead.
	 */
	void setSubImage(int x, int y, IHostImage image);
	
	/**
	 * Get pixel at coordinate x-y in byte format. The pixel data is stored in
	 * the provided array, which needs to have a minimum size of the number of
	 * components in this image.
	 */
	byte[] getPixel(int x, int y, byte[] dst);

	/**
	 * Set pixel at coordinate x-y in byte format. The provided array needs to
	 * have a minimum size of the number of components in this image.
	 */
	void setPixel(int x, int y, byte[] src);
	
	/**
	 * Get pixel at coordinate x-y in float format. The pixel data is stored in
	 * the provided array, which needs to have a minimum size of the number of
	 * components in this image.
	 */
	float[] getPixel(int x, int y, float[] dst);
	
	/**
	 * Set pixel at coordinate x-y in float format. The provided array needs to
	 * have a minimum size of the number of components in this image.
	 */
	void setPixel(int x, int y, float[] src);
	
	/**
	 * Get byte component at coordinate x-y.
	 */
	byte getComponentByte(int x, int y, int component);
	
	/**
	 * Set byte component at coordinate x-y.
	 */
	void setComponentByte(int x, int y, int component, byte value);
	
	/**
	 * Get float component at coordinate x-y.
	 */
	float getComponentFloat(int x, int y, int component);
	
	/**
	 * Set float component at coordinate x-y.
	 */
	void setComponentFloat(int x, int y, int component, float value);
	
	/**
	 * Direct access to internal pixel buffer.
	 */
	ByteBuffer getPixels();
	
	/**
	 * Create an (independent) GPU image from this image. Note that this
	 * operation may be expensive.
	 */
	IGPUImage createGPUImage();
	
	/**
	 * Create new host image.
	 * 
	 * @see #create(int, int, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode, ByteBuffer)
	 */
	static IHostImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat) {
		return create(width, height, componentType, componentFormat, AlphaMode.POST_MULTIPLIED, null);
	}

	/**
	 * Create new host image.
	 * 
	 * @see #create(int, int, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode, ByteBuffer)
	 */
	static IHostImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat, ByteBuffer pixels) {
		return create(width, height, componentType, componentFormat, AlphaMode.POST_MULTIPLIED, pixels);
	}

	/**
	 * Create new host image.
	 * 
	 * @see #create(int, int, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode, ByteBuffer)
	 */
	static IHostImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) {
		return create(width, height, componentType, componentFormat, alphaMode, null);
	}

	/**
	 * Create new host image.
	 * 
	 * @param width
	 *            image width
	 * @param height
	 *            image height
	 * @param componentType
	 *            image type
	 * @param componentFormat
	 *            image format
	 * @param alphaMode
	 *            image alpha mode
	 * @param pixels
	 *            byte buffer containing pixel data or null for creating an
	 *            empty image
	 * @return the created image
	 */
	static IHostImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode, ByteBuffer pixels) {
		return AbstractHostImage.create(width, height, componentType, componentFormat, alphaMode, pixels);
	}
	
	/**
	 * Read a host image from input stream.
	 * 
	 * @param in
	 *            the stream to read from
	 * @param componentType
	 *            the requested component type for the image or null for using
	 *            the best matching type
	 * @param componentFormat
	 *            the requested component format for the image or null for using
	 *            the best matching format
	 * @param alphaMode
	 *            the requested alpha format or null for post multiplied
	 * @return the loaded image
	 * @throws IOException
	 *             if image cannot be read
	 */
	static IHostImage read(InputStream in, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return Platform.get().getImageSupport().readHost(in, componentType, componentFormat, alphaMode);
	}
	
	/**
	 * Read a host image from input stream.
	 * 
	 * @see #read(InputStream, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode)
	 */
	static IHostImage read(InputStream in) throws IOException {
		return read(in, null, null, null);
	}
	
	/**
	 * Read a host image from file.
	 * 
	 * @see #read(InputStream, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode)
	 */
	static IHostImage read(File file, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return read(new FileInputStream(file), componentType, componentFormat, alphaMode);
	}

	/**
	 * Read a host image from file.
	 * 
	 * @see #read(InputStream, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode)
	 */
	static IHostImage read(File file) throws IOException {
		return read(new FileInputStream(file), null, null, null);
	}

	/**
	 * Read a host image from URL.
	 * 
	 * @see #read(InputStream, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode)
	 */
	static IHostImage read(URL url, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return read(url.openStream(), componentType, componentFormat, alphaMode);
	}

	/**
	 * Read a host image from URL.
	 * 
	 * @see #read(InputStream, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode)
	 */
	static IHostImage read(URL url) throws IOException {
		return read(url.openStream(), null, null, null);
	}
}
