package org.corebounce.resman;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.corebounce.soundium.Subsystem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import ch.fhnw.util.IOUtilities;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;

public final class MetaDB extends Subsystem {
	private static final Log log = Log.create();

	private static final String MD_ALGO  = "MD5";
	private static final String ROOT_RES = "root_res";
	private static final String PROPS    = "props";
	private static final String PREVIEWS = "prevw";
	private static final String CACHE    = "cache";
	private static final String ICONS    = "icons";

	public static final int DIGEST_LEN = 32;

	private final File dir;

	private final File  propsDir;
	private final File  previewsDir;
	private final File  cacheDir;
	private final File  iconsDir;

	private final Map<Integer, HashMap<String, Resource>> md2res           = new HashMap<>();
	private final Map<String, Resource>                   path2res         = new HashMap<>();
	private final List<IChangeListener>                   listeners        = new ArrayList<>();
	private final Map<String, String>                     translationCache = new HashMap<String, String>();
	private final Map<String, Image>                      icons48x48       = new HashMap<>();

	private long modCount;

	private static final String[] DEF_ICONS = {"unknown", "Geometry", "Movie", "Bouncelet"};

	public MetaDB(String ... args) throws NoSuchAlgorithmException {
		super(CFG_PREFIX, args);
		File dir = new File(configuration.get("path"));
		if(!(dir.exists()) || !(dir.isDirectory())) throw new IllegalArgumentException();

		MessageDigest.getInstance(MD_ALGO);

		for (int i = 0; i < 16; i++)
			md2res.put(i, new HashMap<String, Resource>());
		this.dir    = dir;
		propsDir    = new File(dir, PROPS);
		previewsDir = new File(dir, PREVIEWS);
		cacheDir    = new File(dir, CACHE);
		iconsDir    = new File(dir, ICONS);

		if (!propsDir.exists())
			propsDir.mkdirs();

		try {
			File rootRes = new File(dir, ROOT_RES);
			if (!rootRes.exists())
				rootRes.createNewFile();
		} catch (Exception ex) {
			log.warning(ex);
		}

		// create preview directories
		for (int i = 0; i < 16; i++) {
			File d = new File(previewsDir, "" + TextUtilities.HEXTAB.charAt(i));
			if (!d.exists())
				d.mkdirs();
		}

		// create cache directories
		for (int i = 0; i < 16; i++) {
			File d = new File(cacheDir, "" + TextUtilities.HEXTAB.charAt(i));
			if (!d.exists())
				d.mkdirs();
		}

		// init icons
		for(String icon : DEF_ICONS) {
			iconsDir.mkdir();
			File file = new File(iconsDir, icon + "_48x48.png");
			if(!(file.exists())) {
				try {IOUtilities.copy(getClass().getResourceAsStream("/" + file.getName()), file);
				} catch(Throwable t) {log.severe(t);}
			}
		}

		// read properties
		for (int i = 0; i < md2res.size(); i++) {
			File bundle = new File(propsDir, TextUtilities.HEXTAB.charAt(i) + ".txt");
			if (!bundle.exists())
				continue;

			BufferedInputStream in = null;
			try {
				in = new BufferedInputStream(new FileInputStream(bundle));

				byte[] buffer = new byte[128 * 1024]; // hopefully big enough

				boolean attention = false;
				for (int count = 0;;) {
					int c = in.read();
					if (c < 0) {
						handleResource(buffer, count);
						break;
					}
					attention |= c == '\n';
					attention |= c == '\r';
					if (c == '#' && attention && count > 40) {
						handleResource(buffer, count);
						buffer[0] = '#';
						count = 1;
					}
					buffer[count++] = (byte) c;
				}
			} catch (Exception ex) {
				log.warning(ex);
			} finally {
				try {
					if (in != null) in.close();
				} catch (Throwable t) {}
			}
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				listeners.clear();
				syncDB();
			}
		});
	}

	public void syncDB() {
		boolean synced = false;
		for (Resource resource : getResources())
			synced |= sync(resource);
		if(synced) log.info("Resources synced.");
	}

	private void handleResource(byte[] buffer, int count) {
		try {
			Resource res = new Resource(new ByteArrayInputStream(buffer, 0, count), this);

			boolean missing = false;
			if (!res.getFile().exists()) {
				log.info("Cannot find " + res.getPath() + "(" + res.getMD5() + "), removing from DB");
				missing = true;
			}

			if (res.getFile().length() != res.getSize() || res.getFile().lastModified() > res.getDate().getTime() + 2000) { // +2000 for M$ filesystems with 2sec resolution 

				log.info("File " + res.getPath() + " changed, removing from DB");
				missing = true;
			}

			if (missing) {
				if (getPreviewFile(res.getMD5()).exists())
					getPreviewFile(res.getMD5()).delete();
				return;
			}

			String md = res.getMD5();
			md2res.get(res.getIndex()).put(md, res);
			path2res.put(res.getPath(), res);
			Set<File> dups = res.getDuplicates();
			if (dups != null) {
				for (File dup : dups)
					path2res.put(dup.getAbsolutePath(), res);
			}

		} catch (Exception ex) {
			return;
		}
	}

	public Resource addToDB(File f) throws IOException {
		synchronized (path2res) {
			Resource res = path2res.get(f.getAbsolutePath());
			if (res == null) {
				try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
					String md5 = Resource.getMD5(in);
					res = getResourceForMD(md5);
					if (res == null) {
						res = new Resource(f, md5);
						if (getPreviewFile(md5).exists())
							getPreviewFile(md5).delete();
					} else {
						res.addDuplicate(f);
					}
				} catch (IOException ex) {
					throw ex;
				}

				sync(res);

				synchronized (md2res) {
					md2res.get(res.getIndex()).put(res.getMD5(), res);
				}
				path2res.put(res.getPath(), res);
				modCount++;
			}
			return res;
		}
	}

	public List<Resource> getResources() {
		synchronized (path2res) {
			return new ArrayList<Resource>(path2res.values());
		}
	}

	public static void storeProperties(Properties[] props, File dir) {
		try {
			for (int j = 0; j < props.length; j++) {
				String label = props[j].getProperty("label");
				if (label == null || label.isEmpty())
					continue;
				File f = new File(dir, label + ".constraints");
				FileOutputStream out = new FileOutputStream(f);
				props[j].store(out, f.getName());
				out.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public File getPropsFile(String digest) {
		return new File(propsDir, digest.substring(0, 1) + ".txt");
	}

	public File getPreviewFile(String digest) {
		return new File(new File(previewsDir, "" + digest.charAt(0)), digest + ".png");
	}

	public File getCacheFile(String digest, String ext) {
		return new File(new File(cacheDir, "" + digest.charAt(0)), digest + "." + ext);
	}

	public File getConstraintsDir() {
		return dir;
	}

	public boolean inMetaDB(File f) {
		if (f.equals(dir))
			return true;
		f = f.getParentFile();
		if (f == null)
			return false;
		return inMetaDB(f);
	}

	public synchronized List<String> getRootResList() {
		ArrayList<String> result = new ArrayList<String>();
		try(BufferedReader in = new BufferedReader(new FileReader(new File(dir, ROOT_RES)))) {
			while (true) {
				String line = in.readLine();
				if (line == null)
					break;
				result.add(line);
			}
		} catch (Throwable e) {
			log.warning(e);
		}
		return result;
	}

	public synchronized void setRootResList(List<String> res) throws IOException {
		try(PrintWriter out = new PrintWriter(new FileWriter(new File(dir, ROOT_RES)))) {
			for (String r : res)
				out.println(r);
		} catch (IOException ex) {
			throw ex;
		}
	}

	public boolean sync(Resource res) {
		synchronized (this) {
			if (!res.needsSync())
				return false;
			try(BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getPropsFile(res.getMD5())))) {
				synchronized (md2res) {
					for (Resource r :  md2res.get(res.getIndex()).values()) {
						r.sync(out);
					}
				}			
			} catch (IOException ex) {
				log.warning(res.toString(), ex);
			}
		}
		for(IChangeListener l : listeners)
			l.metaDBchanged();
		return true;
	}

	public File translatePath(String path) {
		File file = new File(path);
		if (file.exists())
			return file;
		synchronized (translationCache) {
			File result = null;
			String[] parts = null;
			if (path.length() > 0 && path.charAt(1) == ':')
				parts = path.split("[\\\\/]");
			else
				parts = path.split("[\\\\/:]");
			for (Iterator<String> i = translationCache.keySet().iterator(); i.hasNext();) {
				String prefix = i.next();
				if (path.startsWith(prefix))
					result = new File(translationCache.get(prefix) + TextUtilities.cat(parts, File.separatorChar).substring(prefix.length()));
			}
			if (result == null) {
				outer: for (File base = dir.getParentFile(); base != null; base = base.getParentFile()) {
					for (int j = 0; j < parts.length; j++) {
						result = new File(base.getPath() + TextUtilities.cat(parts, j, File.separatorChar));
						if (result.exists()) {
							String prefix = path.substring(0, TextUtilities.cat(parts, 0, j, File.separatorChar).length());
							String replacement = base.getPath();
							translationCache.put(prefix, replacement);
							break outer;
						}
					}
				}
			}
			return result;
		}
	}

	public Resource getResourceForMD(String digest) {
		synchronized (md2res) {
			return md2res.get(Integer.parseInt(digest.substring(0, 1), 16)).get(digest);
		}
	}

	public Resource getResourceForPath(String path) {
		synchronized (path2res) {
			return path2res.get(path);
		}
	}

	public long getModCount() {
		return modCount;
	}

	public void addChangeListener(IChangeListener listener) {
		listeners.add(listener);
	}

	public static String   CFG_PREFIX = "metadb";
	public static String[] CFG_OPTIONS = {
			"path=<path>", "Path to metadb folder",
	};

	public Image icon48x48(Display display, String key) {
		return icon48x48(display, key, DEF_ICONS[0]);
	}

	public Image icon48x48(Display display, String key, String defaultKey) {
		Image result = icons48x48.get(key);
		if(result == null) {
			File file = new File(iconsDir, key + "_48x48.png");
			if(!(file.exists()))
				file = new File(iconsDir, defaultKey + "_48x48.png");
			try {
				result = new Image(display, new FileInputStream(file));
				icons48x48.put(key, result);
			} catch(Throwable t) {log.severe(t);}
		}
		return result;
	}

}
