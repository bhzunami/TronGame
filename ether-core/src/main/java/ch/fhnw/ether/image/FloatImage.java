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

import ch.fhnw.util.color.ColorUtilities;

final class FloatImage extends AbstractHostImage {
	
	FloatImage(int width, int height, ComponentFormat componentFormat, AlphaMode alphaMode, ByteBuffer pixels) {
		super(width, height, ComponentType.BYTE, componentFormat, alphaMode, pixels);
	}
	
	@Override
	public byte[] getPixel(int x, int y, byte[] dst) {
		int pos = pos(x, y);
		for (int i = 0; i < getComponentFormat().getNumComponents(); ++i)
			dst[i] = ColorUtilities.toByte(getPixels().getFloat(pos + i));
		return dst;
	}

	@Override
	public void setPixel(int x, int y, byte[] src) {
		int pos = pos(x, y);
		for (int i = 0; i < getComponentFormat().getNumComponents(); ++i)
			getPixels().putFloat(pos + i * 4, ColorUtilities.toFloat(src[i]));
	}

	@Override
	public float[] getPixel(int x, int y, float[] dst) {
		int pos = pos(x, y);
		for (int i = 0; i < getComponentFormat().getNumComponents(); ++i)
			dst[i] = getPixels().getFloat(pos + i * 4);
		return dst;
	}

	@Override
	public void setPixel(int x, int y, float[] src) {
		int pos = pos(x, y);
		for (int i = 0; i < getComponentFormat().getNumComponents(); ++i)
			getPixels().putFloat(pos + i * 4, src[i]);
	}

	@Override
	public byte getComponentByte(int x, int y, int component) {
		return ColorUtilities.toByte(getComponentFloat(x, y, component));
	}

	@Override
	public void setComponentByte(int x, int y, int component, byte value) {
		setComponentFloat(x, y, component, ColorUtilities.toFloat(value));
	}

	@Override
	public float getComponentFloat(int x, int y, int component) {
		return getPixels().getFloat(pos(x, y));
	}

	@Override
	public void setComponentFloat(int x, int y, int component, float value) {
		getPixels().putFloat(pos(x, y), value);
	}
}
