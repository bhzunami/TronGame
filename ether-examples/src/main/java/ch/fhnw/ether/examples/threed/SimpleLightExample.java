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

import java.util.List;

import org.lwjgl.glfw.GLFW;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.formats.obj.ObjReader;
import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.render.forward.ForwardRenderer;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.light.DirectionalLight;
import ch.fhnw.ether.scene.light.ILight;
import ch.fhnw.ether.scene.light.PointLight;
import ch.fhnw.ether.scene.light.SpotLight;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMaterial;
import ch.fhnw.ether.scene.mesh.material.IMaterial;
import ch.fhnw.ether.scene.mesh.material.LineMaterial;
import ch.fhnw.ether.scene.mesh.material.ShadedMaterial;
import ch.fhnw.ether.view.DefaultView;
import ch.fhnw.ether.view.IView;
import ch.fhnw.util.Log;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.GeodesicSphere;

public final class SimpleLightExample {
	private static final Log LOG = Log.create();

	private static final String[] HELP = { 
			//@formatter:off
			"Simple Light Example", 
			"", 
			"[1] Directional Light [2] Point Light [3] Spot Light",
			"Use cursors to move light position / direction x/y axis",
			"Use q/a to move light position / direction along z axis",
			"", 
			"Use Mouse Buttons + Shift or Mouse Wheel to Navigate" 
			//@formatter:on
	};

	public static void main(String[] args) {
		new SimpleLightExample();
	}

	private static final boolean ADD_BUNNY = true;

	private static final float INC_XY = 0.25f;
	private static final float INC_Z = 0.25f;
	private static final RGB AMBIENT = RGB.BLACK;
	private static final RGB COLOR = RGB.WHITE;

	private IController controller;
	private IScene scene;
	private ILight light = new DirectionalLight(Vec3.Z, AMBIENT, COLOR);
	private IMesh lightMesh;

