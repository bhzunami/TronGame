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

/**
 * An IGPUImage refers to an image that resides on GPU memory. A GPU image is
 * not mutable through this interface, but can be modified by the GPU API
 * through obtaining the GPU handle.
 * 
 * @author radar
 *
 */
public interface IGPUImage extends IImage, IGPUTexture {
	IGPUImage TRANSPARENT_1x1 = create(1, 1, ComponentType.BYTE, ComponentFormat.RGBA, AlphaMode.POST_MULTIPLIED);
	
	/**
	 * Create host image from GPU image. Note that this operation may be expensive.
	 */
	IHostImage createHostImage();
	
	/**
	 * Create new GPU image.
	 * 
	 * @see #create(int, int, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode, ByteBuffer)
	 */
	static IGPUImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat) {
		return create(width, height, componentType, componentFormat, AlphaMode.POST_MULTIPLIED, null);
	}

	/**
	 * Create new GPU image.
	 * 
	 * @see #create(int, int, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode, ByteBuffer)
	 */
	static IGPUImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat, ByteBuffer pixels) {
		return create(width, height, componentType, componentFormat, AlphaMode.POST_MULTIPLIED, pixels);
	}

	/**
	 * Create new GPU image.
	 * 
	 * @see #create(int, int, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode, ByteBuffer)
	 */
	static IGPUImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) {
		return create(width, height, componentType, componentFormat, alphaMode, null);
	}

	/**
	 * Create new GPU image.
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
	static IGPUImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode, ByteBuffer pixels) {
		return new GLGPUImage(width, height, componentType, componentFormat, alphaMode, pixels);
	}

	/**
	 * Read a GPU image from input stream.
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
	static IGPUImage read(InputStream in, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return Platform.get().getImageSupport().readGPU(in, componentType, componentFormat, alphaMode);
	}

	/**
	 * Read a GPU image from input stream.
	 * 
	 * @see #read(InputStream, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode)
	 */
	static IGPUImage read(InputStream in) throws IOException {
		return read(in, null, null, null);
	}
	
	/**
	 * Read a GPU image from file.
	 * 
	 * @see #read(InputStream, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode)
	 */
	static IGPUImage read(File file, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return read(new FileInputStream(file), componentType, componentFormat, alphaMode);
	}

	/**
	 * Read a GPU image from file.
	 * 
	 * @see #read(InputStream, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode)
	 */
	static IGPUImage read(File file) throws IOException {
		return read(new FileInputStream(file), null, null, null);
	}

	/**
	 * Read a GPU image from URL.
	 * 
	 * @see #read(InputStream, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode)
	 */
	static IGPUImage read(URL url, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return read(url.openStream(), componentType, componentFormat, alphaMode);
	}

	/**
	 * Read a GPU image from URL.
	 * 
	 * @see #read(InputStream, ch.fhnw.ether.image.IImage.ComponentType,
	 *      ch.fhnw.ether.image.IImage.ComponentFormat,
	 *      ch.fhnw.ether.image.IImage.AlphaMode)
	 */
	static IGPUImage read(URL url) throws IOException {
		return read(url.openStream(), null, null, null);
	}
}
