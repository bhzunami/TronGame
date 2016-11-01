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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;

public abstract class AbstractMediaTarget<F extends AbstractFrame, T extends IRenderTarget<F>> implements IRenderTarget<F> {
	private static final Log log = Log.create();

	private   final int                     priority;
	private   final boolean                 realTime;
	protected RenderProgram<T>              program;
	protected ITimebase                     timebase;
	protected final AtomicBoolean           isRendering  = new AtomicBoolean();
	private   final AtomicReference<F>      frame        = new AtomicReference<>();
	private         CountDownLatch          startLatch;
	private   final List<BlockingTimeEvent> timeEvents   = new LinkedList<>();
	private   long                          startTime;
	private   Thread                        framePump;
	private   long                          totalFrames;
	private   long                          relFrames;

	protected AbstractMediaTarget(int threadPriority, boolean realTime) {
		this.priority = threadPriority;
		this.realTime = realTime;
	}

	@Override
	public void start() {
		if(program == null) throw new NullPointerException("No program set for this target");
		if(program.getFrameSource().getLengthInFrames() == 1) {
			try {
				setRendering(true);
				runOneCycle();
			} catch(Throwable e) {
				log.severe(e);
			}
			setRendering(false);
		} else {
			startLatch = new CountDownLatch(1);
			framePump = new Thread(()->{
				try {
					setRendering(true);
					startLatch.countDown();
					while(isRendering())
						runOneCycle();
				} catch(Throwable e) {
					setRendering(false);
					log.severe(e);
				}
			}, TextUtilities.getShortClassName(this));
			framePump.setDaemon(true);
			framePump.setPriority(priority);
			framePump.start();
			try {
				startLatch.await();
				startTime = System.nanoTime();
			} catch (InterruptedException e) {
				log.severe(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void runOneCycle() throws RenderCommandException {
		program.runInternal((T)this);
		final AbstractFrame tmp = getFrame();
		if(tmp != null) {
			render();
			if(tmp.isLast())
				setRendering(false);
			tmp.dispose();
		}
		synchronized (timeEvents) {
			for(final Iterator<BlockingTimeEvent> i = timeEvents.iterator(); i.hasNext();) {
				final BlockingTimeEvent e = i.next();
				if(e.time == NOT_RENDERING) {
					if(!isRendering()) {
						e.unblock();
						i.remove();
					}
				} else if(e.time >= getTime()) {
					e.unblock();
					i.remove();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void useProgram(RenderProgram<T> program) throws RenderCommandException {
		if(this.program == program) return;
		this.program = program;	
		program.setTarget((T)this);
	}

	@Override
	public void stop() throws RenderCommandException {
		setRendering(false);
	}

	@Override
	public void render() throws RenderCommandException {}

	@Override
	public final boolean isRendering() {
		return isRendering.get();
	}

	protected final void setRendering(boolean state) {
		isRendering.set(state);
		if(!(state)) {
			synchronized (timeEvents) {
				for(final Iterator<BlockingTimeEvent> i = timeEvents.iterator(); i.hasNext();) {
					BlockingTimeEvent e = i.next();
					e.unblock();
				}
				timeEvents.clear();
			}
		}
	}

	@Override
	public final void sleepUntil(double time) {
		sleepUntil(time, null);
	}

	public static void nap() {
		try {
			Thread.sleep(1);
		} catch (Throwable t) {
			log.warning(t);
		}
	}

	@Override
	public final void sleepUntil(double time, Runnable runnable) {
		if(time == ASAP) return;
		if(Thread.currentThread() == framePump)
			if(time == NOT_RENDERING) {
				while(isRendering())
					nap();
			} else {
				if(isRealTime()) {
					try {
						int sleep = (int)((time - getTime()) * 1000);
						if(sleep > 0)
							Thread.sleep(sleep);
					} catch (Throwable t) {
						log.warning(t);
					}
				}
				while(getTime() <= time)
					nap();
			}
		else if((time == NOT_RENDERING || time > getTime()) && isRendering()) {
			BlockingTimeEvent event = new BlockingTimeEvent(time, runnable);
			synchronized (timeEvents) {
				timeEvents.add(event);
			}
			event.sleep();
		}
	}

	@Override
	public final F getFrame() {
		return frame.get();
	}

	@Override
	public final void setFrame(AbstractFrameSource src, F frame) {
		this.frame.set(frame);
		long length = src.getLengthInFrames();
		if(length > 0 && getRelativeElapsedFrames() >= length)
			relFrames = 0;
		incFrameCount();
	}

	public void incFrameCount() {
		totalFrames++;
		relFrames++;
	}

	static final class BlockingTimeEvent {
		public  final double         time;
		private final CountDownLatch latch = new CountDownLatch(1);
		private final Runnable       callback;

		public BlockingTimeEvent(double time, Runnable callback) {
			this.time    = time;
			this.callback = callback;
		}

		public void unblock() {
			if(callback != null)
				callback.run();
			latch.countDown();
		}

		public void sleep() {
			try {
				latch.await();
			} catch(Throwable t) {
				AbstractMediaTarget.log.warning(t);
			}
		}
	}

	@Override
	public double getTime() {
		if(timebase != null) return timebase.getTime();
		if(realTime)
			return startTime == 0 ? 0 : (System.nanoTime() - startTime) / SEC2NS;

		AbstractFrameSource src = program.getFrameSource();
		long                len = src.getLengthInFrames();
		return getTotalElapsedFrames() / (len <= 0 ?  src.getFrameRate() : (len / src.getLengthInSeconds()));
	}

	@Override
	public final void setTimebase(ITimebase timebase) {
		this.timebase = timebase;
	}

	@Override
	public final long getTotalElapsedFrames() {
		return totalFrames;
	}

	@Override
	public final long getRelativeElapsedFrames() {
		return relFrames;
	}

	@Override
	public final boolean isRealTime() {
		return timebase == null ? realTime : timebase.isRealTime();
	}
	
	public RenderProgram<T> getProgram() {
		return program;
	}
}
