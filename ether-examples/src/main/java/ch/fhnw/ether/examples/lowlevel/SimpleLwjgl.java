package ch.fhnw.ether.examples.lowlevel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.lwjgl.opengl.GL11;

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.ether.view.IWindow.IContext;
import ch.fhnw.util.math.Vec2;

public class SimpleLwjgl {
	
	private final IWindow window;
	
	public static void main(String[] args) {
		new SimpleLwjgl();
	}

	public SimpleLwjgl() {
		Platform.get().init();
		
		window = IWindow.create(new Vec2(500, 500), "OpenGL Window", true);
		window.setVisible(true);
		
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(this::renderer, 0, 100, TimeUnit.MILLISECONDS);
		
		Platform.get().run();
	}
	
	void renderer() {
		if (window.isDisposed())
			return;

		try (IContext ctx = window.acquireContext()) {
			GL11.glClearColor(0, 0, 0, 1);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

			Vec2 v = window.getFramebufferSize();
			GL11.glViewport(0, 0, (int) v.x, (int) v.y);

		} catch (Exception e) {

		}

		window.swapBuffers();
	}
}
