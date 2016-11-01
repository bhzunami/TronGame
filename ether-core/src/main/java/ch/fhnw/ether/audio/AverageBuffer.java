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
 */package ch.fhnw.ether.audio;

 import java.util.Arrays;

 public final class AverageBuffer {
	 private float[] values;
	 private int     index;
	 private double  sum = 0.0;
	 private final   float sRate;
	 private final   int   numChannels;

	 public AverageBuffer(float sampleRate, int numChannels, double sizeInSeconds) {
		 this.numChannels = numChannels;
		 this.sRate       = sampleRate;
		 this.values      = new float[(int) (sampleRate * numChannels * sizeInSeconds)];
		 this.index       = 0;
	 }

	 public void push(double value) {
		 push((float)value);
	 }

	 public void push(float value) {
		 sum = sum - values[index];
		 values[index] = value;
		 sum = sum + value;
		 index = (index + 1) % values.length;
	 }

	 public void push(float value, int count) {
		 for(int i = 0; i < count; i++) {
			 sum = sum - values[index];
			 values[index] = value;
			 sum = sum + value;
			 index = (index + 1) % values.length;
		 }
	 }

	 public void push(float[] valuesArray) {
		 for(float value : valuesArray) {
			 sum = sum - values[index];
			 values[index] = value;
			 sum = sum + value;
			 index = (index + 1) % values.length;
		 }
	 }

	 public void fill(float value) {
		 push(value, values.length);
	 }

	 public float peek(int off) {
		 return values[((index - 1) + values.length + off) % values.length];
	 }

	 public float peek(double offInSeconds) {
		 return peek((int)(sRate * offInSeconds * numChannels));
	 }

	 public float getAverage() {
		 return (float) (sum / values.length);
	 }

	 public int getSize() {
		 return this.values.length;
	 }

	 public double getSizeInSeconds() {
		 return values.length / (sRate * numChannels);
	 }

	 public void setSize(double sizeInSeconds) {
		 setSize((int)(sRate * numChannels * sizeInSeconds));
	 }

	 public void setSize(int size) {
		 if (size == this.values.length)
			 return;
		 float[] old = values;
		 float average = getAverage();
		 values = new float[size];
		 // Copy as much history as possible
		 int min = (size < old.length ? size : old.length);
		 int offset = size - min;
		 for (int i = 0; i < min; i++)
			 values[i + offset] = old[(index + i) % old.length];
		 // Fill too old history with avergae
		 for (int i = 0; i < offset; i++)
			 values[i] = average;
		 // Recompute sum
		 sum = 0.0f;
		 for (int i = 0; i < size; i++)
			 sum+= values[i];
		 // Reset index
		 index = 0;
	 }

	 public void reset() {
		 Arrays.fill(values, 0f);
		 index = 0;
		 sum = 0;
	 }
 }
