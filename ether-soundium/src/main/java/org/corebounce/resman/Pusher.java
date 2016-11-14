package org.corebounce.resman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;

import ch.fhnw.ether.formats.obj.ObjReader;
import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.platform.IImageSupport;
import ch.fhnw.ether.platform.IImageSupport.FileFormat;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.IVideoSource;
import ch.fhnw.ether.video.URLVideoSource;
import ch.fhnw.util.AutoDisposer;
import ch.fhnw.util.FloatList;
import ch.fhnw.util.Log;
import ch.fhnw.util.MIME;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.BoundingBox;

public class Pusher {
	private static final Log log = Log.create();

	private static final int TEX_W = 8192; // keep in sync with Soundium.cs
	private static final int TEX_H = 2048;

	private final OSC            osc;
	private final MetaDB         db;
	private final PreviewFactory pf;

	public Pusher(MetaDB db, PreviewFactory pf, OSC osc) {
		this.osc = osc;
		this.db  = db;
		this.pf  = pf;
	}


	private File atlasFile(File atlas, int idx) {
		if(idx == 0) return atlas;
		String ext = "." + TextUtilities.getFileExtensionWithoutDot(atlas);
		return new File(atlas.getParentFile(), TextUtilities.getFileNameWithoutExtension(atlas) + (idx == 0 ? ext : "_" + idx + ext));
	}

	private void pushMovie(Resource res, File atlas, int slot) {
		int numURI = 0;
		for(int i = 0; ; i++) {
			File f = atlasFile(atlas, i);
			if(!(f.exists())) break;
			numURI++;
		}
		List<Object> values = new ArrayList<>();
		values.add(atlas.toURI().toASCIIString());
		values.add(res.getMD5());
		values.add(Integer.valueOf(res.getProperty(PreviewFactory.P_TILE_W)));
		values.add(Integer.valueOf(res.getProperty(PreviewFactory.P_TILE_H)));
		values.add(Integer.valueOf(numURI));
		if(MIME.match(res.getMimeType(), MIME.MT_GIF)) {
			for(long frameNo : res.getShotStarts())
				values.add(Integer.valueOf((int)frameNo));
		} else
			values.add(Integer.valueOf(res.getProperty(PreviewFactory.P_TILE_N)));
		osc.send("/slots/" + slot + "/texture", values); 
		res.incrementUseCount();
		db.sync(res);
	}

	private void pushImage(Resource res, int slot) {
		osc.send("/slots/" + slot + "/texture",  
				res.getFile().toURI().toASCIIString(),
				res.getMD5(),
				Integer.valueOf(res.getProperty(PreviewFactory.P_WIDTH)),
				Integer.valueOf(res.getProperty(PreviewFactory.P_HEIGHT)),
				Integer.valueOf(1), 
				Integer.valueOf(1));
		res.incrementUseCount();
		db.sync(res);
	}

	private void pushGeometry(Resource res, File vertices, int slot) {
		osc.send("/slots/" + slot + "/geometry",  
				vertices.toURI().toASCIIString(),
				res.getMD5());
		res.incrementUseCount();
		db.sync(res);
	}

	public void pushResource(TableItem item, Resource res, int slot) {
		try {
			if(MIME.match(res.getMimeType(), MIME.X_GEOMETRY+"/*")) {
				File vertices = db.getCacheFile(res.getMD5(), "v");
				if(vertices.exists()) {
					pushGeometry(res, vertices, slot);
					return;
				} else {
					generateGeometry(item, res, slot, vertices);
				}
				return;
			} else {
				int numFrames = (int)Double.parseDouble(res.getProperty(PreviewFactory.P_FRAMES));
				if(numFrames == 1) {
					pushImage(res, slot);
				} else if(numFrames > 1) {
					File atlas = db.getCacheFile(res.getMD5(), "bin");
					if(atlas.exists()) {
						pushMovie(res, atlas, slot);
						return;
					}
					generateAtlas(item, res, slot, atlas);
				}
			}
		} catch(Throwable t) {}
	}

