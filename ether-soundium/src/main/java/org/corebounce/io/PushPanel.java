package org.corebounce.io;

import java.util.Arrays;
import java.util.BitSet;

import org.corebounce.engine.Bouncelet;
import org.corebounce.engine.Engine;
import org.corebounce.engine.IEngineListener;
import org.corebounce.soundium.TabPanel;
import org.corebounce.ui.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import ch.fhnw.util.color.RGB;

public class PushPanel extends TabPanel implements PaintListener, IEngineListener, MouseListener {
	private       Canvas      canvas;
	private final Bouncelet[] bouncelets = new Bouncelet[64];
	private final BitSet      selection  = new BitSet(bouncelets.length);
	private final Engine      engine;
	private       FontData    fontData;
	private       Font        font;
	private       int         lastHeight;

	public PushPanel(Engine engine) {
		super("Push");
		this.engine = engine;
		engine.addEngineListener(this);
	}

	@Override
	protected void fillContent(Composite panel) {
		if(canvas != null)
			canvas.dispose();
		canvas = new Canvas(panel, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		canvas.setLayoutData(GridDataFactory.fill(true, true));
		canvas.addPaintListener(this);
		canvas.addMouseListener(this);
	}

	@Override
	public void paintControl(PaintEvent e) {
		Display display = e.display;
		e.gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		Rectangle bounds = canvas.getBounds();
		if(lastHeight != bounds.height) {
			if(font != null) font.dispose();
			fontData = display.getSystemFont().getFontData()[0];
			fontData.setHeight(bounds.height / 50);
			font = new Font(display, fontData);
		}
		e.gc.setFont(font);
		e.gc.fillRectangle(bounds);
		int pwidth  = (bounds.width - 4) / 8;
		int pheight = (bounds.height -4) / 8;
		int xoff    = (bounds.width  - pwidth  * 8) / 2;
		int yoff    = (bounds.height - pheight * 8) / 2;
		for(int y = 0; y < 8; y++) {
			for(int x = 0; x < 8; x++) {
				int id = y*8+x;
				Bouncelet b = bouncelets[id]; 
				if(b != null && b.isDisposed()) b = null;
				e.gc.setBackground(Engine.rgb2color(display, b == null ? RGB.GRAY10 : b.getColor()));
				int px = xoff+x*pwidth;
				int py = yoff+y*pheight;
				e.gc.fillRoundRectangle(px, py, pwidth - 2, pheight - 2, 4, 4);
				if(selection.get(id)) {
					e.gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
					e.gc.drawRoundRectangle(px, py, pwidth - 2, pheight - 2, 4, 4);
					e.gc.drawRoundRectangle(px-1, py-1, pwidth, pheight, 6, 6);
				}
				if(b != null) {
					e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
					e.gc.drawText(b.getLabel(), px+14, py+2);
					e.gc.drawText(b.getType(),  px+14, py+pheight-(int)(fontData.getHeight()*1.5f));
					int actHeight = pheight - 6;
					e.gc.setBackground(Engine.rgb2color(display, RGB.GRAY40));
					e.gc.fillRoundRectangle(px+2, py+2, 8, actHeight, 4, 4);
					e.gc.setBackground(Engine.rgb2color(display, RGB.GRAY90));
					e.gc.fillRoundRectangle(px+2, py+2+actHeight-((int)(actHeight * b.getActive())), 8, (int)(actHeight * b.getActive()), 4, 4);
					e.gc.drawRoundRectangle(px+2, py+2, 8, actHeight, 4, 4);
				} else {
					e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
					e.gc.drawText(Integer.toString(y*8+x), px+14, py+2);
				}
			}
		}
	}

	@Override
	public void bounceletsChanged(Bouncelet[] bouncelets) {
		Arrays.fill(this.bouncelets, null);
		for(Bouncelet b : bouncelets)
			if(b != null)
				this.bouncelets[b.id] = b;
		if(!(canvas.isDisposed()))
			canvas.redraw();
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {}

	@Override
	public void mouseDown(MouseEvent e) {
		Rectangle bounds = canvas.getBounds();
		int pwidth  = (bounds.width - 4) / 8;
		int pheight = (bounds.height -4) / 8;
		int xoff    = (bounds.width  - pwidth  * 8) / 2;
		int yoff    = (bounds.height - pheight * 8) / 2;
		int x = (e.x - xoff) / pwidth;
		int y = (e.y - yoff) / pheight;

		Bouncelet b = bouncelets[y*8+x];
		if(b != null) engine.select(b, false);
	}

	@Override
	public void mouseUp(MouseEvent e) {}

	@Override
	public void selectionChanged(Bouncelet[] selection) {
		this.selection.clear();
		for(Bouncelet b : selection)
			this.selection.set(b.id);
		if(!(canvas.isDisposed()))
			canvas.redraw();
	}
}
