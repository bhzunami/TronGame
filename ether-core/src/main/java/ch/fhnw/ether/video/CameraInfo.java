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

package ch.fhnw.ether.video;

import java.awt.Dimension;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;

import ch.fhnw.util.math.Vec2;

public class CameraInfo {
	private final Webcam cam;
	
	private CameraInfo(Webcam cam) {
		this.cam = cam;
	}
	
	public Vec2[] getSupportedSizes() {
		Dimension[] sizes = cam.getViewSizes();
		Vec2[] result = new Vec2[sizes.length];
		for(int i = 0; i < sizes.length; i++)
			result[i] = new Vec2(sizes[i].width, sizes[i].height);
		return result;
	}
	
	public static CameraInfo[] getInfos() {
		try {
			List<Webcam> cams = Webcam.getWebcams(5000);
			CameraInfo[] result = new CameraInfo[cams.size()];
			int idx = 0;
			for(Webcam cam : cams)
				result[idx++] = new CameraInfo(cam);
			return result;
		} catch (WebcamException | TimeoutException e) {
			return new CameraInfo[0];
		}
	}

	public Webcam getNativeCamera() {
		return cam;
	}
	
	@Override
	public String toString() {
		return cam.getName();
	}
}