	private void generateGeometry(TableItem item, Resource res, int slot, File vertices) {
		item.setImage((Image)null);
		ItemRepainer repainer = new ItemRepainer(item, 100).start();
		Thread t = new Thread("Geometry:"+res) {
			@Override
			public void run() {
				try {
					final float       numSteps = 4;
					float             step     = 0;
					final URL         obj    = res.getFile().toURI().toURL();
					final List<IMesh> meshes = new ArrayList<>();
					final BoundingBox bb     = new BoundingBox();

					FloatList v  = new FloatList();
					FloatList vn = new FloatList();
					FloatList vt = new FloatList();

					res.setProgress(++step/numSteps);
					new ObjReader(obj).getMeshes().forEach(mesh -> {
						meshes.add(mesh);
						for(int i = mesh.getGeometry().getAttributes().length; --i >= 0;) {
							IGeometry.IGeometryAttribute attr = mesh.getGeometry().getAttributes()[i];
							if(attr.equals(IGeometry.POSITION_ARRAY)) 
								v.addAll(mesh.getGeometry().getData()[i]);
							else if(attr.equals(IGeometry.NORMAL_ARRAY))
								vn.addAll(mesh.getGeometry().getData()[i]);
							else if(attr.equals(IGeometry.COLOR_MAP_ARRAY))
								vt.addAll(mesh.getGeometry().getData()[i]);
						}
						bb.add(mesh.getBounds());
					});
					res.setProgress(++step/numSteps);
					final Vec3    center = bb.getCenter();
					final float[] va     = v._getArray();
					final float   scale  = Math.max(bb.getExtentX(), Math.max(bb.getExtentY(), bb.getExtentZ())); 
					for(int i = 0; i < v.size(); i += 3) {
						va[i+0] = (va[i+0] - center.x) / scale;  
						va[i+1] = (va[i+1] - center.y) / scale;  
						va[i+2] = (va[i+2] - center.z) / scale;  
					}
					res.setProgress(++step/numSteps);

					writeFloats(vertices, v);
					if(vn.size() > 0)
						writeFloats(new File(vertices.getParentFile(), 
								TextUtilities.getFileNameWithoutExtension(vertices) + ".vn"), vn);
					if(vt.size() > 0)
						writeFloats(new File(vertices.getParentFile(), 
								TextUtilities.getFileNameWithoutExtension(vertices) + ".vt"), vt);

					res.setProgress(++step/numSteps);
					db.sync(res);

					pushGeometry(res, vertices, slot);
				} catch(Throwable t) {
					log.warning("Can't create atlas for '" + res.getPath() + "'", t);
				}
				Display.getDefault().asyncExec(()->{
					repainer.stop();
					item.setImage(pf.getPreviewImage(res, Display.getDefault()));
					item.getParent().redraw();
				});
			}

			private void writeFloats(File file, FloatList floats) throws IOException, FileNotFoundException {
				ByteBuffer buf = ByteBuffer.allocate (floats.size() * Float.BYTES);
				buf.order (ByteOrder.LITTLE_ENDIAN);
				for(int i = 0; i < floats.size(); i++)
					buf.putFloat(floats.get(i));
				buf.clear();
				try(FileOutputStream fout = new FileOutputStream (file)) {
					try(FileChannel out = fout.getChannel ()) { 
						out.write (buf);
					}
				}
			}
		};
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	private void generateAtlas(TableItem item, Resource res, int slot, File atlas) {
		item.setImage((Image)null);
		ItemRepainer repainer = new ItemRepainer(item, 100).start();
		Thread t = new Thread("Atlas:"+res) {
			@Override
			public void run() {
				try {
					IImageSupport ims = Platform.get().getImageSupport();
					Throwable[] error = new Throwable[1];
					IVideoSource src = new URLVideoSource(res.getFile().toURI().toURL(), 1);
					RenderProgram<IVideoRenderTarget> program = new RenderProgram<>(src);
					AtlasTarget target  = new AtlasTarget(TEX_W, TEX_H, res) {
						@Override
						protected void writeImage(int idx, IHostImage image) {
							try {
								ims.write(image, new FileOutputStream(atlasFile(atlas, idx)), FileFormat.get(atlas));
							} catch(Throwable t) {error[0] = t;}
						}
					};
					target.useProgram(program);
					target.start();
					target.sleepUntil(IScheduler.NOT_RENDERING);

					if(error[0] != null) throw error[0];

					res.putProperty(PreviewFactory.P_TILE_W, target.getTileWidth());
					res.putProperty(PreviewFactory.P_TILE_H, target.getTileHeight());
					res.putProperty(PreviewFactory.P_TILE_N, target.getNumTiles());
					StringBuilder shotStarts = new StringBuilder();
					for(long frameNo : target.getShotStarts())
						shotStarts.append(Long.valueOf(frameNo)).append(',');
					shotStarts.setLength(shotStarts.length()-1);
					res.putProperty(PreviewFactory.P_SHOT_STARTS, shotStarts.toString());
					target.dispose();
					db.sync(res);
					AutoDisposer.runGC();

					pushMovie(res, atlas, slot);
				} catch(Throwable t) {
					log.warning("Can't create atlas for '" + res.getPath() + "'", t);
				}
				Display.getDefault().asyncExec(()->{
					repainer.stop();
					item.setImage(pf.getPreviewImage(res, Display.getDefault()));
					item.getParent().redraw();
				});
			}
		};
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
}
