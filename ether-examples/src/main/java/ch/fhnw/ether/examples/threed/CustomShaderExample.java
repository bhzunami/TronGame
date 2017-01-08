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

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.render.shader.IShader;
import ch.fhnw.ether.render.shader.base.AbstractShader;
import ch.fhnw.ether.render.variable.base.FloatUniform;
import ch.fhnw.ether.render.variable.builtin.ColorArray;
import ch.fhnw.ether.render.variable.builtin.PositionArray;
import ch.fhnw.ether.render.variable.builtin.ViewUniformBlock;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.AbstractMaterial;
import ch.fhnw.ether.scene.mesh.material.ICustomMaterial;
import ch.fhnw.ether.view.DefaultView;
import ch.fhnw.ether.view.IView;
import ch.fhnw.ether.view.IView.ViewType;

public final class CustomShaderExample {
	static final class CustomMaterial extends AbstractMaterial implements ICustomMaterial {
		private static class Shader extends AbstractShader {
			public Shader() {
				super(CustomShaderExample.class, "custom_shader_example.custom_shader", "/shaders/custom_shader",
						Primitive.TRIANGLES);
				addArray(new PositionArray());
				addArray(new ColorArray());

				addUniform(new ViewUniformBlock());
				addUniform(new FloatUniform("custom.red_gain", "redGain"));
			}
		}

		private final IShader shader = new Shader();
		private float redGain;

		public CustomMaterial(float redGain) {
			super(provide(new MaterialAttribute<Float>("custom.red_gain")), require(IGeometry.POSITION_ARRAY));
			this.redGain = redGain;
		}

		public float getRedGain() {
			return redGain;
		}

		public void setRedGain(float redGain) {
			this.redGain = redGain;
			updateRequest();
		}

		@Override
		public IShader getShader() {
			return shader;
		}

		@Override
		public Object[] getData() {
			return data(redGain);
		}
	}

	public static void main(String[] args) {
		new CustomShaderExample();
	}

	private static IMesh makeColoredTriangle() {
		float[] vertices = {
				0, 0, 0, 
				0.5f, 0, 0.5f,
				0, 0, 0.5f };
		float[] colors = { 
				1, 0, 0, 1, 
				0, 1, 0, 1, 
				0, 0, 1, 1 
				};

		DefaultGeometry g = DefaultGeometry.createVC(vertices, colors);
		return new DefaultMesh(Primitive.TRIANGLES, new CustomMaterial(2f), g);
	}

	private IMesh mesh;

	// Setup the whole thing
	public CustomShaderExample() {
		// Init platform
		Platform.get().init();

		// Create controller
		IController controller = new DefaultController();
		controller.run(time -> {
			// Create view
			new DefaultView(controller, 100, 100, 500, 500,
					new IView.Config(ViewType.INTERACTIVE_VIEW, 0, new IView.ViewFlag[0]), "Test");

			// Create scene and add triangle
			IScene scene = new DefaultScene(controller);
			controller.setScene(scene);

			mesh = makeColoredTriangle();
			scene.add3DObject(mesh);
		});
		controller.animate((time, interval) -> {
			((CustomMaterial) mesh.getMaterial()).setRedGain((float) Math.sin(time) + 1);
		});

		Platform.get().run();
	}
}
