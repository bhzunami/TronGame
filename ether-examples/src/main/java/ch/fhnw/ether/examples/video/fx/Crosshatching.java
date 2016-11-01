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

package ch.fhnw.ether.examples.video.fx;

import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.ether.video.fx.IVideoGLFX;

public class Crosshatching extends AbstractVideoFX implements IVideoGLFX {
	@Override
	public String mainFrag() {
		return lines(
				"const float hatch_y_offset  = 5.0;",
				"const float lum_threshold_1 = 1.0;",
				"const float lum_threshold_2 = 0.7;",
				"const float lum_threshold_3 = 0.5;",
				"const float lum_threshold_4 = 0.3;",
				"vec3 tc = vec3(1.0, 0.0, 0.0);",
				"float lum = length(result.rgb);",
				"tc = vec3(1.0, 1.0, 1.0);",
				"if (lum < lum_threshold_1) {",
				"  if (mod(gl_FragCoord.x + gl_FragCoord.y, 10.0) == 0.0)", 
				"    tc = vec3(0.0, 0.0, 0.0);",
				"}",
				"if (lum < lum_threshold_2) {",
				"  if (mod(gl_FragCoord.x - gl_FragCoord.y, 10.0) == 0.0)", 
				"    tc = vec3(0.0, 0.0, 0.0);",
				"}",
				"if (lum < lum_threshold_3) {",
				"  if (mod(gl_FragCoord.x + gl_FragCoord.y - hatch_y_offset, 10.0) == 0.0)", 
				"    tc = vec3(0.0, 0.0, 0.0);",
				"}",
				"if (lum < lum_threshold_4) {",
				"  if (mod(gl_FragCoord.x - gl_FragCoord.y - hatch_y_offset, 10.0) == 0.0)", 
				"    tc = vec3(0.0, 0.0, 0.0);",
				"}",
				"result = vec4(tc, 1.0);"
				);
	}
}
