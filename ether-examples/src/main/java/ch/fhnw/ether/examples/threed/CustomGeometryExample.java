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

package ch.fhnw.ether.examples.threed;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.ether.view.DefaultView;
import ch.fhnw.ether.view.IView;
import ch.fhnw.util.color.RGBA;

public final class CustomGeometryExample {
	public static void main(String[] args) {
		new CustomGeometryExample();
	}
	
public static List<IMesh> makeColoredTriangle() {
		
		float[][] vertices = {
			{
				5, 5, 5, 
				0, 5, 0,
				5, 0, 0,
			},
			{
				5, 5, 5, 
				5, 10, 0,
				0, 5, 0,
			},
			{
				5, 5, 5, 
				10, 5, 0,
				5, 10, 0
			},
			{
				5, 5, 5, 
				5, 0, 0,
				10, 5, 0,
			},
		};
		float[][] colors = {
				{
					1, 0, 0, 1, 
					0, 1, 0, 1, 
					0, 0, 1, 1
				},
				{
					1, 0, 0, 1, 
					0, 1, 0, 1, 
					0, 0, 1, 1
				},
				{
					1, 0, 0, 1, 
					0, 1, 0, 1, 
					0, 0, 1, 1
				},
				{
					1, 0, 0, 1, 
					0, 1, 0, 1, 
					0, 0, 1, 1
				},
		};
		
		List<IMesh> meshes = new ArrayList<>();
		
		for(int i = 0; i < colors.length; i++) {
			DefaultGeometry g = DefaultGeometry.createVC(vertices[i], colors[i]);
			meshes.add(new DefaultMesh(Primitive.TRIANGLES, new ColorMaterial(new RGBA(colors[i])), g));
		}
		
		
		return meshes;
	}

	private static IMesh makeColoredTriangle(float off) {
		float[] vertices = { off + 0, 0, off + 0, 0, off + 0, 0.5f, off + 0.5f, 0, 0.5f };
		float[] colors = { 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1 };

		DefaultGeometry g = DefaultGeometry.createVC(vertices, colors);
		return new DefaultMesh(Primitive.TRIANGLES, new ColorMaterial(RGBA.WHITE, true), g, IMesh.Queue.DEPTH);
	}

	// Setup the whole thing
	public CustomGeometryExample() {
		// Init platform
		Platform.get().init();
		
		// Create controller
		IController controller = new DefaultController();
		controller.run(time -> {
			// Create view
			new DefaultView(controller, 100, 100, 500, 500, IView.INTERACTIVE_VIEW, "Test");

			// Create scene and add triangle
			IScene scene = new DefaultScene(controller);
			controller.setScene(scene);

			scene.add3DObjects(makeColoredTriangle());
			//scene.add3DObject(makeColoredTriangle(1));
		});
		
		Platform.get().run();
	}
}
