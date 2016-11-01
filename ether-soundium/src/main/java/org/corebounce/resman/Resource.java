package org.corebounce.resman;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.IProgressListener;
import ch.fhnw.util.Log;
import ch.fhnw.util.MIME;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.net.NetworkUtilities;

public final class Resource implements Comparable<Resource>, IProgressListener {
	private static final Log log = Log.create();

	public static final String PATH        = "path";
	public static final String NAME        = "name";
	public static final String MD5         = "resourceMd5";
	public static final String SIZE        = "size";
	public static final String DATE        = "date";
	public static final String MIME_TYPE   = "mimeType";
	public static final String USE_COUNT   = "useCount";
	public static final String DUP         = "dup";

	private static final DateFormat GMT_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'", Locale.US);

	private static List<String> PROPERTIES         = new ArrayList<>();
	private static List<String> USER_PROPERTIES    = new ArrayList<>();
	private static Set<String>  DEFAULT_PROPERTIES = new HashSet<>();

	private final SortedMap<String, String> properties = new TreeMap<>();

	private final File file;
	private final String path;
	private final String md5;
	private final int    index;
	private final long   size;
	private final Date   date;
	private final String mimeType;
	private       float  progress = -1;
	private String       previewMD5 = ClassUtilities.EMPTY_String;
	private int          useCount;
	private Set<String>  tags;
	private Set<File>    duplicates;
	private boolean      needsSync;

