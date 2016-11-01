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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import ch.fhnw.ether.platform.IImageSupport.FileFormat;
import ch.fhnw.ether.platform.Platform;

/**
 * IImage is the opaque base type of all images. Besides size and format we
 * don't know anything about the image.
 * 
 * @author radar
 *
 */
public interface IImage {

	enum ComponentType {
		BYTE(1), FLOAT(4);

		private final int size;

		ComponentType(int size) {
			this.size = size;
		}

		public int getSize() {
			return size;
		}
	}

	enum ComponentFormat {
		G(1, false), GA(2, true), RGB(3, false), RGBA(4, true);

		private final int     numComponents;
		private final boolean hasAlpha;

		ComponentFormat(int numComponents, boolean hasAlpha) {
			this.numComponents = numComponents;
			this.hasAlpha      = hasAlpha;
		}

		public int getNumComponents() {
			return numComponents;
		}

		public static ComponentFormat get(int numComponents) {
			return ComponentFormat.values()[numComponents - 1];
		}

		public boolean hasAlpha() {
			return hasAlpha;
		}
	}

	enum AlphaMode {
		POST_MULTIPLIED, PRE_MULTIPLIED
	}

	/**
	 * Get image width in pixels.
	 */
	int getWidth();

	/**
	 * Get image height in pixels.
	 */
	int getHeight();

	/**
	 * Get image component type.
	 */
	ComponentType getComponentType();

	/**
	 * Get image component format.
	 */
	ComponentFormat getComponentFormat();

	/**
	 * Get image alpha mode.
	 */
	AlphaMode getAlphaMode();

	/**
	 * Get image pixel size in bytes.
	 */
	int getNumBytesPerPixel();
	
	/**
	 * Write an image to output stream.
	 * 
	 * @param image
	 *            the image to be written
	 * @param out
	 *            the stream to write to
	 * @param format
	 *            the requested file format or null for using default format
	 *            (jpg)
	 * @throws IOException
	 *             if image cannot be written
	 */
	static void write(IImage image, OutputStream out, FileFormat format) throws IOException {
		Platform.get().getImageSupport().write(image, out, format);
	}

	/**
	 * Write image to file.
	 * @see #write(IImage, OutputStream, FileFormat)
	 */
	static void write(IImage image, File file) throws IOException {
		write(image, new FileOutputStream(file), FileFormat.get(file));
	}
}
