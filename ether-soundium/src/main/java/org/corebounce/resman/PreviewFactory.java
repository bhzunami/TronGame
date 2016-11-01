package org.corebounce.resman;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import ch.fhnw.ether.controller.DefaultController;
import ch.fhnw.ether.formats.obj.ObjReader;
import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.platform.IImageSupport;
import ch.fhnw.ether.platform.IImageSupport.FileFormat;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.platform.SWTImageSupport;
import ch.fhnw.ether.render.forward.ForwardRenderer;
import ch.fhnw.ether.scene.DefaultScene;
import ch.fhnw.ether.scene.IScene;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.light.DirectionalLight;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.IMaterial;
import ch.fhnw.ether.scene.mesh.material.ShadedMaterial;
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.IVideoSource;
import ch.fhnw.ether.video.PreviewTarget;
import ch.fhnw.ether.video.URLVideoSource;
import ch.fhnw.ether.view.IView.Config;
import ch.fhnw.ether.view.IView.ViewType;
import ch.fhnw.ether.view.OffscreenView;
import ch.fhnw.util.IOUtilities;
import ch.fhnw.util.Log;
import ch.fhnw.util.MIME;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.color.ColorUtilities;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.BoundingBox;

public class PreviewFactory {
	private static final Log   log = Log.create();

	protected static final int STRIP_HEIGHT         = 48;
	protected static final int STRIP_WIDTH          = 1400;
	private   static final int CACHE_SIZE           = 8192;

	private final Set<String>               negativeCache     = new HashSet<>();
	private final Map<Resource, IHostImage> previewCache      = new WeakHashMap<>(CACHE_SIZE);
	private final Map<Resource, Image>      previewImageCache = new HashMap<>(CACHE_SIZE);
	private final BlockingDeque<Resource>   generateQueue     = new LinkedBlockingDeque<>();
	private final Set<String>               generateLock      = new HashSet<String>();
	private       int                       progressCnt;
	private final MetaDB                    db;

	private static final IImageSupport LOADER = Platform.get().getImageSupport();

	private static IHostImage none      = IHostImage.create(1, 1, ComponentType.BYTE, ComponentFormat.RGB); 
	static IHostImage noPreview = load(PreviewFactory.class.getResourceAsStream("/nopreview.png"));  

	public static final String P_FRAMES         = "frames";
	public static final String P_WIDTH          = "width";
	public static final String P_HEIGHT         = "height";
	public static final String P_COLOR          = "color";
	public static final String P_HUE            = "hue";
	public static final String P_SATURATION     = "saturation";
	public static final String P_BRIGHTNESS     = "brightness";
	public static final String P_DEPTH          = "depth";
	public static final String P_VERTICES       = "vertices";
	public static final String P_NORMALS        = "normals";
	public static final String P_TEXTURE_COORDS = "textureCoords";
	public static final String P_FACES          = "faces";
	public static final String P_FONT_NAME      = "fontName";
	public static final String P_HAS_ALPHA      = "alpha";
	public static final String P_TILE_W         = "tileW";
	public static final String P_TILE_H         = "tileH";
	public static final String P_TILE_N         = "tileN";
	public static final String P_SHOT_STARTS    = "shots";

	static {
		Resource.registerProperty(P_WIDTH, true);
		Resource.registerProperty(P_HEIGHT, true);
		Resource.registerProperty(P_FRAMES, true);
		Resource.registerProperty(P_COLOR, true);
		Resource.registerProperty(P_HUE, true);
		Resource.registerProperty(P_SATURATION, true);
		Resource.registerProperty(P_BRIGHTNESS, true);
		Resource.registerProperty(P_DEPTH, false);
		Resource.registerProperty(P_VERTICES, false);
		Resource.registerProperty(P_NORMALS, true);
		Resource.registerProperty(P_TEXTURE_COORDS, false);
		Resource.registerProperty(P_FACES, false);
		Resource.registerProperty(P_FONT_NAME, true);
		Resource.registerProperty(P_HAS_ALPHA, true);
		Resource.registerProperty(P_TILE_W, false);
		Resource.registerProperty(P_TILE_H, false);
		Resource.registerProperty(P_TILE_N, false);
		Resource.registerProperty(P_SHOT_STARTS, false);
	}

