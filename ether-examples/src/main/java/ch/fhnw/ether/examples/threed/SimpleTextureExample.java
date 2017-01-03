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
import ch.fhnw.ether.controller.event.IEventScheduler;
import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMapMaterial;
import ch.fhnw.ether.scene.mesh.material.IMaterial;
import ch.fhnw.ether.view.DefaultView;
import ch.fhnw.ether.view.IView;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;

public final class SimpleTextureExample {

    public static void main(String[] args) {
        new SimpleTextureExample();
    }

    private static IMesh makeTexturedTriangle() {
        float[] vertices = { 0, 1f, 0, 1f, 0, 0f, 0, 0, 1f };
        // float[] colors = { 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1 };
        float[] colors = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
        // float[] texCoords = { 0, 0, 1, 1, 0, 1 };
        float[] texCoords = { 0, 0, 0, 1, 1, 1 };

        try {
            IGPUImage t = IGPUImage.read(SimpleTextureExample.class.getResource("/textures/tron.jpg"));

            IMesh texturedMeshT = new DefaultMesh(Primitive.TRIANGLES, new ColorMapMaterial(t),
                    DefaultGeometry.createVM(vertices, texCoords), Queue.DEPTH);

            IMaterial m = new ColorMapMaterial(RGBA.WHITE, t, true);
            IGeometry g = DefaultGeometry.createVCM(vertices, colors, texCoords);
            return texturedMeshT;
//            return new DefaultMesh(Primitive.TRIANGLES, m, g);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("cant load image");
            System.exit(1);
        }
        return null;
    }

    private IMesh mesh;

    // Setup the whole thing
    public SimpleTextureExample() {
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

            mesh = makeTexturedTriangle();
            scene.add3DObject(mesh);
        });

        // Animate (Using event timer)
        controller.animate(new IEventScheduler.IAnimationAction() {
            private int c = 0;

            @Override
            public void run(double time, double interval) {

                // make some heavy animation calculation
                c += 4;
                if (c >= 360)
                    c = 0;
                float f = 0.4f + 0.6f * (float) (Math.sin(Math.toRadians(c)) * 0.5 + 1);

                // apply changes to geometry
//                 mesh.setTransform(Mat4.scale(f, f, f));
//                mesh.getGeometry().modify(1, (id, vertices) -> {
//                    for (int i = 0; i < vertices.length; ++i) {
//                        if (i % 4 == 3)
//                            continue;
//                         vertices[i] -= 0.2f * (1 - f);
//                         if (vertices[i + 0] <= 0)
//                         vertices[i + 0] = 1;
//                    }
//                });
            }
        });

        Platform.get().run();
    }
}
