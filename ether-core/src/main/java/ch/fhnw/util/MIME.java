package ch.fhnw.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MIME {
	private static HashMap<String, String> mimeMediaType = new HashMap<String, String>();
	private static HashMap<String, String> mimeSubType = new HashMap<String, String>();
	private static HashMap<String, String> mime2extension = new HashMap<String, String>();
	private static ArrayList<String[]> mimeTypes = new ArrayList<String[]>();

	public final static String APPLICATION = "application";
	public final static String PHOTOSHOP   = "photoshop";
	public final static String X_FONT      = "x-font";

	public final static String AUDIO = "audio";
	public final static String MIDI = "midi";
	public final static String MP3 = "mp3";
	public final static String X_AIFF = "x-aiff";
	public final static String X_RAW = "x-raw";
	public final static String X_WAV = "x-wav";
	
	public final static String IMAGE  = "image";
	public final static String GIF    = "gif";
	public final static String JPEG   = "jpeg";
	public final static String PNG    = "png";
	public final static String TIFF   = "tiff";
	public final static String TGA    = "tga";
	public final static String BMP    = "bmp";
	public final static String X_HDR  = "x-hdr";
	public final static String PICT   = "pict";
	public final static String ANYMAP = "x-portable-anymap";
	
	public final static String VIDEO = "video";
	public final static String MPEG = "mpeg";
	public final static String QUICKTIME = "quicktime";
	public final static String MP4 = "mp4";
	public final static String M4V = "x-m4v";
	public final static String X_MSVIDEO = "x-msvideo";
	
	public final static String TEXT = "text";
	public final static String HTML = "html";
	public final static String PLAIN = "plain";
	public final static String RICHTEXT = "richtext";
	public final static String TAB_SEPARATED_VALUES = "tab-separated-values";
	
	public final static String X_GEOMETRY = "x-geometry";
	public final static String OBJ = "obj";
	
	public final static String MULTIPART    = "multipart";
	public final static String FORM_DATA    = "form-data";
	public final static String OCTET_STREAM = "octet-stream";

	public final static String X_WWW_FORM_URLENCODED = "x-www-form-urlencoded";

	public final static String MT_JPEG = type(IMAGE, JPEG);
	public final static String MT_PNG  = type(IMAGE, PNG);
	public final static String MT_TGA  = type(IMAGE, TGA);
	public final static String MT_BMP  = type(IMAGE, BMP);
	public final static String MT_PSD  = type(APPLICATION, PHOTOSHOP);
    public final static String MT_HDR  = type(IMAGE, X_HDR);
    public final static String MT_PICT = type(IMAGE, PICT);
    public final static String MT_PNM  = type(IMAGE, ANYMAP);
    public final static String MT_GIF  = type(IMAGE, GIF);
    public final static String MT_TIFF = type(IMAGE, TIFF);
    
	public static final String MT_MP4  = type(VIDEO, MP4);
	public static final String MT_MOV  = type(VIDEO, QUICKTIME);

	public static final String MT_OBJ  = type(X_GEOMETRY, OBJ);

	public static final String MT_TTF  = type(APPLICATION, X_FONT);
	
	static {
		put(AUDIO, MIDI,   ".mid");
		put(AUDIO, MIDI,   ".midi");
		put(AUDIO, X_AIFF, ".aif");
		put(AUDIO, X_AIFF, ".aiff");
		put(AUDIO, X_AIFF, ".aifc");
		put(AUDIO, X_RAW,  ".raw");
		put(AUDIO, X_WAV,  ".wav");
		put(AUDIO, MP3,    ".mp3");
		
		put(IMAGE,       GIF,        ".gif");
		put(IMAGE,       JPEG,       ".jpe");
		put(IMAGE,       JPEG,       ".jpeg");
		put(IMAGE,       JPEG,       ".jpg");
		put(IMAGE,       PNG,        ".png");
		put(IMAGE,       TIFF,       ".tiff");
		put(IMAGE,       TIFF,       ".tif");
		put(IMAGE,       TGA,        ".tga");
		put(IMAGE,       BMP,        ".bmp");
		put(APPLICATION, PHOTOSHOP,  ".psd");
		put(IMAGE,       X_HDR,      ".hdr");		
		put(IMAGE,       ANYMAP,     ".pnm");
		
		put(VIDEO, MPEG,      ".mpeg");
		put(VIDEO, MPEG,      ".mpg");
		put(VIDEO, MPEG,      ".mpe");
		put(VIDEO, QUICKTIME, ".mov");
		put(VIDEO, MP4,       ".mp4");
		put(VIDEO, M4V,       ".m4v");
		put(VIDEO, X_MSVIDEO, ".avi");

		put(TEXT, HTML,                 ".html");
		put(TEXT, HTML,                 ".htm");
		put(TEXT, PLAIN,                ".txt");
		put(TEXT, RICHTEXT,             ".rtf");
		put(TEXT, TAB_SEPARATED_VALUES, ".tsv");

		put(X_GEOMETRY, OBJ,            ".obj");
		
		put(APPLICATION, X_FONT,        ".ttf");
	}

	private static String extension(String filename) {
		int position = filename.lastIndexOf('.');
		if (position != -1)
			return filename.substring(position).toLowerCase();
		return filename.toLowerCase();
	}

	private static void put(String mediaType, String subType, String extension) {
		mimeMediaType.put(extension, mediaType);
		mimeSubType.put(extension, subType);
		mimeTypes.add(new String[] { mediaType, subType });
		mime2extension.put(mediaType + "/" + subType, extension);
	}

	public static String[][] getMimeTypes() {
		return mimeTypes.toArray(new String[mimeTypes.size()][]);
	}

	public static String mime2extension(String mimeType) {
		String result = mime2extension.get(mimeType);
		return result == null ? "" : result;
	}

	public static String getContentTypeFor(File file) {
		return getContentTypeFor(file.getName());
	}

	public static String getContentTypeFor(String filename) {
		String extension = extension(filename);
		String mclass = mimeMediaType.get(extension);
		if (mclass == null)
			return APPLICATION + "/" + OCTET_STREAM;
		String mtype = mimeSubType.get(extension);
		if (mtype == null)
			return APPLICATION + "/" + OCTET_STREAM;
		return mclass + "/" + mtype;
	}

	public static boolean match(String mimeType, String pattern) {
		String[] mt = TextUtilities.split(mimeType, '/');
		String[] mp = TextUtilities.split(pattern, '/');

		boolean result = (mp[0].equals(mt[0]) && mp[1].equals(mt[1])) || (mp[0].equals("*") && mp[1].equals(mt[1]))
				|| (mp[0].equals(mt[0]) && mp[1].equals("*")) || (mp[0].equals("*") && mp[1].equals("*"));

		return result;
	}
	
	public static String type(String mediaType, String subType) {
		return mediaType + "/" + subType;
	}

	public static String getContentTypeFor(URL url) {
		return getContentTypeFor(url.getPath());
	}
}