	public SimpleLightExample() {
		// Init platform
		Platform.get().init();
		
		// Create controller
		controller = new DefaultController(new ForwardRenderer()) {
			@Override
			public void keyPressed(IKeyEvent e) {
				switch (e.getKey()) {
				case GLFW.GLFW_KEY_1:
					scene.remove3DObject(light);
					light = new DirectionalLight(light.getPosition(), AMBIENT, COLOR);
					scene.add3DObject(light);
					break;
				case GLFW.GLFW_KEY_2:
					scene.remove3DObject(light);
					light = new PointLight(light.getPosition(), AMBIENT, COLOR, 10);
					scene.add3DObject(light);
					break;
				case GLFW.GLFW_KEY_3:
					scene.remove3DObject(light);
					light = new SpotLight(light.getPosition(), AMBIENT, COLOR, 10, Vec3.Z_NEG, 15, 0);
					scene.add3DObject(light);
					break;
				case GLFW.GLFW_KEY_UP:
					lightMesh.setPosition(lightMesh.getPosition().add(Vec3.Y.scale(INC_XY)));
					break;
				case GLFW.GLFW_KEY_DOWN:
					lightMesh.setPosition(lightMesh.getPosition().add(Vec3.Y_NEG.scale(INC_XY)));
					break;
				case GLFW.GLFW_KEY_LEFT:
					lightMesh.setPosition(lightMesh.getPosition().add(Vec3.X_NEG.scale(INC_XY)));
					break;
				case GLFW.GLFW_KEY_RIGHT:
					lightMesh.setPosition(lightMesh.getPosition().add(Vec3.X.scale(INC_XY)));
					break;
				case GLFW.GLFW_KEY_Q:
					lightMesh.setPosition(lightMesh.getPosition().add(Vec3.Z.scale(INC_Z)));
					break;
				case GLFW.GLFW_KEY_A:
					lightMesh.setPosition(lightMesh.getPosition().add(Vec3.Z_NEG.scale(INC_Z)));
					break;
				case GLFW.GLFW_KEY_H:
					printHelp(HELP);
					break;
				default:
					super.keyPressed(e);
				}
				light.setPosition(lightMesh.getPosition());
			}
		};

		controller.run(time -> {
			try {
				// Create view
				new DefaultView(controller, 100, 100, 500, 500, IView.INTERACTIVE_VIEW, "Simple Sphere");

				// Create scene and add some content
				scene = new DefaultScene(controller);
				controller.setScene(scene);

				// Add first light and light geometry
				GeodesicSphere s = new GeodesicSphere(4);

				lightMesh = new DefaultMesh(Primitive.TRIANGLES, new ColorMaterial(RGBA.YELLOW), DefaultGeometry.createV(s.getTriangles()), Flag.DONT_CAST_SHADOW);
				lightMesh.setTransform(Mat4.trs(0, 0, 0, 0, 0, 0, 0.1f, 0.1f, 0.1f));
				lightMesh.setPosition(new Vec3(0, 0, 2));
				light.setPosition(lightMesh.getPosition());

				scene.add3DObjects(light);
				scene.add3DObjects(lightMesh);

				// Add a second light (now that we have multiple light support...)
				scene.add3DObject(new PointLight(new Vec3(2, 0, 2), RGB.BLACK, RGB.BLUE));

				// Add a ground plane
				IMesh ground = MeshUtilities.createGroundPlane(1000);
				scene.add3DObject(ground);

				// Add geometry
				IMaterial solidMaterial = new ShadedMaterial(RGB.BLACK, RGB.BLUE, RGB.GRAY, RGB.WHITE, 10, 1, 1f);
				IMaterial lineMaterial = new LineMaterial(new RGBA(1, 1, 1, 0.2f));

				IGPUImage t = IGPUImage.read(SimpleLightExample.class.getResource("/textures/earth_nasa.jpg"));
				IMaterial textureMaterial = new ShadedMaterial(RGB.BLACK, RGB.BLUE, RGB.GRAY, RGB.RED, 10, 1, 1f, t);

				IMesh solidMeshT = new DefaultMesh(Primitive.TRIANGLES, solidMaterial, DefaultGeometry.createVN(s.getTriangles(), s.getNormals()));
				IMesh solidMeshL = new DefaultMesh(Primitive.LINES, lineMaterial, DefaultGeometry.createV(s.getLines()), Queue.TRANSPARENCY);

				solidMeshT.setTransform(Mat4.trs(-1, 0, 0.5f, 0, 0, 0, 1, 1, 1));
				solidMeshL.setTransform(Mat4.trs(-1, 0, 0.5f, 0, 0, 0, 1, 1, 1));

				IMesh texturedMeshT = new DefaultMesh(Primitive.TRIANGLES, textureMaterial, DefaultGeometry.createVNM(s.getTriangles(), s.getNormals(), s.getTexCoords()));
				texturedMeshT.setTransform(Mat4.trs(1, 0, 0.5f, 0, 0, 0, 1, 1, 1));

				IMesh solidCubeT = MeshUtilities.createCube(solidMaterial);
				solidCubeT.setTransform(Mat4.trs(0, 0, 0.5f, 0, 0, 0, 0.8f, 0.8f, 0.8f));

				scene.add3DObjects(solidMeshT, solidMeshL, texturedMeshT, solidCubeT);

				// Add bunny
				IMesh solidBunnyT = null;
				if (ADD_BUNNY) {
					try {
						List<IMesh> meshes = new ObjReader(getClass().getResource("/models/bunny_original.obj")).getMeshes();
						solidBunnyT = new DefaultMesh(Primitive.TRIANGLES, solidMaterial, meshes.get(0).getGeometry());
						solidBunnyT.setTransform(Mat4.trs(2, 0, 0, 90, 0, 0, 4, 4, 4));
						scene.add3DObject(solidBunnyT);
					} catch (Throwable throwable) {
						throwable.printStackTrace();
					}
				}
			} catch(Throwable t) {
				LOG.severe(t);
			}
		});
		
		Platform.get().run();
	}
}
