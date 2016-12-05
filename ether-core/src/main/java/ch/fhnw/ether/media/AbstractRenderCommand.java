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

package ch.fhnw.ether.media;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.math.MathUtilities;

public abstract class AbstractRenderCommand<T extends IRenderTarget<?>> extends Parametrizable {
	private boolean                          enabled = true; 
	private boolean                          skip    = false;
	private final AtomicReference<ImageData> buffer  = new AtomicReference<ImageData>();
	private int                              plotX;
	private boolean                          pausePlot;
	
	protected AbstractRenderCommand(Parameter ... parameters) {
		super(parameters);
	}

	protected abstract void run(T target) throws RenderCommandException;

	protected void init(T target) throws RenderCommandException {}

	public final void runInternal(T target) throws RenderCommandException {
		if(isEnabled())
			run(target);
	}

	@Override
	public String toString() {
		return TextUtilities.getShortClassName(this) ;
	}

	public void setEnable(boolean state) {
		this.enabled = state;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setSkip(boolean state) {
		this.skip = state;
	}

	public boolean isSkip() {
		return skip;
	}

	public int getPlotHeight() {
		return 64;
	}

	public void plot(PaintEvent e, int y, int width, int height) {
		ImageData buffer = this.buffer.get();
		if(buffer == null || buffer.width != width || buffer.height != height) {
			buffer = new ImageData(width, height, 24, new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
			this.buffer.set(buffer);
			this.plotX = 0;
		}
		int   x     = plotX;
		Image image = new Image(e.display, buffer);
		e.gc.drawImage(image, 
				0,                0, x, buffer.height, 
				buffer.width - x, y, x, buffer.height); 
		e.gc.drawImage(image, 
				x, 0, buffer.width - x, buffer.height, 
				0, y, buffer.width - x, buffer.height);
		e.gc.drawString(getGroupLabel(), 0, y, true);
		image.dispose();
	}

	/**
	 * Advances the current plot by one column and clears the current column.
	 */
	public void clear() {
		clear(RGB.BLACK);
	}

	/**
	 * Advances the current plot by one column and clears the current column.
	 * 
	 * @param color The clear color.
	 */
	public void clear(RGB color) {
		ImageData buffer = this.buffer.get();
		if(buffer == null) return;
		if(!(pausePlot)) plotX++;
		if(plotX >= buffer.width) plotX = 0;
		int off= plotX * 3;
		byte br = (byte) (color.r*255);
		byte bg = (byte) (color.g*255);
		byte bb = (byte) (color.b*255);
		for(int i = 0; i < buffer.height; i++) {
			buffer.data[off+0] = br;
			buffer.data[off+1] = bg;
			buffer.data[off+2] = bb;
			off += buffer.bytesPerLine;
		}
	}

	float[] tmp = ClassUtilities.EMPTY_floatA;
	/**
	 * Plots an int vector as a column.
	 * 
	 * @param values The values to plot.
	 * @param min The lower bound of the value range.
	 * @param max The upper bound of the value range (inclusive).
	 * @param color The color to multiplied by the value. 
	 */
	public void column(int[] values, int min, int max, RGB color) {
		if(tmp.length != values.length)
			tmp = new float[values.length];
		for(int i = 0; i < values.length; i++)
			tmp[i] = values[i];
		column(tmp, min, max, color);
	}

	/**
	 * Plots a float vector as a column.
	 * 
	 * @param values The values to plot.
	 * @param color The color to multiplied by the value. 
	 */
	public void column(float[] values, RGB color) {
		column(values, 0, 1, color);
	}


	private static float map(float v, float min, float max) {
		return MathUtilities.clamp(MathUtilities.map(v, min, max, 0, 1), 0, 1);
	}

	/**
	 * Plots a float vector as a column.
	 * 
	 * @param values The values to plot.
	 * @param min The lower bound of the value range.
	 * @param max The upper bound of the value range (inclusive).
	 * @param color The color to multiplied by the value. 
	 */
	public void column(float[] values, float min, float max, RGB color) {
		ImageData buffer = this.buffer.get();
		if(buffer == null) return;
		int off = plotX * 3;
		for(int i = 0; i < buffer.height; i++) {
			final float v = map(values[(values.length*(buffer.height-(i+1)))/buffer.height], min, max);
			buffer.data[off+0] = (byte) (v*color.r*255);
			buffer.data[off+1] = (byte) (v*color.g*255);
			buffer.data[off+2] = (byte) (v*color.b*255);
			off += buffer.bytesPerLine;
		}
	}	

	/**
	 * Plots a float vector as points.
	 * 
	 * @param values The values to plot.
	 * @param color The color to multiplied by the value. 
	 */
	public void points(float[] values, RGB color) {
		points(values, 0, 1, color);
	}

	/**
	 * Plots a float vector as a points.
	 * 
	 * @param values The values to plot.
	 * @param min The lower bound of the value range.
	 * @param max The upper bound of the value range (inclusive).
	 * @param color The color to multiplied by the value. 
	 */	public void points(float[] values, float min, float max, RGB color) {
		 ImageData buffer = this.buffer.get();
		 if(buffer == null) return;
		 for(int i = 0; i < values.length; i++) {
			 float y = (i  + map(values[i], min, max)) / values.length;
			 int off = (plotX * 3) + ((int)((1-y) * buffer.height)) * buffer.bytesPerLine;
			 if(off < 0 || off >= buffer.data.length -3) return;
			 buffer.data[off++] = (byte) (color.r*255);
			 buffer.data[off++] = (byte) (color.g*255);
			 buffer.data[off++] = (byte) (color.b*255);
		 }
	 }

	 /**
	  * Plots a float vector as a bars.
	  * 
	  * @param values The values to plot.
	  * @param color The color to multiplied by the value. 
	  */
	 public void bars(float[] values, RGB color) {
		 bars(values, 0, 1, color);
	 }

	 /**
	  * Plots a float vector as a bars.
	  * 
	  * @param values The values to plot.
	  * @param min The lower bound of the value range.
	  * @param max The upper bound of the value range (inclusive).
	  * @param color The color to multiplied by the value. 
	  */	
	 public void bars(float[] values, float min, float max, RGB color) {
		 ImageData buffer = this.buffer.get();
		 if(buffer == null) return;
		 byte br = (byte) (color.r*255);
		 byte bg = (byte) (color.g*255);
		 byte bb = (byte) (color.b*255);
		 for(int i = 0; i < values.length; i++) {
			 float y     = map(values[i], min, max);
			 int   count = (int)(y * buffer.height) / values.length;
			 y = (i + y) / values.length;
			 int off = (plotX * 3) + ((int)((1-y) * buffer.height)) * buffer.bytesPerLine;
			 if(off < 0 || off >= buffer.data.length -3) return;
			 if(count < 0 || off < 0) return;
			 for(int j = 0; j < count && off < buffer.data.length; j++) {
				 buffer.data[off+0] = br;
				 buffer.data[off+1] = bg;
				 buffer.data[off+2] = bb;
				 off += buffer.bytesPerLine;
			 }
		 }
	 }

	 /**
	  * Plots a single point.
	  * 
	  * @param y The value to plot.
	  * @param color The color to multiplied by the value. 
	  */
	 public void point(float y, RGB color) {
		 point(y, 0, 1, color);
	 }

	 /**
	  * Plots a single point.
	  * 
	  * @param value The value to plot.
	  * @param min The lower bound of the value range.
	  * @param max The upper bound of the value range (inclusive).
	  * @param color The color to multiplied by the value. 
	  */
	 public void point(float value, float min, float max, RGB color) {
		 ImageData buffer = this.buffer.get();
		 if(buffer == null) return;
		 value = map(value, min, max);
		 int off = (plotX * 3) + ((int)((1-value) * buffer.height)) * buffer.bytesPerLine;
		 if(off < 0 || off >= buffer.data.length -3) return;
		 buffer.data[off++] = (byte) (color.r*255);
		 buffer.data[off++] = (byte) (color.g*255);
		 buffer.data[off++] = (byte) (color.b*255);
	 }	

	 /**
	  * Plots a value as a bar.
	  * 
	  * @param y The value to plot.
	  * @param color The color to multiplied by the value. 
	  */
	 public void bar(float y, RGB color) {
		 ImageData buffer = this.buffer.get();
		 if(buffer == null) return;
		 int off = (plotX * 3) + ((int)((1-y) * buffer.height)) * buffer.bytesPerLine;
		 int count = (int)(y * buffer.height);
		 if(count < 0 || off < 0) return;
		 byte br = (byte) (color.r*255);
		 byte bg = (byte) (color.g*255);
		 byte bb = (byte) (color.b*255);
		 for(int i = 0; i < count && off < buffer.data.length; i++) {
			 buffer.data[off+0] = br;
			 buffer.data[off+1] = bg;
			 buffer.data[off+2] = bb;
			 off += buffer.bytesPerLine;
		 }
	 }

	public void setPausePlot(boolean pause) {
		this.pausePlot = pause;
	}	
}
