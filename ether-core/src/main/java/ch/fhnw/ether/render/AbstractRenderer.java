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

package ch.fhnw.ether.render;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

import org.lwjgl.opengl.GL11;

import ch.fhnw.ether.render.forward.ShadowVolumes;
import ch.fhnw.ether.render.gl.GLContextManager;
import ch.fhnw.ether.render.gl.GLError;
import ch.fhnw.ether.render.gl.Texture;
import ch.fhnw.ether.render.shader.IShader;
import ch.fhnw.ether.render.shader.base.AbstractPostShader;
import ch.fhnw.ether.scene.attribute.IAttribute;
import ch.fhnw.ether.scene.camera.IViewCameraState;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.view.IView;
import ch.fhnw.ether.view.IView.ViewFlag;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.ether.view.IWindow.IContext;
import ch.fhnw.util.Log;
import ch.fhnw.util.color.RGBA;

public abstract class AbstractRenderer implements IRenderer {
	private static final Log log = Log.create();

	public static final class RenderGlobals {
		public final Map<IAttribute, Supplier<?>> attributes = new IdentityHashMap<>();
		public final ViewInfo viewInfo = new ViewInfo();
		public final LightInfo lightInfo = new LightInfo();

		private RenderGlobals() {
			viewInfo.getAttributes(attributes);
			lightInfo.getAttributes(attributes);
		}
	}

	private static final int MAX_RENDER_QUEUE_SIZE = 3;

	private final Thread renderThread;
	private final BlockingQueue<Runnable> renderQueue = new ArrayBlockingQueue<>(MAX_RENDER_QUEUE_SIZE);

	protected final RenderGlobals globals = new RenderGlobals();

	private ShadowVolumes shadowVolumes;

	public AbstractRenderer() {
		this(true);
	}

	public AbstractRenderer(boolean startRenderThread) {
		if(startRenderThread) {
			renderThread = new Thread(this::runRenderThread, "render-thread");
			renderThread.setDaemon(true);
			renderThread.setPriority(Thread.MAX_PRIORITY);
			renderThread.start();
		} else
			renderThread = null;
	}

	@Override
	public ExecutionPolicy getExecutionPolicy() {
		return renderThread == null ? ExecutionPolicy.SINGLE_THREADED : ExecutionPolicy.DUAL_THREADED;
	}

	@Override
	public Renderable createRenderable(IMesh mesh) {
		return new Renderable(mesh, globals.attributes);
	}
	
	@Override
	public boolean ready() {
		if (getExecutionPolicy() == ExecutionPolicy.SINGLE_THREADED)
			return true;
		else 
			return renderQueue.size() < MAX_RENDER_QUEUE_SIZE;
	}

	@Override
	public void submit(IRenderState state) {
		try {
			if (getExecutionPolicy() == ExecutionPolicy.SINGLE_THREADED) {
				render(state);
			} else {
				renderQueue.put(() -> render(state));
			}
		} catch (Exception e) {
			log.warning(e);
		}
	}

	private void render(IRenderState renderState) {
		// update renderables (only once for all views)
		// note that it's absolutely imperative that this is executed for
		// every render runnable created. otherwise scene-render state will
		// get out of sync resulting in ugly fails.
		try (IContext ctx = GLContextManager.acquireContext()) {
			renderState.getRenderUpdates().forEach(update -> {
				update.update();
				GLError.checkWithMessage("updating");
			});
		} catch (Exception e) {
			log.warning(e);
		}

		// render all views
		renderState.getRenderStates().forEach(targetState -> {
			IView view = targetState.getView();
			IWindow window = view.getWindow();
			if (window == null)
				return;
			IViewCameraState vcs = targetState.getViewCameraState();
			try (IContext ctx = window.acquireContext()) {
				render(targetState, view, vcs);
				GLError.checkWithMessage("rendering");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		// swap buffers for all views
		renderState.getRenderStates().forEach(targetState -> {
			IView view = targetState.getView();
			IWindow window = view.getWindow();
			if (window == null)
				return;
			try {
				window.swapBuffers();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void render(IRenderTargetState renderState, IView view, IViewCameraState vcs) {
		// default gl state
		// FIXME: need to make this configurable and move to renderer
		RGBA clearColor = view.getConfig().getClearColor();
		GL11.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
		GL11.glClearDepth(1.0f);

		if (view.getConfig().has(ViewFlag.SMOOTH_LINES)) {
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
		}

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		// culling is enabled per default, and only disabled when requested
		GL11.glEnable(GL11.GL_CULL_FACE);

		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

		GL11.glViewport(0, 0, (int)view.getViewport().w, (int)view.getViewport().h);

		if (!view.isEnabled())
			return;

		// update views and lights
		globals.viewInfo.update(vcs);
		globals.lightInfo.update(vcs, renderState.getLights());

		// render everything
		render(renderState);
		//view.getWindow().swapBuffers();
	}

	protected abstract void render(IRenderTargetState state);

	protected void renderObjects(IRenderTargetState state, Queue pass) {
		for (Renderable renderable : state.getRenderables()) {
			if (renderable.getQueue() == pass) {
				renderable.render();
			}
		}
	}

	protected void renderPostObjects(IRenderTargetState state, Queue pass, Texture colorMap, Texture depthMap) {
		for (Renderable renderable : state.getRenderables()) {
			if (renderable.getQueue() == pass) {
				// XXX: set color/depth map to renderable's shader
				IShader shader = renderable.getShader();
				if (shader instanceof AbstractPostShader) {
					((AbstractPostShader)shader).setMaps(colorMap, depthMap);
					renderable.render();
				} else {
					// XXX warn?? fail? we should be able to test for this case much earlier in a flexible way
					renderable.render();
				}
			}
		}
	}
	
	protected void renderShadowVolumes(IRenderTargetState state, Queue pass) {
		if (shadowVolumes == null) {
			shadowVolumes = new ShadowVolumes(globals.attributes);
		}
		shadowVolumes.render(pass, state.getRenderables(), globals.lightInfo.getNumLights());
	}

	private void runRenderThread() {
		while (true) {
			try {
				renderQueue.take().run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	//	protected boolean isRenderThread() {
	//		return Thread.currentThread().equals(renderThread);
	//	}
}