	public PreviewFactory(MetaDB metadb) {
		db = metadb;
		for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						try {
							// we have no guarantee that the resource was not added more than once to the queue, thus the lock
							Resource resource = generateQueue.take();
							String path = resource.getPath();
							synchronized (generateLock) {
								if (generateLock.contains(path))
									continue;
								generateLock.add(path);
							}
							generatePreview(resource);
							synchronized (generateLock) {
								generateLock.remove(path);
							}
							Thread.yield();
						} catch (Throwable t) {
						}
					}
				}
			});
			t.setName("PreviewGenerator[" + i + "]");
			t.setPriority(Thread.MIN_PRIORITY);
			t.setDaemon(true);
			t.start();
		}
	}

	public IHostImage getPreview(Resource resource) {
		synchronized (negativeCache) {
			if (negativeCache.contains(resource.getPath()))
				return noPreview;
		}
		synchronized (previewCache) {
			IHostImage result = previewCache.get(resource);
			if (result == null) {
				try {
					result = loadPreview(resource, db.getPreviewFile(resource.getMD5()));
					if(result != null) {
						synchronized (previewCache) {
							previewCache.put(resource, result);
							previewImageCache.remove(resource);
						}
						return result;
					}
				} catch (Exception ex) {}
			} else 
				return result;
		}
		generateAsync(resource, true);
		return noPreview;
	}

	public void generateAsync(Resource resource, boolean front) {
		if (front) {
			if (generateQueue.contains(resource)) {
				synchronized (generateQueue) {
					generateQueue.remove(resource);
				}
			}
			generateQueue.addFirst(resource);
		} else {
			if (!generateQueue.contains(resource))
				generateQueue.addLast(resource);
		}
	}

	private void generatePreview(Resource resource) {
		File previewFile = null;
		try {
			previewFile = db.getPreviewFile(resource.getMD5());
		} catch (Exception ex) {}
		IHostImage preview = loadPreview(resource, previewFile);
		if (preview == null) {
			if (canPreview(resource)) {
				try {
					preview = createPreview(resource);
					if (preview != null) {
						db.sync(resource);
						if (previewFile != null) 
							store(preview, previewFile);
					}
				} catch (Exception e) {
					log.warning(resource.getPath(), e);
				}
			}
			progressCnt++;
			System.out.print(".");
			System.out.flush();
			if (progressCnt % 60 == 0)
				System.out.println();
		}

		if (preview == null) {
			synchronized (negativeCache) {
				log.warning("Unable to create preview for " + resource);
				negativeCache.add(resource.getPath());
			}
		} else {
			synchronized (previewCache) {
				previewCache.put(resource, preview);
				previewImageCache.remove(resource);
			}
		}
	}

	private IHostImage loadPreview(Resource resource, File previewFile) {
		IHostImage result = null;
		if (previewFile != null && previewFile.exists()) {
			try {
				result = load(previewFile);
			} catch (Throwable e) {
				log.warning(resource.toString(), e);
				for (String path : generateLock)
					log.info(path);
				previewFile.delete();
			}
		}
		return result;
	}

	private IHostImage createPreview(Resource resource) throws MalformedURLException, IOException, RenderCommandException, InterruptedException {
		IHostImage result = noPreview;
		if(MIME.match(resource.getMimeType(), MIME.MT_OBJ)) {
			result = preview3D(resource);
		} else if(MIME.match(resource.getMimeType(), MIME.MT_TTF)) {
			result = previewFont(resource);
		} else if(LOADER.canRead(resource.getMimeType())) {
			result = previewImage(resource);
			colorProps(result, resource);
		} else if(URLVideoSource.canRead(resource.getMimeType())) {
			result = previewMovie(resource);
			colorProps(result, resource);
		}
		return result;
	}

	private IHostImage previewFont(Resource resource) throws IOException {
		IHostImage     result = IHostImage.create(STRIP_HEIGHT*16, STRIP_HEIGHT, ComponentType.BYTE, ComponentFormat.RGB);
		IOException[]  error  = new IOException[1];
		Display.getDefault().syncExec(()->{
			Display display = Display.getDefault();
			display.loadFont(resource.getPath());

			try(FileInputStream in = new FileInputStream(resource.getPath())) {
				java.awt.Font awtfont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
				resource.putProperty(P_FONT_NAME, awtfont.getFontName());
				in.close();
				Font font = new Font(display, new FontData(awtfont.getName(), (int)(STRIP_HEIGHT*0.7), SWT.NORMAL)); 

				Image image = new Image(display, result.getWidth(), result.getHeight());
				GC gc = new GC(image);
				gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
				gc.fillRectangle(image.getBounds());
				gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
				gc.setFont(font);
				gc.drawString("ABCD abcd 0123", 0, 2);
				gc.dispose();
				font.dispose();

				byte[]    rgb       = new byte[4];
				ImageData imageData = image.getImageData();
				for(int y = imageData.height; --y >= 0;)
					for(int x = imageData.width; --x >= 0;) {
						RGB srcrgb = imageData.palette.getRGB(imageData.getPixel(x, y));
						rgb[0] = (byte) srcrgb.red;
						rgb[1] = (byte) srcrgb.green;
						rgb[2] = (byte) srcrgb.blue;
						result.setPixel(x, imageData.height-(y+1), rgb);
					}
				image.dispose();

			} catch(IOException e) {
				error[0] = e;
			} catch(Throwable t) {
				error[0] = new IOException(t);
			} 
		});

		if(error[0] != null)
			throw error[0];

		return result;
	}

	private static final Vec3[] ROT = {
			new Vec3(90,0,0), Vec3.X,
			new Vec3(90,0,0), Vec3.Y,
			new Vec3(0,0,0),  Vec3.X,
			new Vec3(45,0,0), new Vec3(1,1,0),
	};

	private DefaultController controller = new DefaultController(new ForwardRenderer(false));

	private IHostImage preview3D(Resource resource) throws IOException, InterruptedException {
		CountDownLatch  latch  = new CountDownLatch(1);
		IOException[]   error  = new IOException[1];
		DecimalFormat   fmt    = TextUtilities.decimalFormat(2);
		final int[]     stats  = new int[4];      
		OffscreenView[] view   = new OffscreenView[1]; 
		controller.run(time->{
			try {
				view[0] = new OffscreenView(controller, STRIP_HEIGHT * 8, STRIP_HEIGHT * 2, new Config(ViewType.RENDER_VIEW, 4, RGBA.BLACK));
				IScene scene = new DefaultScene(controller);

				controller.setScene(scene);
				scene.add3DObject(new DirectionalLight(Vec3.Z, ch.fhnw.util.color.RGB.BLACK, ch.fhnw.util.color.RGB.WHITE));
				controller.setCamera(view[0], new Camera(new Vec3(0,0,10), Vec3.ZERO, -4, -100, 100));

				final URL obj  = resource.getFile().toURI().toURL();
				BoundingBox bb = new BoundingBox();
				for(IMesh mesh : new ObjReader(obj).getMeshes()) {
					for(int i = mesh.getGeometry().getAttributes().length; --i >= 0;) {
						IGeometry.IGeometryAttribute attr = mesh.getGeometry().getAttributes()[i];
						if(attr.equals(IGeometry.POSITION_ARRAY))
							stats[0] += mesh.getGeometry().getData()[i].length / attr.getNumComponents();
						else if(attr.equals(IGeometry.NORMAL_ARRAY))
							stats[1] += mesh.getGeometry().getData()[i].length / attr.getNumComponents();
						else if(attr.equals(IGeometry.COLOR_MAP_ARRAY))
							stats[2] += mesh.getGeometry().getData()[i].length / attr.getNumComponents();
					}
					bb.add(mesh.getBounds());
				}
				float s = 0.7f / Math.max(bb.getExtentX(), Math.max(bb.getExtentY(), bb.getExtentZ()));
				IMaterial mat = new ShadedMaterial(ch.fhnw.util.color.RGB.WHITE);
				for(int i = 0; i < 4; i++) {
					Mat4 t = Mat4.multiply(
							Mat4.scale(s),
							Mat4.rotate(ROT[i*2].x,ROT[i*2+1]),
							Mat4.translate(bb.getCenter().scale(-1))
							);
					for(IMesh mesh : new ObjReader(obj).getMeshes()) {
						IMesh m = new DefaultMesh(mesh.getType(), mat, mesh.getGeometry());
						m.setTransform(t);
						m.setPosition(new Vec3(1.5f-i, 0, 0));
						scene.add3DObject(m);
					}
				}
				controller.getRenderManager().update();					

				resource.putProperty(P_WIDTH,          fmt.format(bb.getMaxX() - bb.getMinX()));
				resource.putProperty(P_HEIGHT,         fmt.format(bb.getMaxY() - bb.getMinY()));
				resource.putProperty(P_DEPTH,          fmt.format(bb.getMaxZ() - bb.getMinZ()));
				resource.putProperty(P_VERTICES,       stats[0]);
				resource.putProperty(P_NORMALS,        stats[1]);
				resource.putProperty(P_TEXTURE_COORDS, stats[2]);
				resource.putProperty(P_FACES,          stats[3]);
			} catch(IOException e) {
				error[0] = e;
			} catch(Throwable t) {
				error[0] = new IOException(t);
			} finally {
				latch.countDown();
			}
		});

		latch.await();
		if(error[0] != null)
			throw error[0];
		return LOADER.scale(view[0].getImage(), STRIP_HEIGHT * 4, STRIP_HEIGHT);
	}

	private IHostImage previewMovie(Resource resource) throws IOException, MalformedURLException, RenderCommandException {
		IHostImage result;
		IVideoSource src = new URLVideoSource(resource.getFile().toURI().toURL(), 1, 1);
		resource.putProperty(P_WIDTH,  src.getWidth());
		resource.putProperty(P_HEIGHT, src.getHeight());
		resource.putProperty(P_FRAMES, src.getLengthInFrames());
		RenderProgram<IVideoRenderTarget> program = new RenderProgram<>(src);
		final PreviewTarget target  = new PreviewTarget(STRIP_WIDTH, STRIP_HEIGHT);
		target.useProgram(program);
		target.start();
		target.sleepUntil(IScheduler.NOT_RENDERING);
		result = target.getPreview();
		return result;
	}

	private IHostImage previewImage(Resource resource) throws FileNotFoundException, IOException {
		IHostImage result;
		IHostImage image = load(resource.getFile());
		resource.putProperty(P_WIDTH,  image.getWidth());
		resource.putProperty(P_HEIGHT, image.getHeight());
		resource.putProperty(P_FRAMES, 1);
		resource.putProperty(P_HAS_ALPHA, image.getComponentFormat() == ComponentFormat.RGBA);
		if(image.getComponentFormat() != ComponentFormat.RGBA && image.getComponentFormat() != ComponentFormat.RGB) {
			File tmp = new File(resource.getFile().getParentFile(), resource.getFile().getName() + ".bak");
			IOUtilities.moveFile(resource.getFile(), tmp, true);
			try {
				log.info("Converting '" + resource.getFile().getName() + "' from " + image.getComponentFormat() + " to " + ComponentFormat.RGB);
				image = image.convert(image.getComponentType(), ComponentFormat.RGB, image.getAlphaMode());
				store(image, resource.getFile());
			} catch(Throwable t) {
				IOUtilities.moveFile(tmp, resource.getFile(), false);
			} finally {
				tmp.delete();
			}
		}
		result = LOADER.scale(image, (STRIP_HEIGHT * image.getWidth()) / image.getHeight(), STRIP_HEIGHT);
		return result;
	}

	private void colorProps(IHostImage result, Resource resource) {
		double hue = 0;
		double sat = 0;
		double bri = 0;
		int colorCount = 0;
		int pxCount = 0;
		float hsb[] = new float[3];
		byte  rgb[] = new byte[4];
		for (int x = result.getWidth(); --x >= 0;) {
			for (int y = result.getHeight(); --y >= 0;) {
				result.getPixel(x, y, rgb);

				final int red   = rgb[0] & 0xFF;
				final int green = rgb[1] & 0xFF;
				final int blue  = rgb[2] & 0xFF;

				final int avg = (red + green + blue) / 3;
				if (avg < 8 || avg > 247)
					continue;

				ColorUtilities.RGBtoHSB(red, green, blue, hsb);
				hue += hsb[0];
				sat += hsb[1];
				bri += hsb[2];

				if (Math.abs(red - avg) > 10)
					colorCount++;
				else if (Math.abs(green - avg) > 6)
					colorCount++;
				else if (Math.abs(blue - avg) > 16)
					colorCount++;
				pxCount++;
			}
		}

		resource.putProperty(P_COLOR, pxCount == 0 ? false : ((double) colorCount / pxCount) > 0.04);

		if (pxCount > 0) {
			hue /= pxCount;
			sat /= pxCount;
			bri /= pxCount;
		}

		hue = MathUtilities.clamp(hue, 0, 1);
		sat = MathUtilities.clamp(sat, 0, 1);
		bri = MathUtilities.clamp(bri, 0, 1);

		resource.putProperty(P_HUE,        hue);
		resource.putProperty(P_SATURATION, sat);
		resource.putProperty(P_BRIGHTNESS, bri);
	}

	private boolean canPreview(Resource resource) {
		return MIME.match(resource.getMimeType(), MIME.MT_OBJ)
				|| MIME.match(resource.getMimeType(), MIME.MT_TTF)
				|| URLVideoSource.canRead(resource.getMimeType()) 
				|| LOADER.canRead(resource.getMimeType());
	}

	private void store(IHostImage preview, File file) {
		try {
			LOADER.write(preview, new FileOutputStream(file), FileFormat.get(file));
		} catch(Throwable t) {
			log.warning("Can't store " + file, t);
		}
	}

	private static IHostImage load(InputStream in) {
		try {
			return LOADER.readHost(in, null, null, null);
		} catch (Throwable t) {
			log.warning("Can't load " + in, t);
			return none;
		}  
	}

	private static IHostImage load(File file) throws FileNotFoundException, IOException {
		return LOADER.readHost(new FileInputStream(file), null, null, null);
	}

	public Image getPreviewImage(Resource res, Display display) {
		synchronized (previewImageCache) {
			Image result = previewImageCache.get(res);
			if(result == null) {
				try {
					result = new Image(display, SWTImageSupport.toImageData(getPreview(res)));
					previewImageCache.put(res, result);
				} catch(Throwable t) {
					log.warning(res.getPath(), t);
				}
			}
			return result;
		}
	}
}
