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

package ch.fhnw.ether.render.gl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.lwjgl.glfw.GLFW;

import ch.fhnw.ether.view.IWindow;
import ch.fhnw.ether.view.IWindow.IContext;
import ch.fhnw.util.math.Vec2;

// decide whether we should really use ExistingContext or be strict 
// that there is no current context when a temporary one is requested. 
// (problem is: this could lead to nested contexts and corresponding state issues.
public class GLContextManager {
	private static final boolean DBG = true;

	private static final int NUM_CONTEXTS = 4;

	private static final class ExistingContext implements IContext {
		@Override
		public void close() throws Exception {
			if (DBG)
				GLError.checkWithMessage("temporary void context release");
		}
	}

	private static final class TemporaryContext implements IContext {
		IWindow window;

		TemporaryContext() {
			window = IWindow.create(new Vec2(16, 16), null, false);
		}

		void acquire() {
			window.acquireContext();
			if (DBG)
				GLError.checkWithMessage("temporary real context acquire");
		}

		void release() {
			if (DBG)
				GLError.checkWithMessage("temporary real context release");
			window.releaseContext();
		}

		@Override
		public void close() throws Exception {
			releaseContext(this);
		}
	}

	private static class ContextPool {
		final BlockingQueue<TemporaryContext> contexts = new LinkedBlockingQueue<>();

		public ContextPool() {
			contexts.add(new TemporaryContext());
		}

		TemporaryContext acquireContext(boolean wait) {
			TemporaryContext context = null;
			if (wait) {
				try {
					context = contexts.take();
				} catch (InterruptedException e) {
				}
			} else {
				context = contexts.poll();
			}
			if (context != null)
				context.acquire();
			return context;
		}

		void releaseContext(TemporaryContext context) {
			context.release();
			contexts.add(context);
		}
	}

	private static final IContext VOID_CONTEXT = new ExistingContext();

	private static ContextPool contexts;

	private static IWindow sharedContext;

	public static void init() {
		if (contexts == null) {
			contexts = new ContextPool();
			sharedContext = contexts.contexts.peek().window;
			for (int i = 0; i < NUM_CONTEXTS - 1; ++i)
				contexts.contexts.add(new TemporaryContext());
		}
	}

	public static IContext acquireContext() {
		return acquireContext(true);
	}

	public static IContext acquireContext(boolean wait) {
		if (GLFW.glfwGetCurrentContext() != 0)
			return VOID_CONTEXT;
		return contexts.acquireContext(wait);
	}

	public static void releaseContext(IContext context) {
		if (context instanceof TemporaryContext)
			contexts.releaseContext((TemporaryContext) context);
	}

	public static IWindow getSharedContextWindow() {
		return sharedContext;
	}
}
