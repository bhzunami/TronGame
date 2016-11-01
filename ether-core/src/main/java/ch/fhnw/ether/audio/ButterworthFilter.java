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

public class ButterworthFilter {
    private static final float sqrt2 = (float)Math.sqrt(2D);
    private final float a1;
    private final float a2;
    private final float a3;
    private final float a4;
    private final float a5;
    private float prev1;
    private float prev2;

    private ButterworthFilter(float a1, float a2, float a3, float a4, float a5) {
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.a4 = a4;
        this.a5 = a5;
    }

    public void process(float in[], float out[]) {
        for(int i = 0; i < in.length; i++) {
            float b2 = in[i] - a4 * prev1 - a5 * prev2;
            float b3 = b2 * a1 + a2 * prev1 + a3 * prev2;
            prev2 = prev1;
            prev1 = b2;
            out[i] = b3;
        }

    }

    public void processBand(float buffer[]) {
        for(int i = 0; i < buffer.length; i++) {
            float b2 = buffer[i] - a4 * prev1 - a5 * prev2;
            float b3 = (b2 - prev2) * a1;
            prev2 = prev1;
            prev1 = b2;
            buffer[i] = b3;
        }

    }

    public void processReversed(float in[], float out[]) {
        if(in.length != out.length)
            throw new IllegalArgumentException();
        for(int i = 0; i < in.length; i++) {
            float b2 = in[i] - a4 * prev1 - a5 * prev2;
            float b3 = b2 * a1 + a2 * prev1 + a3 * prev2;
            prev2 = prev1;
            prev1 = b2;
            out[i] = in[i] - b3;
        }

    }

    public float process(float sample) {
        float b2 = sample - a4 * prev1 - a5 * prev2;
        float b3 = b2 * a1 + a2 * prev1 + a3 * prev2;
        prev2 = prev1;
        prev1 = b2;
        return b3;
    }

    public void processWeighted(float in[], float out[], float begFactor, float endFactor) {
        float inc = (endFactor - begFactor) / in.length;
        for(int i = 0; i < in.length; i++)
        {
            float b2 = in[i] - a4 * prev1 - a5 * prev2;
            float b3 = b2 * a1 + a2 * prev1 + a3 * prev2;
            prev2 = prev1;
            prev1 = b2;
            out[i] += b3 * begFactor;
            begFactor += inc;
        }

    }

    public static ButterworthFilter getLowpassFilter(double sampleRate, double frequency) {
        float b0 = (float)(1.0D / Math.tan((Math.PI * frequency) / sampleRate));
        float a1 = 1.0F / (1.0F + sqrt2 * b0 + b0 * b0);
        float a2 = a1 + a1;
        float a3 = a1;
        float a4 = 2.0F * (1.0F - b0 * b0) * a1;
        float a5 = ((1.0F - sqrt2 * b0) + b0 * b0) * a1;
        ButterworthFilter engine = new ButterworthFilter(a1, a2, a3, a4, a5);
        return engine;
    }

    public static ButterworthFilter getHighpassFilter(double sampleRate, double frequency) {
        float b0 = (float)Math.tan((Math.PI * frequency) / sampleRate);
        float a1 = 1.0F / (1.0F + sqrt2 * b0 + b0 * b0);
        float a2 = -(a1 + a1);
        float a3 = a1;
        float a4 = 2.0F * (b0 * b0 - 1.0F) * a1;
        float a5 = ((1.0F - sqrt2 * b0) + b0 * b0) * a1;
        ButterworthFilter engine = new ButterworthFilter(a1, a2, a3, a4, a5);
        return engine;
    }

    public static ButterworthFilter getBandpassFilter0(double sampleRate, double frequency, double bandWidth) {
        if(bandWidth < 1.0)
            bandWidth = 1.0;
        float b0 = (float)(1.0 / Math.tan((Math.PI * bandWidth) / sampleRate));
        float b1 = (float)(2.0 * Math.cos((2 * Math.PI * frequency) / sampleRate));
        float a1 = 1.0F / (1.0F + b0);
        float a2 = 0.0F;
        float a3 = -a1;
        float a4 = -b0 * b1 * a1;
        float a5 = (b0 - 1.0F) * a1;
        ButterworthFilter engine = new ButterworthFilter(a1, a2, a3, a4, a5);
        return engine;
    }

    private static ButterworthFilter getBandrejectFilter0(double sampleRate, double frequency, double bandWidth) {
        if(bandWidth < 1.0)
            bandWidth = 1.0;
        float b0 = (float)Math.tan((Math.PI * bandWidth) / sampleRate);
        float b1 = (float)(2.0 * Math.cos((2 * Math.PI * frequency) / sampleRate));
        float a1 = 1.0F / (1.0F + b0);
        float a2 = -b1 * a1;
        float a3 = a1;
        float a4 = a2;
        float a5 = (1.0F - b0) * a1;
        ButterworthFilter engine = new ButterworthFilter(a1, a2, a3, a4, a5);
        return engine;
    }

    public static ButterworthFilter getBandpassFilter(double sampleRate, double lower, double upper) {
        double center = Math.exp((Math.log(lower) + Math.log(upper)) / 2.0);
        double width = Math.abs(upper - lower);
        return getBandpassFilter0(sampleRate, center, width);
    }

    public static ButterworthFilter getBandrejectFilter(double sampleRate, double lower, double upper) {
        double center = Math.exp((Math.log(lower) + Math.log(upper)) / 2.0);
        double width = Math.abs(upper - lower);
        return getBandrejectFilter0(sampleRate, center, width);
    }

    public void copyHistory(ButterworthFilter other) {
        prev1 = other.prev1;
        prev2 = other.prev2;
    }

    public void setHistory(float p1, float p2) {
        prev1 = p1;
        prev2 = p2;
    }
}