	static {
		DEFAULT_PROPERTIES.add(PATH);
		DEFAULT_PROPERTIES.add(MD5);
		DEFAULT_PROPERTIES.add(SIZE);
		DEFAULT_PROPERTIES.add(MIME_TYPE);
		DEFAULT_PROPERTIES.add(USE_COUNT);
		registerProperty(NAME, true);
		registerProperty(MIME_TYPE, true);
		registerProperty(SIZE, true);
		registerProperty(DATE, true);
		registerProperty(USE_COUNT, true);

		GMT_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public Resource(File res, String md) {
		file = res;
		path = res.getAbsolutePath();
		md5 = md;
		index = Integer.parseInt(md5.substring(0, 1), 16);
		size = file.length();
		date = new Date(file.lastModified());
		mimeType = MIME.getContentTypeFor(res);
		setNeedsSync();
	}

	public Resource(ByteArrayInputStream in, MetaDB db) throws IOException {
		Properties props = new Properties();
		props.load(in);
		try {
			path = props.getProperty(PATH);
			file = db.translatePath(path);
		} catch (Exception e) {
			throw new IOException("cannot init resource: " + props.getProperty(PATH));
		}
		try {
			md5 = props.getProperty(MD5) == null ? getMD5(file) : props.getProperty(MD5);
		} catch (Exception e) {
			throw new IOException("cannot init resource: " + file);
		}
		index = Integer.parseInt(md5.substring(0, 1), 16);

		long sz = 0;
		try {
			sz = Long.parseLong(props.getProperty(SIZE));
		} catch (Exception e) {
			sz = file.length();
			setNeedsSync();
		}
		size = sz;

		Date dt = null;
		try {
			dt = GMT_FORMATTER.parse(props.getProperty(DATE));
		} catch (Exception e) {
			dt = new Date(file.lastModified());
			setNeedsSync();
		}
		date = dt;

		String mt = props.getProperty(MIME_TYPE);
		if (mt == null) {
			mt = MIME.getContentTypeFor(file);
			setNeedsSync();
		}
		mimeType = mt;

		try {
			useCount = Integer.parseInt(props.getProperty(USE_COUNT));
		} catch (Exception e) {
			useCount = 0;
		}

		for (Enumeration<Object> e = props.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			if (DEFAULT_PROPERTIES.contains(key))
				continue;
			putProperty(key, props.getProperty(key));
		}

		for (int i = 0;; i++) {
			String dup = props.getProperty(DUP + i);
			if (dup == null)
				break;
			addDuplicate(db.translatePath(dup));
		}
		in.close();
	}

	public void sync(OutputStream out) throws IOException {
		Properties props = new Properties();
		props.put(PATH, file.getPath());
		props.put(MD5, md5);
		props.put(SIZE, "" + size);
		props.put(DATE, GMT_FORMATTER.format(date));
		props.put(MIME_TYPE, mimeType);
		props.put(USE_COUNT, "" + useCount);
		synchronized (props) {
			for (Entry<String, String> entry : properties.entrySet()) {
				props.setProperty(entry.getKey(), entry.getValue());
			}
		}
		if (duplicates != null) {
			int i = 0;
			for (File f : duplicates) {
				props.put(DUP + i, f.getPath());
				++i;
			}
		}
		props.store(out, md5);
		needsSync = false;
	}

	public boolean needsSync() {
		return needsSync;
	}

	public void setNeedsSync() {
		needsSync = true;
	}

	public static void registerProperty(String property, boolean isUser) {
		PROPERTIES.add(property);
		if(isUser)
			USER_PROPERTIES.add(property);
	}

	public String getProperty(String key) {
		if (key.equals(PATH))
			return getPath();
		if (key.equals(NAME))
			return getFile().getName();
		if (key.equals(DATE))
			return GMT_FORMATTER.format(date);
		if (key.equals(MIME_TYPE))
			return mimeType;
		if (key.equals(USE_COUNT))
			return String.valueOf(useCount);
		synchronized (properties) {
			return properties.get(key);
		}
	}

	public void putProperty(String key, boolean value) {
		putProperty(key, Boolean.toString(value));
	}

	public void putProperty(String key, double value) {
		putProperty(key, Double.toString(value));
	}

	public void putProperty(String key, long value) {
		putProperty(key, Long.toString(value));
	}
	
	public void putProperty(String key, String value) {
		synchronized (properties) {
			properties.put(key.intern(), value);
		}
		setNeedsSync();
	}

	public static List<String> getProperties() {
		return PROPERTIES;
	}

	public static List<String> getUserProperties() {
		return USER_PROPERTIES;
	}

	public double getDoubleProperty(String key) {
		return Double.parseDouble(getProperty(key));
	}

	public String listProperties() {
		String propList = file.getName() + "\n";
		propList += "type: " + mimeType + "\n";
		propList += "size: " + size + " bytes\n";
		propList += "modified: " + date + "\n";
		propList += "used: " + useCount + " times\n\n";
		synchronized (properties) {
			for (Entry<String, String> entry : properties.entrySet()) {
				propList += entry.getKey() + ": " + entry.getValue() + "\n";
			}
		}
		propList = propList.substring(0, propList.length() - 1);
		return propList;
	}

	public String getPath() {
		return path;
	}

	public File getFile() {
		return file;
	}

	public Set<File> getDuplicates() {
		return duplicates;
	}

	public void addDuplicate(File f) {
		if (!f.exists())
			return;
		if (duplicates == null)
			duplicates = new HashSet<File>();
		duplicates.add(f);
		log.info("duplicate resource:" + f + " and " + getPath());
		setNeedsSync();
	}

	public String getPreviewMD5() {
		return previewMD5;
	}

	public void setPreviewMD5(String digest) {
		previewMD5 = digest;
		setNeedsSync();
	}

	public static String getMD5(InputStream in) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] buffer = new byte[1024 * 64];
			while (true) {
				int r = in.read(buffer);
				if (r == -1)
					break;
				md.update(buffer, 0, r);
			}
			return toHex(md.digest());
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String getMD5(File file) throws IOException {
		try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
			return getMD5(in);
		}		
	}

	public String getMD5() {
		return md5;
	}

	public int getIndex() {
		return index;
	}

	public static String toHex(byte[] buffer) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < buffer.length; i++) {
			result.append(TextUtilities.HEXTAB.charAt((buffer[i] >> 4) & 0xF));
			result.append(TextUtilities.HEXTAB.charAt(buffer[i] & 0xF));
		}
		return result.toString();
	}

	public long getSize() {
		return size;
	}

	public Date getDate() {
		return date;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getUseCount() {
		return String.valueOf(useCount);
	}

	public void resetUseCount() {
		useCount = 0;
		setNeedsSync();
	}

	public void incrementUseCount() {
		useCount++;
		setNeedsSync();
	}

	public Set<String> getTags() {
		if (tags == null) {
			tags = new HashSet<String>();

			String[] tmp = getFile().getParentFile().getAbsolutePath().toLowerCase().split("[\\\\/:]");

			for (int i = 0; i < tmp.length; i++)
				if (tmp[i].length() > 1)
					tags.add(stripTrailNum(cleanupTag(tmp[i])));

			String name = getFile().getName().toLowerCase();
			int idx = name.lastIndexOf('.');
			if (idx > 0)
				name = name.substring(0, idx);
			tmp = cleanupTag(name).split(" ");

			for (int i = 0; i < tmp.length; i++) {
				String s = tmp[i];
				tmp[i] = stripTrailNum(stripLeadNum(s));
				if (i == 0 && tmp[i].length() == 0)
					tmp[i] = s;
				if (tmp[i].length() > 1)
					tags.add(tmp[i]);
			}
		}
		return tags;
	}

	public static String cleanupTag(String s) {
		s = NetworkUtilities.URLDecode(s);
		StringBuffer result = new StringBuffer();
		int count = s.length();
		for (int i = 0; i < count; i++) {
			char c = s.charAt(i);
			if (Character.isLetter(c) || Character.isDigit(c))
				result.append(c);
			else
				result.append(' ');
		}
		return result.toString();
	}

	@Override
	public int compareTo(Resource o) {
		return getPath().compareTo(o.getPath());
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Resource ? getPath().equals(((Resource) o).getPath()) : false;
	}

	@Override
	public int hashCode() {
		return getPath().hashCode();
	}

	@Override
	public String toString() {
		return "#" + getMD5() + "_" + getFile().getName();
	}

	private static String stripLeadNum(String s) {
		while (s.length() > 0 && Character.isDigit(s.charAt(0)))
			s = s.substring(1);
		return s;
	}

	private static String stripTrailNum(String s) {
		StringBuffer result = new StringBuffer(s);
		while (result.length() > 0 && Character.isDigit(result.charAt(result.length() - 1)))
			result.setLength(result.length() - 1);
		return result.toString();
	}

	public String getLabel() {
		String result = getFile().getName();
		if(MIME.match(getMimeType(), MIME.MT_OBJ)) {
			String width  = getProperty(PreviewFactory.P_WIDTH);
			String height = getProperty(PreviewFactory.P_HEIGHT);
			String depth  = getProperty(PreviewFactory.P_DEPTH);
			if (width != null && height != null)
				result += " | " + width + "x" + height + (depth == null ? "" : "x" + depth);
			if (getProperty(PreviewFactory.P_VERTICES) != null) {
				result += " " + getProperty(PreviewFactory.P_VERTICES) + " v / " + getProperty(PreviewFactory.P_TEXTURE_COORDS) + " t / "
						   + getProperty(PreviewFactory.P_NORMALS) + " n /  " + getProperty(PreviewFactory.P_FACES) + " f";
			}
		} else {
			String frames = getProperty(PreviewFactory.P_FRAMES);
			String width  = getProperty(PreviewFactory.P_WIDTH);
			String height = getProperty(PreviewFactory.P_HEIGHT);
			if (frames != null && width != null && height != null)
				result += " | " + width + "x" + height + "x" + frames;
		}
		return result;
	}

	@Override
	public float getProgress() {
		return progress;
	}
	
	@Override
	public void setProgress(float progress) {
		this.progress = progress;
	}

	@Override
	public void done() {
		this.progress = -1;
	}

	public long[] getShotStarts() {
		try {
			String[] shots = TextUtilities.split(getProperty(PreviewFactory.P_SHOT_STARTS), ',');
			long[] result = new long[shots.length];
			for(int i = 0; i < shots.length; i++)
				result[i] = Long.parseLong(shots[i]);
			return result;
		} catch(Throwable t0) {
			try {
				return new long[] {0, Long.parseLong(getProperty(PreviewFactory.P_TILE_N))};
			} catch(Throwable t1) {
				return new long[1];
			}
		}
	}
}