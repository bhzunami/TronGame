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

package org.corebounce.resman;

import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.video.AbstractVideoTarget;
import ch.fhnw.ether.video.VideoFrame;
import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.IProgressListener;

public abstract class AtlasTarget extends AbstractVideoTarget implements IDisposable {
	private IHostImage              current;
	private boolean                 init = true;
	private int                     tileW;
	private int                     tileH;
	private final int               maxW;
	private final int               maxH;
	private int                     x;
	private int                     y;
	private long                    count;
	private int                     idx;
	private final IProgressListener progress;
	
	public AtlasTarget(int width, int height, IProgressListener progress) {
		super(Thread.MIN_PRIORITY, AbstractVideoFX.CPUFX, false);
		this.progress = progress;
		this.maxW     = width;
		this.maxH     = height;
	}

	@Override
	public void render() throws RenderCommandException {
		VideoFrame vframe = getFrame();
		IHostImage frame  = null;

		if(init) {
			long numFrames = getVideoSource().getLengthInFrames();
			init      = false;
			frame     = vframe.getHostImage();
			if(frame == null) return;
			tileW = frame.getWidth();
			tileH = frame.getHeight();
			if(tileW > maxW) {
				tileH = (tileH * maxW) / tileW; 
				tileW = maxW;
			}
			if(tileH > maxH) {
				tileW = (tileH * maxH) / tileH; 
				tileH = maxH;
			}
			int count = Math.min(maxW / tileW, maxH / tileH);
			count = (int)Math.min(count, Math.ceil(Math.sqrt(numFrames)));
			current = IHostImage.create(maxW, maxH, ComponentType.BYTE, ComponentFormat.RGB);
		}

		frame = vframe.getHostImage();

		if(frame != null) {
			if(vframe.isLast()) {
				writeImage(idx++, current);
				if(progress != null) progress.done();
			} else {
				current.setSubImage(x, y, frame.scale(tileW,tileH));
				count++;
				if(progress != null) progress.setProgress((float)count/(float)getVideoSource().getLengthInFrames());
				x += tileW;
				if(x > current.getWidth()-tileW) {
					x = 0;
					y += tileH;
					if(y > current.getHeight()-tileH) {
						y = 0;
						writeImage(idx++, current);
						current.clear();
					}
				}
			}
		}		
	}

	public int getTileWidth() {
		return tileW;
	}

	public int getTileHeight() {
		return tileH;
	}

	public int getWidth() {
		return current.getWidth();
	}

	public int getHeight() {
		return current.getWidth();
	}

	@Override
	public void dispose() {
		current.dispose();
	}

	public long getNumTiles() {
		return count;
	}

	protected abstract void writeImage(int idx, IHostImage image);

	public long[] getShotStarts() {
		return getVideoSource().getShotStarts();
	}

	/*
	public static void main(String[] args) throws MalformedURLException, IOException, RenderCommandException {
		IVideoSource src = new URLVideoSource(new File(args[0]).toURI().toURL(), 1, 64);
		System.out.println(src);
		RenderProgram<IVideoRenderTarget> program = new RenderProgram<>(src);
		AtlasTarget target  = new AtlasTarget(8192, 8192);
		target.useProgram((RenderProgram<IVideoRenderTarget>)program);
		target.start();
		target.sleepUntil(IScheduler.NOT_RENDERING);
		System.out.println("num images:" + target.getAtlas().length);
		System.out.println("TILE_W:" + target.getTileWidth());
		System.out.println("TILE_H:" + target.getTileHeight());
		System.out.println("TILE_N:" + target.getNumTiles());
		target.dispose();
	}
	 */
}
