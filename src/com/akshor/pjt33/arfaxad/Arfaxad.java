package com.akshor.pjt33.arfaxad;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import javax.swing.*;

/**
 * Arfaxad is a lyric projection program developed with the primary aim of being a useful tool for churches. Although it
 * doesn't aim to compete with Easy Worship, it does aim to be the best free option on Linux.
 */
public class Arfaxad
{
	/** Internationalised resources */
	// The default value may be replaced after selecting profile
	static ResourceBundle resources = ResourceBundle.getBundle("com.akshor.pjt33.arfaxad.arfaxad");
	/** General config */
	static Properties configuration;
	/** Screens */
	static GraphicsDevice[] screens = new GraphicsDevice[2];
	/** Profile */
	static Profile profile;
	static MainWindow mainWindow;
	static ProjectorWindow projectorWindow;
	/** Song database */
	static SortedSet<Song> songs;
	/** Default font. It's possible that in future each song will be able to configure its font. */
	static Font defaultFont;
	/** Model and control for the current and next song */
	static ModelControl currentMC = new ModelControl();
	static ModelControl nextMC = new ModelControl();
	/** Debug options */
	static boolean warnIfOneScreen = true;

	public static void main(String[] args) {
		// Workaround for OS X
		System.setProperty("apple.awt.fullscreencapturealldisplays", "false");

		// Read arguments
		for (String arg : args) {
			if ("--no-warning".equals(arg)) warnIfOneScreen = false;
			else System.err.println("Unrecognised argument " + arg);
		}

		// Check we have config
		if (!readConfig()) return;

		// Obtain graphics devices and check that there are two screens
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gds = ge.getScreenDevices();
		int numScr = 0;
		for (GraphicsDevice gd : gds) {
			if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
				screens[numScr++] = gd;
				if (numScr == 2) break;
			}
		}
		if (warnIfOneScreen && numScr < 2) warnMissingScreen();

		// Select a profile
		File profileDir = toFile(configuration.getProperty("directory.profiles"));
		if (!profileDir.isDirectory()) profile = new Profile();
		else {
			File[] profileFiles = profileDir.listFiles();
			if (profileFiles.length == 0) profile = new Profile();
			else {
				Profile[] profiles = new Profile[profileFiles.length];
				int idx = 0;
				for (File archProfile : profileFiles) {
					try {
						profiles[idx++] = new Profile(archProfile);
					}
					catch (IOException ioe) {
						// TODO Notify user
					}
				}
				if (idx == 0) profile = new Profile();
				else if (idx == 1) profile = profiles[0];
				else {
					if (idx != profiles.length) {
						Profile[] newProfiles = new Profile[idx];
						System.arraycopy(profiles, 0, newProfiles, 0, idx);
						profiles = newProfiles;
					}
					profile = (Profile)(JOptionPane.showInputDialog(null, resources.getString("dialog.choose.profile.msg"),
					                    resources.getString("dialog.choose.profile.title"),
					                    JOptionPane.QUESTION_MESSAGE, null, profiles,
					                    profiles[0]));
					if (profile == null) System.exit(0);
				}
			}
		}

		// It's possible that the profile's locale isn't that of the ResourceBundle
		if (!profile.locale.equals(resources.getLocale())) {
			Locale.setDefault(profile.locale);
			resources = ResourceBundle.getBundle("com.akshor.pjt33.arfaxad.arfaxad");
			JComponent.setDefaultLocale(profile.locale);
			Collation.reload();
		}

		// Load song database
		songs = new TreeSet<Song>();
		songs.addAll(profile.loadSongs());

		// Do this properly to avoid problems
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Show main window
				mainWindow = new MainWindow();
				mainWindow.setVisible(true);
			}
		});
	}

	/**
	 * Without two screens the system doesn't work, so warn the user
	 */
	private static void warnMissingScreen() {
		String exit = resources.getString("action.quit");
		JOptionPane jop = new JOptionPane(
		    resources.getString("dialog.missing.screen.msg"),
		    JOptionPane.ERROR_MESSAGE,
		    JOptionPane.OK_CANCEL_OPTION,
		    null,
		    new String[] { exit, resources.getString("action.continue"), },
		    exit);
		jop.createDialog(null, resources.getString("dialog.missing.screen.title")).setVisible(true);
		Object action = jop.getValue();
		if (action.equals(exit)) System.exit(0);
	}

	/**
	 * At present we assume that it follows Debian rules: global config in /etc/arfaxad.conf and personal config in
	 * ~/.arfaxad/arfaxad.conf
	 *
	 * TODO More idiomatic alternative for Windows
	 */
	private static boolean readConfig() {
		configuration = new Properties();
		InputStream in = null;
		try {
			in = Arfaxad.class.getResourceAsStream("arfaxad.conf");
			configuration.load(in);
			// TODO Complain about keys missing from the default config
		}
		catch (IOException ioe) {
			// TODO GUI message
			System.err.println("Error: can't read the default config");
			return false;
		}
		finally {
			try {
				if (in != null) in.close();
				in = null;
			}
			catch (IOException ioe) {
				// Ignore
			}
		}

		for (String filename : new String[] {
		            "/etc/arfaxad.conf",
		            "./arfaxad.conf", // for Windows
		            "~/.arfaxad/arfaxad.conf"
		        }) {
			File file = toFile(filename);
			if (file.isFile()) {
				Properties conf2 = new Properties(configuration);
				try {
					in = new FileInputStream(file);
					conf2.load(in);
					// TODO Complain about keys which aren't in the default config.
					configuration = conf2;
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
				finally {
					try {
						if (in != null) in.close();
						in = null;
					}
					catch (IOException ioe) {
						// Ignore
					}
				}
			}
		}

		return true;
	}

	public static ImageIcon loadIcon(String name) {
		java.net.URL url = Arfaxad.class.getResource(name);
		return new ImageIcon(url);
	}

	public static Color parseColour(String colour) {
		// Firstly: is it a hex number?
		if (colour.startsWith("#")) colour = colour.substring(1);
		try {
			return new Color(0xff000000 + Integer.parseInt(colour, 16));
		}
		catch (NumberFormatException nfe) {
			// No, it isn't.
		}

		// Secondly: is it a standard name?
		try {
			Field f = Color.class.getDeclaredField(colour);
			return (Color)f.get(null);
		}
		catch (Exception e) {
			// Not that either.
		}

		// Default value.
		return Color.WHITE;
	}

	public static boolean equal(Object obj1, Object obj2) {
		return (obj1 == null || obj2 == null) ? obj1 == obj2 : obj1.equals(obj2);
	}

	public static File toFile(String str) {
		if (str.startsWith("~/")) {
			str = System.getProperty("user.home") + str.substring(1);
		}
		return new File(str);
	}
}
