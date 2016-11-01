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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;

import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.Log;

// XXX currently defunct because of AWT dependencies
public class CameraSource extends AbstractFrameSource implements IVideoSource, IDisposable, WebcamListener {
	private static final Log LOG = Log.create();

	private static final AtomicBoolean kill   = new AtomicBoolean();
	private static final int           Q_SIZE = 3;             
	
	private final Webcam                       cam;
	private AtomicBoolean                      disposed = new AtomicBoolean(false);
	private final CameraInfo                   info;
	private final BlockingQueue<BufferedImage> imgQ = new LinkedBlockingQueue<>(Q_SIZE);

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			try {
				Thread.sleep(1000);
				kill.set(true);
			} catch (InterruptedException e) {}
		}));
		new Thread("Camera watchdog") {
			@Override
			public void run() {
				try {
					while(!(kill.get()))
						Thread.sleep(1000);
					Runtime.getRuntime().halt(0);
				} catch (InterruptedException e) {}
			}
		}.start();
	}

	private CameraSource(CameraInfo info) {
		this.info     = info;
		this.cam      = info.getNativeCamera();
		this.cam.open(true);
		Dimension max = cam.getViewSize();
		for(Dimension dim : this.cam.getViewSizes())
			if(dim.width > max.width && dim.height > max.height)
				max = dim;
		setSize(max.width, max.height);
		cam.addWebcamListener(this);
	}

	@Override
	public void dispose() {
		if(!(disposed.getAndSet(true)))
			cam.close();
	}

	@Override
	protected void run(IRenderTarget<?> target) throws RenderCommandException {
		if(!(cam.isOpen())) return;
		try {
			// XXX will not work with lwjgl
			// ((IVideoRenderTarget)target).setFrame(this, new VideoFrame(AWTImageSupport.createFrame(imgQ.take())));
		} catch(Throwable t) {
			throw new RenderCommandException(t);
		}
	}

	public void setSize(int width, int height) {
		cam.close();
		cam.setViewSize(new Dimension(width, height));
		cam.open(true);
	}

	@Override
	public float getFrameRate() {
		return (float) cam.getFPS();
	}

	@Override
	public long getLengthInFrames() {
		return FRAMECOUNT_UNKNOWN;
	}

	@Override
	public double getLengthInSeconds() {
		return LENGTH_INFINITE;
	}

	@Override
	public int getWidth() {
		return cam.getViewSize().width;
	}

	@Override
	public int getHeight() {
		return cam.getViewSize().height;
	}

	//--- utilities

	@Override
	public String toString() {
		return info.toString();
	}

	@Override
	protected void finalize() throws Throwable {
		dispose();
		super.finalize();
	}

	public static CameraSource create(CameraInfo cameraInfo) {
		return new CameraSource(cameraInfo);
	}

	@Override
	public void webcamImageObtained(WebcamEvent event) {
		try {
			if(imgQ.size() == Q_SIZE) imgQ.poll();
			imgQ.put(event.getImage());
		} catch (Throwable t) {
			LOG.severe(t);
		}
	}

	@Override
	public void webcamClosed(WebcamEvent arg0) {}

	@Override
	public void webcamDisposed(WebcamEvent arg0) {}


	@Override
	public void webcamOpen(WebcamEvent arg0) {}

	@Override
	public long[] getShotStarts() {
		return ClassUtilities.EMPTY_longA;
	}
}
