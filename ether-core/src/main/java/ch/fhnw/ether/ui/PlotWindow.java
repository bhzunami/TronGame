package ch.fhnw.ether.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.platform.Platform;

public class PlotWindow implements PaintListener, MouseListener {
	private Canvas                         canvasUI;
	private Shell                          shell;
	private final AbstractRenderCommand<?> cmd;

	public PlotWindow(final AbstractRenderCommand<?> cmd) {
		this.cmd = cmd;
		Platform.get().runOnMainThread(new Runnable() {
			@Override
			public void run() {
				shell = new Shell(Display.getDefault());
				shell.setText("Plot");
				shell.setLayout(new FillLayout());
				canvasUI = new Canvas(shell, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
				canvasUI.addPaintListener(PlotWindow.this);
				canvasUI.addMouseListener(PlotWindow.this);
				canvasUI.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));
				shell.pack();
				shell.setSize(800, shell.getSize().y);
				shell.setVisible(true);
				new Repeating(500, 20, canvasUI, canvasUI::redraw);
			}
		});
	}

	@Override
	public void paintControl(PaintEvent e) {
		if(cmd instanceof RenderProgram<?>) {
			RenderProgram<?> prog = (RenderProgram<?>)cmd;
			if(!(prog.getTarget().isRendering())) {
				shell.dispose();
				return;
			}
		}
		int h = shell.getSize().y - shell.getClientArea().height;
		shell.setSize(shell.getSize().x, h + plot(cmd, e, 0));
	}

	private int plot(AbstractRenderCommand<?> cmd, PaintEvent e, int height) {
		if(cmd instanceof RenderProgram<?>) {
			for(AbstractRenderCommand<?> c : ((RenderProgram<?>)cmd).getProgram())
				height = plot(c, e, height);
		} else if(cmd instanceof IPlotable) {
			IPlotable p = (IPlotable)cmd;
			cmd.plot(e, height, canvasUI.getSize().x, p.getPlotHeight());
			height += p.getPlotHeight();
		}
		return height;
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {}

	@Override
	public void mouseDown(MouseEvent e) {
		setPausePlot(cmd, true);
	}

	@Override
	public void mouseUp(MouseEvent e) {	
		setPausePlot(cmd, false);
	}	

	private void setPausePlot(AbstractRenderCommand<?> cmd, boolean pause) {
		if(cmd instanceof RenderProgram<?>) {
			for(AbstractRenderCommand<?> c : ((RenderProgram<?>)cmd).getProgram())
				setPausePlot(c, pause);
		} else if(cmd instanceof IPlotable)
			cmd.setPausePlot(pause);
	}
}