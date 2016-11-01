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

package ch.fhnw.ether.platform;

import ch.fhnw.ether.view.IWindow;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.math.Vec2;

public interface IPlatform {
	/**
	 * Initialize platform. Call this before doing anything else.
	 */
	void init();

	/**
	 * Run platform. Does not return.
	 */
	void run();

	/**
	 * Exit platform. Closes all windows and releases all resources.
	 */
	void exit();
	
	/**
	 * Returns true if called from main thread, false otherwise.
	 */
	boolean isMainThread();

	/**
	 * Execute task on main thread. Typically used for opening / closing
	 * windows, etc.
	 */
	void runOnMainThread(Runnable runnable);
	
	/**
	 * Create window.
	 */
	IWindow createWindow(Vec2 size, String title, boolean decorated);

	/**
	 * Create full screen window.
	 */
	IWindow createWindow(IMonitor monitor, String title);

	/**
	 * Get platform-dependent image support (image reading / writing / scaling).
	 */
	IImageSupport getImageSupport();
	
	/**
	 * Get the connected monitors.
	 */
	IMonitor[] getMonitors();
	
	/*
	 * Add a runnable which will be executed before the platform graphics resources are disposed.
	 */
	void addShutdownTask(Runnable r);
	
	/*
	 * Add a disposable which will be disposed before the platform graphics resources are disposed.
	 */
	void addShutdownDispose(IDisposable d);

	/*
	 * Removes a previously installed shutdown task (runnable or disposable).
	 */
	void removeShutdownTask(Object o);
}
