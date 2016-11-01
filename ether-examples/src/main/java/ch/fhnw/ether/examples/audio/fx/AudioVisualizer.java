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

package ch.fhnw.ether.examples.audio.fx;


import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.IAudioSource;
import ch.fhnw.ether.audio.JavaSoundTarget;
import ch.fhnw.ether.audio.URLAudioSource;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.BandsButterworth;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.audio.fx.PitchDetect;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.ui.ParameterWindow;
import ch.fhnw.util.TextUtilities;

public class AudioVisualizer {
	private static final int N_CUBES = 60;

	public static void main(String[] args) throws RenderCommandException, IOException {
		IAudioSource                      src = new URLAudioSource(new File(args[0]).toURI().toURL());
		DCRemove                          dcrmv = new DCRemove();
		AutoGain                          gain  = new AutoGain();
		FFT                               fft   = new FFT(20, Window.HANN);
		BandsButterworth                  bands = new BandsButterworth(40, 8000, 40, N_CUBES, 1);
		PitchDetect                       pitch = new PitchDetect(fft, 2);

		final JavaSoundTarget audioOut = new JavaSoundTarget();

		Display display = new Display();
	    Shell shell = new Shell(display);
		shell.setText("Audio Visualizer");
		shell.setLayout(new FillLayout());
		Canvas c = new Canvas(shell, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		c.addPaintListener(e->{
			Rectangle b = c.getBounds();
			e.gc.fillRectangle(b);
			Color bc = e.gc.getBackground();
			try{
				e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_BLUE));
				int w = b.width / N_CUBES;
				for(int i = 0; i < N_CUBES; i++) {
					int h = (int) (bands.power(i) * b.height);
					e.gc.fillRectangle(i * w, b.height - h, w, h);
				}
				e.gc.setBackground(bc);
				e.gc.drawText(TextUtilities.toString(pitch.pitch()), 0, 20);
			} catch(Throwable t) {
				e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_RED));
				e.gc.fillRectangle(b);
			}
			c.redraw();
		});
		shell.open();
		
		RenderProgram<IAudioRenderTarget> audio = new RenderProgram<>(src, /*sin,*/ dcrmv, gain, fft, bands, pitch /*, robo, ifft*/);

		new ParameterWindow(audio);

		audioOut.useProgram(audio);
		audioOut.start();
		
		while (!shell.isDisposed()) {
		      if (!display.readAndDispatch()) {
		        display.sleep();
		      }
		    }
		    display.dispose();
	}
}
