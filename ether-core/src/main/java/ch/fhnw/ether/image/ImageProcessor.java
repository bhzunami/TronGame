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

package ch.fhnw.ether.image;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ImageProcessor {
	private static final ExecutorService POOL = Executors.newCachedThreadPool();
	private static final int NUM_CHUNKS = Runtime.getRuntime().availableProcessors();

	private ImageProcessor() {
		
	}
	
	private static final class Chunk implements Runnable {
		private final int from;
		private final int to;
		private final ByteBuffer pixels;
		private final ILineProcessor processor;
		private final int lineLength;

		Chunk(IHostImage image, int from, int to, ILineProcessor processor) {
			this.from = from;
			this.to = to;
			this.pixels = image.getPixels().duplicate();
			this.processor = processor;
			this.lineLength = image.getWidth() * image.getNumBytesPerPixel();
		}

		@Override
		public void run() {
			for (int j = from; j < to; j++) {
				pixels.position(j * lineLength);
				processor.process(pixels, j);
			}
		}
	}

	public static void processLines(IHostImage image, ILineProcessor processor) {
		List<Future<?>> result = new ArrayList<>(NUM_CHUNKS + 1);
		int inc = Math.max(32, image.getHeight() / NUM_CHUNKS);
		for (int from = 0; from < image.getHeight(); from += inc)
			result.add(POOL.submit(new Chunk(image, from, Math.min(from + inc, image.getHeight()), processor)));
		try {
			for (Future<?> f : result)
				f.get();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
