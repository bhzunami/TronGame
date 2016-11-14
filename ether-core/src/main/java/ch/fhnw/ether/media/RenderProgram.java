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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import ch.fhnw.ether.audio.IAudioSource;
import ch.fhnw.ether.midi.IMidiSource;
import ch.fhnw.ether.video.IVideoSource;
import ch.fhnw.util.ArrayUtilities;
import ch.fhnw.util.CollectionUtilities;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.IdentityHashSet;
import ch.fhnw.util.Log;

public class RenderProgram<T extends IRenderTarget<?>> extends AbstractRenderCommand<T> {
	private static Log log = Log.create();

	private final AtomicReference<T> target = new AtomicReference<>();

	static class Update {
		final AbstractRenderCommand<?> oldCmd;
		final AbstractRenderCommand<?> newCmd;
		final boolean                  first;

		Update(AbstractRenderCommand<?> oldCmd, AbstractRenderCommand<?> newCmd, boolean first) {
			this.oldCmd = oldCmd;
			this.newCmd = newCmd;
			this.first  = first;
		}

		boolean isAdd() {
			return newCmd != null && oldCmd == null;
		}

		boolean isReplace() {
			return newCmd != null && oldCmd != null;
		}

		boolean isRemove() {
			return newCmd == null && oldCmd != null;
		}

		void add(List<AbstractRenderCommand<?>> program) {
			if(first)
				program.add(0, newCmd);
			else
				program.add(newCmd);
		}

		void replace(List<AbstractRenderCommand<?>> program) {
			program.set(program.indexOf(oldCmd), newCmd);
		}

		void remove(List<AbstractRenderCommand<?>> program) {
			program.remove(oldCmd);
		}
	}

	private final AtomicReference<AbstractRenderCommand<T>[]> program   = new AtomicReference<>();

	@SuppressWarnings("unchecked")
	@SafeVarargs
	public RenderProgram(IAudioSource source, AbstractRenderCommand<T> ... commands) {
		program.set(ArrayUtilities.prepend((AbstractRenderCommand<T>)source, commands));
	}

	@SuppressWarnings("unchecked")
	@SafeVarargs
	public RenderProgram(IVideoSource source, AbstractRenderCommand<T> ... commands) {
		program.set(ArrayUtilities.prepend((AbstractRenderCommand<T>)source, commands));
	}

	@SuppressWarnings("unchecked")
	@SafeVarargs
	public RenderProgram(IMidiSource source, AbstractRenderCommand<T> ... commands) {
		program.set(ArrayUtilities.prepend((AbstractRenderCommand<T>)source, commands));
	}

	private Update createAddFirst(AbstractRenderCommand<T> cmd) {
		return new Update(null, cmd, true);
	}

	private Update createAddLast(AbstractRenderCommand<T> cmd) {
		return new Update(null, cmd, false);
	}

	private Update createRemove(AbstractRenderCommand<T> cmd) {
		return new Update(cmd, null, false);
	}

	private Update createReplace(AbstractRenderCommand<T> oldCmd, AbstractRenderCommand<T> newCmd) {
		return new Update(oldCmd, newCmd, false);
	}

	public void addFirst(AbstractRenderCommand<T> cmd) {
		update(createAddFirst(cmd));
	}

	public void addLast(AbstractRenderCommand<T> cmd) {
		update(createAddLast(cmd));
	}

	public void remove(AbstractRenderCommand<T> cmd) {
		update(createRemove(cmd));
	}

	@SuppressWarnings("unchecked")
	public void replace(AbstractFrameSource source) {
		update(createReplace(program.get()[0], (AbstractRenderCommand<T>)source)) ; 
	}

	public void replace(AbstractRenderCommand<T> oldCmd, AbstractRenderCommand<T> newCmd) {
		update(createReplace(oldCmd, newCmd)) ; 
	}

	public void update(Update ... updates) {
		List<AbstractRenderCommand<?>> tmp = new ArrayList<>(program.get().length + updates.length);
		CollectionUtilities.addAll(tmp, program.get());

		for(Update update : updates) {
			if(update.isAdd())
				update.add(tmp);
			if(update.isReplace())
				update.replace(tmp);
			if(update.isRemove())
				update.remove(tmp);
		}

		setProgram(tmp);
	}

	@SuppressWarnings("unchecked")
	private synchronized void setProgram(List<AbstractRenderCommand<?>> program) {
		AbstractRenderCommand<T>[] oldProgram = this.program.get(); 
		AbstractRenderCommand<T>[] newProgram = program.toArray(new AbstractRenderCommand[program.size()]);

		final IdentityHashSet<AbstractRenderCommand<T>> removed = new IdentityHashSet<>(oldProgram);		
		removed.removeAll(new IdentityHashSet<>(newProgram));

		new Thread(()->{
			try {
				// Give all targets some time to switch to the changed program
				// before disposing any commands. Very hacky...
				Thread.sleep(500);
			} catch (Throwable t) {}
			for(AbstractRenderCommand<T> cmd : removed) {
				try {
					if(cmd instanceof IDisposable)
						((IDisposable)cmd).dispose();
				} catch(Throwable t) {
					log.warning(t);
				}
			}
		}, "disposing:" + removed).start();

		T t = target.get();
		if(t != null) {
			for(AbstractRenderCommand<T> command : newProgram) {
				try {command.init(t);} catch(Throwable e) {log.severe(e);};
			}
		}

		this.program.set(newProgram);
	}

	protected void run() throws RenderCommandException {
		AbstractRenderCommand<T>[] commands = program.get(); 
		for(AbstractRenderCommand<T> command : commands)
			if(!(command.isSkip()))
				command.runInternal(target.get());
	}


	public AbstractRenderCommand<T>[] getProgram() {
		return program.get();
	}

	public AbstractFrameSource getFrameSource() {
		AbstractRenderCommand<T>[] commands = program.get(); 
		for(AbstractRenderCommand<T> command : commands)
			if(command instanceof AbstractFrameSource)
				return (AbstractFrameSource)command;
		return null;
	}

	public void setTarget(T target) throws RenderCommandException {
		if(target != null && this.target.get() != null)
			throw new RenderCommandException("Cannot replace target '" + this.target + "'  by '" + target + "'");
		this.target.set(target);
		AbstractRenderCommand<T>[] commands = program.get(); 
		for(AbstractRenderCommand<T> command : commands)
			command.init(target);
	}

	@Override
	protected void run(T target) throws RenderCommandException {
		run();
	}

	@Override
	protected void init(T target) throws RenderCommandException {
		setTarget(target);
	}

	public T getTarget() {
		return target.get();
	}
}
