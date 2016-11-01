package ch.fhnw.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class IOUtilities {
	static final int BUFFER_SZ = 1024*1024;

	
	public static void moveFile(File from, File to, boolean deleteToIfExists) throws IOException {
		moveFile(from, to, deleteToIfExists, false);
	}

	public static void moveFile(File from, File to, boolean deleteToIfExists, boolean createDirs) throws IOException {
		if (deleteToIfExists && to.exists())
			to.delete();
		if (createDirs)
			to.getParentFile().mkdirs();
		if (!from.renameTo(to)) {
			copy(from, to);
			from.delete();
		}
	}

	/**
	 * Copy source into dest.
	 * 
	 * @param source
	 *            The source reader.
	 * @param dest
	 *            The destination writer.
	 */
	public static long copy(Reader source, Writer dest) throws IOException {
		return copy(source, dest, true);
	}

	/**
	 * Copy source into dest.
	 * 
	 * @param source
	 *            The source file.
	 * @param dest
	 *            The destination file.
	 */
	public static long copy(File source, File dest) throws IOException {
		return copy(new FileInputStream(source), new FileOutputStream(dest));
	}

	/**
	 * Copy source into dest.
	 * 
	 * @param source
	 *            The source stream.
	 * @param dest
	 *            The destination file.
	 */
	public static long copy(InputStream source, File dest) throws IOException {
		return copy(source, new FileOutputStream(dest));
	}

	/**
	 * Copy source into dest.
	 * 
	 * @param source
	 *            The source stream.
	 * @param dest
	 *            The destination stream.
	 */
	public static long copy(InputStream in, OutputStream out) throws IOException {
		return copy(in, out, true);
	}

	/**
	 * Copy source into dest.
	 * 
	 * @param source
	 *            The source stream.
	 * @param dest
	 *            The destination stream.
	 * @param close
	 *            Close both stream when done.
	 */
	public static long copy(InputStream in, OutputStream out, boolean close) throws IOException {
		BufferedInputStream bin = new BufferedInputStream(in);
		BufferedOutputStream bout = new BufferedOutputStream(out);
		long count = 0;
		try {
			byte[] buffer = new byte[BUFFER_SZ];
			int read = -1;
			while ((read = bin.read(buffer, 0, buffer.length)) != -1) {
				bout.write(buffer, 0, read);
				count += read;
			}
			bout.flush();
			return count;
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (close) {
				try {
					bin.close();
				} catch (Exception ex) {
				}
				try {
					bout.close();
				} catch (Exception ex) {
				}
			}
		}
	}

	/**
	 * Copy source into dest.
	 * 
	 * @param source
	 *            The source reader.
	 * @param dest
	 *            The destination reader.
	 * @param close
	 *            Close both stream when done.
	 */
	public static long copy(Reader in, Writer out, boolean close) throws IOException {
		BufferedReader bin = new BufferedReader(in);
		BufferedWriter bout = new BufferedWriter(out);
		long count = 0;
		try {
			char[] buffer = new char[BUFFER_SZ];
			int read = -1;
			while ((read = bin.read(buffer, 0, buffer.length)) != -1) {
				bout.write(buffer, 0, read);
				count += read;
			}
			bout.flush();
			return count;
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (close) {
				try {
					bin.close();
				} catch (Exception ex) {
				}
				try {
					bout.close();
				} catch (Exception ex) {
				}
			}
		}
	}
}
