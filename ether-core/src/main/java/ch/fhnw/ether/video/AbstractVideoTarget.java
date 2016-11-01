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

import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.image.IImage.AlphaMode;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.ether.media.AbstractMediaTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.ether.video.fx.IVideoCPUFX;
import ch.fhnw.ether.video.fx.IVideoGLFX;

public abstract class AbstractVideoTarget extends AbstractMediaTarget<VideoFrame, IVideoRenderTarget> implements IVideoRenderTarget {
	private final Class<?> preferredType;

	protected AbstractVideoTarget(int threadPriority, Class<?> preferredType, boolean realTime) {
		super(threadPriority, realTime);
		this.preferredType = preferredType;
	}

	@Override
	public void useProgram(RenderProgram<IVideoRenderTarget> program) throws RenderCommandException {
		AbstractRenderCommand<?>[] cmds = program.getProgram();
		int numGlFx    = 0;
		int numFrameFx = 0;
		for(int i = 1; i < cmds.length; i++) {
			if(!(cmds[i] instanceof AbstractVideoFX))
				throw new ClassCastException("Command '" + cmds[i] + "' not sublcass of " + AbstractVideoFX.class.getName());
			if(cmds[i] instanceof IVideoCPUFX)
				numFrameFx++;
			if(cmds[i] instanceof IVideoGLFX)
				numGlFx++;
		}
		if(numGlFx == cmds.length - 1 && preferredType == AbstractVideoFX.GLFX) {}
		else if(numFrameFx == cmds.length - 1 && preferredType == AbstractVideoFX.CPUFX) {}
		else if(numGlFx == 0 && numFrameFx == 0) {}
		else
			throw new IllegalArgumentException("All commands must implement either " + IVideoGLFX.class.getName() + " or " + IVideoCPUFX.class.getName());
		super.useProgram(program);
	}

	@Override
	public IVideoSource getVideoSource() {
		return (IVideoSource)program.getFrameSource();
	}

	public IGPUImage getSrcTexture(AbstractVideoFX fx) {
		AbstractRenderCommand<?>[] cmds = program.getProgram();
		for(int i = cmds.length; --i >= 1;)
			if(cmds[i] == fx)
				for(int j = i; --j >= 0;)
					if(cmds[j].isEnabled())
						return j == 0 ? getFrame().getGPUImage() : getDstTexture((AbstractVideoFX)cmds[j]);
		return null;
	}

	public IGPUImage getDstTexture(AbstractVideoFX fx) {
		AbstractRenderCommand<?>[] cmds = program.getProgram();
		IVideoSource               src  = (IVideoSource) cmds[0];
		if(cmds[cmds.length - 1] == fx || fx.getDstTexture() == null)
			return IGPUImage.create(src.getWidth(), src.getHeight(), ComponentType.BYTE, ComponentFormat.RGBA, AlphaMode.POST_MULTIPLIED);
		return fx.getDstTexture();
	}

	public Class<?> runAs() {
		return preferredType;
	}
}
