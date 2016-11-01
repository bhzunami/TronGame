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

package ch.fhnw.ether.controller.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.fhnw.ether.media.ITimebase;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.util.Pair;

public final class DefaultEventScheduler implements IEventScheduler {

	private static final long START_TIME = System.nanoTime();

	private final Runnable runnable;

	private final double interval;

	private final Thread schedulerThread;

	private final List<IAnimationAction> animations = new ArrayList<>();
	private final List<Pair<Double, IAction>> actions = new ArrayList<>();

	private final AtomicBoolean repaint = new AtomicBoolean();

	private ITimebase timebase;
	private AtomicBoolean running = new AtomicBoolean(true);

	public DefaultEventScheduler(Runnable runnable, float fps) {
		this.runnable = runnable;
		interval = 1 / fps;
		schedulerThread = new Thread(this::runSchedulerThread, "scheduler-thread");
		schedulerThread.setDaemon(true);
		schedulerThread.setPriority(Thread.MAX_PRIORITY);
		schedulerThread.start();
	}

	@Override
	public void animate(IAnimationAction action) {
		synchronized (animations) {
			animations.add(action);
		}
	}

	@Override
	public void kill(IAnimationAction action) {
		synchronized (animations) {
			animations.remove(action);
		}
	}

	@Override
	public void run(IAction action) {
		synchronized (actions) {
			actions.add(new Pair<>(0.0, action));
		}
	}

	@Override
	public void run(double delay, IAction action) {
		synchronized (actions) {
			actions.add(new Pair<>(getTime() + delay, action));
		}
	}

	@Override
	public void repaint() {
		repaint.set(true);
	}

	@Override
	public boolean isSchedulerThread() {
		return Thread.currentThread().equals(schedulerThread);
	}

	private void runSchedulerThread() {
		while (running.get()) {
			double time = getTime();

			// run actions first
			{
				List<Pair<Double, IAction>> aa;
				synchronized (actions) {
					aa = new ArrayList<>();
					for (Pair<Double, IAction> p : actions) {
						if (time > p.first)
							aa.add(p);
					}
					if (!aa.isEmpty())
						actions.removeAll(aa);
				}
				for (Pair<Double, IAction> a : aa) {
					try {
						a.second.run(time);
					} catch (Exception e) {
						e.printStackTrace();
					}
					repaint.set(true);
				}
			}

			// run animations second
			{
				List<IAnimationAction> aa;
				synchronized (animations) {
					aa = new ArrayList<>(animations);
				}
				for (IAnimationAction a : aa) {
					try {
						a.run(time, interval);
					} catch (Exception e) {
						e.printStackTrace();
					}
					repaint.set(true);
				}
			}

			if (repaint.getAndSet(false)) {
				try {
					runnable.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			double elapsed = getTime() - time;
			double remaining = interval - elapsed;
			if (remaining > 0) {
				try {
					//System.out.println("sleep for " + remaining);
					Thread.sleep((long) (remaining * 1000));
				} catch (Exception e) {
				}
			} else {
				System.err.println("scheduler: scene thread overload (max=" + s2ms(interval) + "ms used=" + s2ms(elapsed) + "ms)");
			}
		}
	}

	@Override
	public double getTime() {
		if(timebase != null) return timebase.getTime();
		long elapsed = System.nanoTime() - START_TIME;
		return elapsed / ITimebase.SEC2NS;
	}

	private int s2ms(double time) {
		return (int)(time * 1000);
	}

	@Override
	public void setTimebase(ITimebase timebase) {
		this.timebase = timebase;
	}

	@Override
	public boolean isRendering() {
		return running.get();
	}

	@Override
	public void start() throws RenderCommandException {
		// do nothing
	}

	@Override
	public void stop() throws RenderCommandException {
		running.set(false);
	}

	@Override
	public void sleepUntil(double timeInSec) {
		try {
			if(timeInSec == ASAP) return;
			else if(timeInSec == NOT_RENDERING) {
				while(isRendering())
					Thread.sleep(1);
			} else {
				while(timeInSec < getTime())
					Thread.sleep(1);
			}
		} catch (Throwable t) {
			LOG.warning(t);
		}
	}

	@Override
	public void sleepUntil(double timeInSec, Runnable runnable) {
		if(timeInSec == ASAP) runnable.run();
		else if(timeInSec == NOT_RENDERING) {
			sleepUntil(timeInSec);
			runnable.run();
		} else {
			CountDownLatch latch = new CountDownLatch(1);
			run(timeInSec - getTime(), time->{
				runnable.run();
				latch.countDown();
			});
			try {
				latch.await();
			} catch (Throwable t) {
				LOG.warning(t);
			}
		}
	}
	
	@Override
	public boolean isRealTime() {
		return timebase == null ? true : timebase.isRealTime();
	}
}
