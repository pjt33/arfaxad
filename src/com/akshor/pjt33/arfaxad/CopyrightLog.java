package com.akshor.pjt33.arfaxad;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class CopyrightLog
{
	private final PrintWriter pw;
	private final String date;
	private final Set<String> logged = new HashSet<String>();

	public CopyrightLog(File f) throws FileNotFoundException {
		try {
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f, true), "UTF-8"));
		}
		catch (UnsupportedEncodingException uee) {
			throw new IllegalStateException("Required charset UTF-8 not supported", uee);
		}

		// ISO 8601.
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		date = sdf.format(new Date());
	}

	public void log(Song song) {
		if (song == null) return;

		StringBuilder sb = new StringBuilder();
		sb.append(date);
		sb.append(',');

		escapeCSV(sb, song.title);
		sb.append(',');

		if (song.author == null) sb.append("Unknown");
		else escapeCSV(sb, song.author);
		sb.append(',');

		if (song.year == -1) sb.append("Unknown");
		else sb.append(song.year);
		sb.append(',');

		if (song.administrator == null) sb.append("Unknown");
		else escapeCSV(sb, song.administrator);
		sb.append(',');

		sb.append(song.publicDomain);
		String line = sb.toString();
		if (logged.add(line)) pw.println(line);
		pw.flush();
	}

	private static void escapeCSV(StringBuilder sb, String str) {
		sb.append('"');
		for (char ch : str.toCharArray()) {
			if (ch == '"') sb.append(ch);
			sb.append(ch);
		}
		sb.append('"');
	}
}
