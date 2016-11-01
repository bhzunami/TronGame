package org.corebounce.resman;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.fhnw.util.Log;

public final class FileScanner implements Runnable {
	private static final Log log = Log.create();

	private static final String[] IGNORE_EXT = { ".db", ".html", ".swf", ".ini", ".cbr", ".constraints", ".bak"};

	private static int threadCount;

	private final File dir;

	private final AtomicBoolean running = new AtomicBoolean(true);
	private final Object        lock    = new Object();

	private final MetaDB         db;
	private final PreviewFactory pf;

	public FileScanner(MetaDB db, PreviewFactory pf, File dir) {
		this.db   = db;
		this.pf   = pf;
		this.dir = dir;
		Thread thread = new Thread(this, "FSScanner[" + (threadCount++) + "]");
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public String toString() {
		return dir.getPath();
	}

	@Override
	public void run() {
		Set<File> processedFiles = new HashSet<File>();

		while (running.get()) {
			HashSet<File> addFiles = scan(dir, new HashSet<File>());

			synchronized (lock) {
				addFiles.removeAll(processedFiles);
			}

			fileLoop: for (File file : addFiles) {
				if (db.inMetaDB(file))
					continue fileLoop;
				try {
					addToMetaDB(file);
				} catch (Throwable e) {
					log.warning(file.toString(), e);
				}
				synchronized (lock) {
					processedFiles.add(file);
				}
			}

			try {
				setStatus("Sleeping...");
				synchronized (FileScanner.this) {
					FileScanner.this.wait(10000);
				}
				db.syncDB();
			} catch (Exception e) {
				log.severe(e);
			}
		}
	}

	private void addToMetaDB(File file) throws IOException {
		Resource res = db.addToDB(file);
		pf.generateAsync(res, false);
	}

	private HashSet<File> scan(File file, HashSet<File> result) {
		if (file.isFile() && !file.getName().startsWith(".")) {
			for (int j = 0; j < IGNORE_EXT.length; j++) {
				if (file.getName().endsWith(IGNORE_EXT[j]))
					return result;
			}
			result.add(file);
		} else if (file.isDirectory()) {
			setStatus("Scanning " + file.getPath() + "...");
			File[] files = file.listFiles();
			if (files != null) {
				for (int i = files.length; --i >= 0;) {
					scan(files[i], result);
				}
			}
		}
		return result;
	}

	private void setStatus(String msg) {
		// log.info(msg);
	}
}
