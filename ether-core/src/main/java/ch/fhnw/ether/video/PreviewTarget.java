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

package ch.fhnw.ether.video;

import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.IProgressListener;

public class PreviewTarget extends AbstractVideoTarget implements IDisposable {
	private static final int BORDER = 2;

	private IHostImage              preview;
	private boolean                 init = true;
	private int                     prvN;
	private int                     prvHeight;
	private int                     prvWidth;
	private int                     x;
	private double[]                startEnd;
	private double                  prvSkip;
	private int                     idx;
	private final IProgressListener progress;

	public PreviewTarget(int width, int height) {
		this(width, height, null);
	}
	
	public PreviewTarget(int width, int height, IProgressListener progress) {
		this(width, height, 0, AbstractFrameSource.LENGTH_UNKNOWN, progress);
	}

	public PreviewTarget(int width, int height, IProgressListener progress, double...durations) {
		this(startEnd(durations), width, height, progress);
	}

	private static double[] startEnd(double[] durations) {
		double[] result = new double[durations.length*2];
		double   start  = 0;
		int      idx    = 0;
		for(double d : durations) {
			result[idx++] = start;
			start += d;
			result[idx++] = start;
		}
		return result;
	}

	public PreviewTarget(int width, int height, double startInSeconds, double lengthInSeconds, IProgressListener progress) {
		this(new double[] {startInSeconds, startInSeconds+lengthInSeconds}, width, height, progress);
	}

	private PreviewTarget(double[] startEnd, int width, int height, IProgressListener progress) {
		super(Thread.MIN_PRIORITY, AbstractVideoFX.CPUFX, false);
		this.progress = progress;
		this.startEnd = startEnd;
		this.preview  = IHostImage.create(width, height, ComponentType.BYTE, ComponentFormat.RGBA);
	}
	
	@Override
	public void render() throws RenderCommandException {
		VideoFrame vframe = getFrame();
		IHostImage frame  = null;
		
		if(startEnd[1] == AbstractFrameSource.LENGTH_UNKNOWN) 
			startEnd[1] = getVideoSource().getLengthInSeconds();
		if(init) {
			init      = false;
			frame     = vframe.getHostImage();
			if(frame == null) return;
			prvHeight = preview.getHeight();
			prvWidth  = (prvHeight * frame.getWidth()) / frame.getHeight(); 
			prvN      = Math.max(preview.getWidth() / (prvWidth + BORDER), 1);
			prvSkip   = (startEnd[1]-startEnd[0]) / prvN;
		}
		
		while(vframe.playOutTime < startEnd[idx])
			return;
		
		frame = vframe.getHostImage();
		
		if(frame != null && x + prvWidth < preview.getWidth()) {
			preview.setSubImage(x, 0, frame.scale(prvWidth, prvHeight));
			x += prvWidth + BORDER;
			if(progress != null) progress.setProgress((float)x/(float)preview.getWidth());
		}
		
		if(vframe.playOutTime >= startEnd[idx+1]) {
			processPreview();
			idx += 2;
			if(idx >= startEnd.length)
				stop();
			else {
				prvSkip = (startEnd[idx+1]-startEnd[idx+0]) / prvN;
				x = 0;
			}
			if(progress != null) progress.done();
		} else
			startEnd[idx] += prvSkip;
	}

	protected void processPreview() throws RenderCommandException {}
	
	public IHostImage getPreview() {
		return preview;
	}

	@Override
	public void dispose() {
		preview.dispose();
	}	
}
