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

package ch.fhnw.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Set;

public class AutoDisposer<T> extends Thread {
	private static final Log LOG = Log.create();

	private static final boolean DBG = false;
	
	protected ReferenceQueue<T>                   refQ   = new ReferenceQueue<>();
	protected IdentityHashSet<Reference<T>>       refSet = new IdentityHashSet<>();
	private   Class<? extends Reference<T>>       refCls;
	private   Constructor<? extends Reference<T>> ctor;

	public AutoDisposer(Class<? extends Reference<T>> refCls) {
		super("AutoDisposer for " + refCls.getName());
		this.refCls = refCls;
		setDaemon(true);
		setPriority(Thread.MIN_PRIORITY);
		start();
	}

	@SuppressWarnings("unchecked")
	public synchronized void add(T object) {
		try {
			if(ctor == null) {
				for(Constructor<?> ctor : refCls.getDeclaredConstructors()) {
					Class<?>[] argTypes = ctor.getParameterTypes();
					if(argTypes.length == 2 
							&& argTypes[0].isAssignableFrom(object.getClass()) 
							&& argTypes[1].isAssignableFrom(refQ.getClass())) {
						this.ctor = (Constructor<? extends Reference<T>>) ctor;
						break;
					}
				}
				if(ctor == null)
					throw new IllegalArgumentException("No reference constructor found in " + refCls.getName());
			}
			
			Reference<T> ref = ctor.newInstance(object, refQ);
			refSet.add(ref);

			if(DBG) System.out.println("add:" + ref);
		} catch(Throwable t) {
			LOG.severe(t);
		}
	}

	@Override
	public void run() {
		for(;;) {
			try {
				doDispose((Reference<?>)refQ.remove());
			} catch (Throwable t) {
				LOG.severe(t);
			}
		}
	}
	
	public synchronized void doDispose() {
		if(DBG) System.out.println("*** " + refCls.getName() + ": doDispose(" + refSet.size() + ")");
		for(;;) {
			Reference<?> ref = (Reference<?>) refQ.poll();
			if(ref == null)
				break;

			doDispose(ref);
		}
		if(DBG) System.out.println("+++ " + refCls.getName() + ": doDispose");
	}

	private void doDispose(Reference<?> ref) {
		if(DBG) System.out.println("dispose:" + ref);
		ref.dispose();
		refSet.remove(ref);
	}

	public abstract static class Reference<T> extends WeakReference<T> {
		String label;
		
		public Reference(T referent, ReferenceQueue<? super T> q) {
			super(referent, q);
			try {
				label = referent.toString();
			} catch(Throwable t) {}
		}

		public abstract void dispose();

		@Override
		public String toString() {
			return getClass().getName() + ":" + ClassUtilities.identityHashCode(this) + ":" + label;
		}
	}

	public Set<Reference<T>> getRefSet() {
		return refSet;
	}

	public static void runGC() {
		for(int i = 0; i < 5; i++) {
			long before = Runtime.getRuntime().freeMemory();
			System.gc();
			Runtime.getRuntime().runFinalization();
			System.gc();
			long after  = Runtime.getRuntime().freeMemory();
			if(after >= before)
				break;
		}		
	}	
}
