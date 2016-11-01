package ch.fhnw.ether.platform;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public class GLFWMonitor implements IMonitor {
	private long handle;
	private int  index;
	
	public GLFWMonitor(long handle, int index) {
		this.handle = handle;
		this.index  = index;
	}

	@Override
	public int getX() {
		int[] xpos = new int[1];
		int[] ypos = new int[1];
		GLFW.glfwGetMonitorPos(handle, xpos, ypos);
		return xpos[0];
	}

	@Override
	public int getY() {
		int[] xpos = new int[1];
		int[] ypos = new int[1];
		GLFW.glfwGetMonitorPos(handle, xpos, ypos);
		return ypos[0];
	}

	@Override
	public int getWidth() {
		GLFWVidMode vm = GLFW.glfwGetVideoMode(handle);
		return vm.width();
	}

	@Override
	public int getHeight() {
		GLFWVidMode vm = GLFW.glfwGetVideoMode(handle);
		return vm.height();
	}
	
	public long getHandle() {
		return handle;
	}
	
	@Override
	public int getIndex() {
		return index;
	}
	
	@Override
	public String toString() {
		return GLFW.glfwGetMonitorName(handle) + " (" + getX() + "," + getY() + ") " + getWidth() + "x" + getHeight();
	}
}
