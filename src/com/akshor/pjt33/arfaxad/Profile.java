package com.akshor.pjt33.arfaxad;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class Profile
{
	public Locale locale = Locale.getDefault();
	public String name;
	// TODO Make this less Linux-oriented
	public File songDir = new File("/usr/share/arfaxad/songs");
	public boolean recursiveSearch = false;
	// TODO Ability to configure margins
	// Margins in %.
	public int marginN = 4;
	public int marginS = 6;
	public int marginE = 5;
	public int marginW = 5;
	// Justification
	public boolean justifyLeft = true;
	// TODO Validate the size
	public BufferedImage logo;
	// The preferred minimum font size (for a 1024x768 screen)
	public int minSize = 55;
	// The largest font size we permit (for a 1024x768 screen)
	public int maxSize = 75;
	// The font size which we use for copyright information (for a 1024x768 screen)
	public int copyrightSize = 30;
	/** A log for copyright information */
	public CopyrightLog log;

	/** Build a default profile */
	Profile() {
		name = Arfaxad.resources.getString("default.profile");
	}

	/** Read a profile from a file */
	Profile(File file) throws IOException {
		String logPath = new File(file.getParentFile().getParentFile(), "copyright.log").getPath();
		// TODO
		Properties prop = new Properties();
		InputStream in = new FileInputStream(file);
		try {
			prop.load(in);
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ioe) {
				// Nothing useful to do here.
			}
		}

		Enumeration<?> e = prop.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String)e.nextElement();
			String value = prop.getProperty(key);
			if ("name".equals(key)) name = value;
			else if ("directory.songs".equals(key)) {
				songDir = Arfaxad.toFile(value);
				if (!songDir.isDirectory()) {
					// TODO i18n
					System.err.println("Song directory " + songDir + " (" + songDir.getCanonicalPath()
					                   + ") doesn't exist or is not a directory");
					throw new IOException();
				}
			}
			else if ("logo".equals(key)) {
				try {
					logo = ImageIO.read(Arfaxad.toFile(value));
				}
				catch (Exception ex) {
					// TODO i18n
					System.err.println("Error reading logo " + value + ": " + ex);
				}
			}
			else if ("localisation".equals(key)) {
				try {
					locale = parseLocale(value);
				}
				catch (Exception ex) {
					System.err.println("Error: unable to parse locale '" + key + "'");
				}
			}
			else if ("log".equals(key)) logPath = value;
			else {
				// TODO i18n
				System.err.println("Error reading " + file + ": unexpected key: " + key);
				throw new IOException();
			}
		}

		if (songDir == null) throw new IOException();

		// Open log
		if (logPath != null) {
			try {
				log = new CopyrightLog(Arfaxad.toFile(logPath));
			}
			catch (FileNotFoundException fnfe) {
				// Treat as though there were no log...
			}
		}
		if (log == null) {
			// TODO i18n
			JOptionPane.showMessageDialog(null, "Unable to open log file: " + logPath, "Warning: log file",
			                              JOptionPane.WARNING_MESSAGE);
		}
	}

	List<Song> loadSongs() {
		List<Song> songs = new ArrayList<Song>();
		loadSongs(songs, songDir, recursiveSearch);
		return songs;
	}

	private static void loadSongs(List<Song> songs, File dir, boolean recursive) {
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				Song song = Song.loadSong(file);
				if (song != null) songs.add(song);
			}
			else if (recursive && file.isDirectory()) loadSongs(songs, file, recursive);
		}
	}

	private static Locale parseLocale(String s) {
		if (s.matches("^[a-z][a-z]$")) return new Locale(s);
		if (s.matches("^[a-z][a-z]_[A-Z][A-Z]$")) return new Locale(s.substring(0, 2), s.substring(3, 5));
		if (s.matches("^[a-z][a-z]_[A-Z][A-Z]\\..*"))
			return new Locale(s.substring(0, 2), s.substring(3, 5), s.substring(6));
		throw new IllegalArgumentException();
	}

	@Override
	public String toString() {
		return name;
	}
}
