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

package ch.fhnw.ether.audio;

import java.util.Arrays;
import java.util.LinkedList;

import ch.fhnw.ether.audio.AudioUtilities.Window;

public final class BlockBuffer {
	private LinkedList<float[]> blocks = new LinkedList<>();
	private float[]             c0;
	private int                 s0;
	private float[]             c1;
	private int                 s1;
	private final Window        windowType;

	public BlockBuffer(int blockSize, boolean halfOverlap, Window windowType) {
		this.windowType = windowType;
		this.c0         = new float[blockSize];
		this.s1         = blockSize / 2;
		if(halfOverlap)
			this.c1 = new float[blockSize];
	}

	public void add(float[] data) {
		for(int i = 0; i < data.length; i++) {
			c0[s0++] = data[i];
			if(s0 == c0.length) {
				push(c0);
				c0 = new float[c0.length];
				s0 = 0;
			}
			if(c1 != null) {
				c1[s1++] = data[i];
				if(s1 == c1.length) {
					push(c1);
					c1 = new float[c1.length];
					s1 = 0;
				}
			}
		}
	}

	private void push(float[] block) {
		AudioUtilities.applyWindow(windowType, 1, block);
		blocks.add(block);
	}

	public float[] nextBlock() {
		return blocks.isEmpty() ? null : blocks.remove(); 
	}

	public boolean nextBlockComplex(float[] block) {
		float[] b = nextBlock();
		if(b == null) return false;

		Arrays.fill(block, 0f);
		for(int i = 0; i < b.length; i++)
			block[i*2] = b[i];
		return true;
	}

	public void reset() {
		s0 = 0;
		s1 = c0.length / 2;
	}

	public int size() {
		return c0.length;
	}
}
