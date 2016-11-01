
package ch.fhnw.ether.examples.threed;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.formats.obj.ObjReader;
import ch.fhnw.ether.platform.IImageSupport.FileFormat;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.render.forward.ForwardRenderer;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.light.DirectionalLight;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.view.IView.Config;
import ch.fhnw.ether.view.IView.ViewType;
import ch.fhnw.ether.view.OffscreenView;
import ch.fhnw.util.Log;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.BoundingBox;

public class OffscreenRendering {
	private static final Log log = Log.create();
	
	private static final Vec3[] ROT = {
			new Vec3(90,0,0), Vec3.X,
			new Vec3(90,0,0), Vec3.Y,
			new Vec3(0,0,0),  Vec3.X,
			new Vec3(45,0,0), new Vec3(1,1,0),
	};

	public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
		new OffscreenRendering(args);
	}
	
	public OffscreenRendering(String[] args) throws FileNotFoundException, IOException, InterruptedException {
		Platform.get().init();
		IController    controller = new DefaultController(new ForwardRenderer(false));
		CountDownLatch inited     = new CountDownLatch(1);
		controller.run(time -> {
			OffscreenView view = new OffscreenView(controller, 1024, 256, new Config(ViewType.RENDER_VIEW, 4, RGBA.BLACK));
			IScene scene = new DefaultScene(controller);
			
			controller.setScene(scene);
			scene.add3DObject(new DirectionalLight(Vec3.Z, RGB.BLACK, RGB.WHITE));
			controller.setCamera(view, new Camera(new Vec3(0,0,10), Vec3.ZERO, -4, -100, 100));
			try {
				final URL obj  = getClass().getResource("/models/fhnw.obj");
				log.info("Loading '" + obj + "'");
				BoundingBox bb = new BoundingBox();
				for(IMesh mesh : new ObjReader(obj).getMeshes())
					bb.add(mesh.getBounds());
				float s = 0.7f / Math.max(bb.getExtentX(), Math.max(bb.getExtentY(), bb.getExtentZ()));
				for(int i = 0; i < 4; i++) {
					Mat4 t = Mat4.multiply(
							Mat4.scale(s),
							Mat4.rotate(ROT[i*2].x,ROT[i*2+1]),
							Mat4.translate(bb.getCenter().scale(-1))
							);
					for(IMesh mesh : new ObjReader(obj).getMeshes()) {
						IMesh m = new DefaultMesh(mesh.getType(), mesh.getMaterial(), mesh.getGeometry());
						m.setTransform(t);
						m.setPosition(new Vec3(1.5f-i, 0, 0));
						scene.add3DObject(m);
					}
				}
				log.info("Rending scene to texture");
				controller.getRenderManager().update();
				
				File out = new File("preview.png");
				log.info("Writing texture to '" + out.getAbsolutePath() + "'");
				Platform.get().getImageSupport().write(view.getImage(), new FileOutputStream(out), FileFormat.PNG);
			} catch (IOException e) {
				e.printStackTrace();
			}
			inited.countDown();
		});
		inited.await();
	}
}

