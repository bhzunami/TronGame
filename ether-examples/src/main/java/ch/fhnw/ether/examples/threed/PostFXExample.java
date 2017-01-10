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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.formats.IModelReader.Options;
import ch.fhnw.ether.formats.obj.ObjReader;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.render.shader.IShader;
import ch.fhnw.ether.render.shader.base.AbstractPostShader;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.light.DirectionalLight;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.AbstractMaterial;
import ch.fhnw.ether.scene.mesh.material.ICustomMaterial;
import ch.fhnw.ether.view.DefaultView;
import ch.fhnw.ether.view.IView;
import ch.fhnw.ether.view.IView.ViewType;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.math.Vec3;

public final class PostFXExample {
	public static final class PostMaterial extends AbstractMaterial implements ICustomMaterial {
		private static class PostShader extends AbstractPostShader {
			public PostShader() {
				super(PostFXExample.class, "post_fx_example.post_shader", "/shaders/post_shader", Primitive.TRIANGLES);
			}
		}

		private final IShader shader = new PostShader();

		public PostMaterial() {
			super(provide(), require(IGeometry.POSITION_ARRAY, IGeometry.COLOR_MAP_ARRAY));
		}

		@Override
		public IShader getShader() {
			return shader;
		}

		@Override
		public Object[] getData() {
			return data();
		}
	}

	public static void main(String[] args) {
		new PostFXExample();
	}

	public PostFXExample() {
		Platform.get().init();
		
		IController controller = new DefaultController();
		controller.run(time -> {
			System.out.println("test");
			
			new DefaultView(controller, 100, 100, 500, 500, new IView.Config(ViewType.INTERACTIVE_VIEW, 0, new IView.ViewFlag[0]), "Test");

			IScene scene = new DefaultScene(controller);
			controller.setScene(scene);
			
			scene.add3DObject(new DirectionalLight(new Vec3(-1, -1, 1), RGB.BLACK, RGB.WHITE));
			
			URL obj;
			try {
				obj = new File("fhnw.obj").toURI().toURL();
				final List<IMesh> meshes = new ArrayList<>();
					new ObjReader(obj, Options.CONVERT_TO_Z_UP).getMeshes().forEach(mesh -> meshes.add(mesh));
				final List<IMesh> merged = MeshUtilities.mergeMeshes(meshes);
				scene.add3DObjects(merged);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
//			scene.add3DObject(MeshUtilities.createCube());

			scene.add3DObject(MeshUtilities.createQuad(new PostMaterial(), Queue.POST, IMesh.NO_FLAGS));
		});
		
		Platform.get().run();
	}
}
