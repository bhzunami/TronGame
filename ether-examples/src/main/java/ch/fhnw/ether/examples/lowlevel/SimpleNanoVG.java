package ch.fhnw.ether.examples.lowlevel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL11;

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.ether.view.IWindow.IContext;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Vec2;

public class SimpleNanoVG {
	interface INVGRenderCommand {
		void render(long context);
	}
	
	class Rectangle {
		private Vec2 position;
		private Vec2 dimension;
		private NVGColor color; // we better use RGBA here (immutable) and then use a renderer-side buffer for temp value
		
		private INVGRenderCommand command;
		
		Rectangle(Vec2 position, Vec2 dimension, RGBA color) {
			this.position = position;
			this.dimension = dimension;
			this.color = NVGColor.create();
			setColor(color);
		}
		
		public Vec2 getPosition() {
			return position;
		}
		
		public void setPosition(Vec2 position) {
			this.position = position;
			invalidate();
		}
		
		public Vec2 getDimension() {
			return dimension;
		}
		
		public void setDimension(Vec2 dimension) {
			this.dimension = dimension;
			invalidate();
		}
		
		public RGBA getColor() {
			return new RGBA(color.r(), color.g(), color.b(), color.a());
		}
		
		public void setColor(RGBA color) {
			NanoVG.nvgRGBAf(color.r, color.g, color.b, color.a, this.color);
			invalidate();
		}
		
		private void invalidate() {
			command = null;
		}

		public INVGRenderCommand getRenderCommand() {
			if (command == null) {
				command = new INVGRenderCommand() {
					Vec2 position = Rectangle.this.position;
					Vec2 dimension = Rectangle.this.dimension;
					NVGColor color = Rectangle.this.color; // XXX not safe!
	
					@Override
					public void render(long context) {
						NanoVG.nvgBeginPath(context);
						NanoVG.nvgRect(context, position.x, position.y, dimension.x, dimension.y);
						NanoVG.nvgFillColor(context, color);
						NanoVG.nvgFill(context);
					}
				};
			}
			return command;
		}
	}
	
	private final IWindow window;
	private final Rectangle rectangle;
	private final BlockingQueue<INVGRenderCommand> queue = new LinkedBlockingQueue<>();
	
	public static void main(String[] args) {
		new SimpleNanoVG();
	}

	public SimpleNanoVG() {
		Platform.get().init();
		
		window = IWindow.create(new Vec2(500, 500), "NanoVG Window", true);
		window.setVisible(true);
		
		rectangle = new Rectangle(new Vec2(50, 50), new Vec2(100, 100), RGBA.RED);
		
		Thread renderer = new Thread(this::renderer);
		renderer.setDaemon(true);
		renderer.start();
		
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(this::scheduler, 0, 100, TimeUnit.MILLISECONDS);
		
		Platform.get().run();
	}
	
	void scheduler() {
		rectangle.setPosition(rectangle.getPosition().add(Vec2.ONE));
		submit(rectangle.getRenderCommand());
	}
	
	void submit(INVGRenderCommand command) {
		queue.offer(command);
	}

	void renderer() {
		long nvgContext = 0;
		NVGColor color = null;
		
		try {
			for (;;) {
				INVGRenderCommand command = queue.take();
				if (window.isDisposed())
					return;

				try (IContext ctx = window.acquireContext()) {
					GL11.glClearColor(0, 0, 0, 1);
					GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
	
					Vec2 v = window.getFramebufferSize();
	
					GL11.glViewport(0, 0, (int) v.x, (int) v.y);
	
					if (nvgContext == 0) {
						nvgContext = NanoVGGL3.nnvgCreateGL3(0);
						color = NVGColor.create();
						NanoVG.nvgRGBAf(1f, 0.8f, 0f, 1f, color);
					}
					NanoVG.nvgBeginFrame(nvgContext, (int) v.x, (int) v.y, 1);
					command.render(nvgContext);
					NanoVG.nvgEndFrame(nvgContext);
				} catch (Exception e) {
					
				}
				
				window.swapBuffers();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
