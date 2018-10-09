package com.akshor.pjt33.arfaxad;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * The lyrics of a song, the most important data.
 */
public class Song implements Bookmark
{
	private static DocumentBuilder db;
	static {
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}

	/** Elements used by the XML format */
	private static final String ELT_SONG = "song";
	private static final String ELT_INFO = "info";
	private static final String ELT_BRIDGE = "bridge";
	private static final String ELT_CHORUS = "chorus";
	private static final String ELT_GENERAL = "general";
	private static final String ELT_PRECHORUS = "prechorus";
	private static final String ELT_VERSE = "verse";

	/** Attributes used by the XML format */
	private static final String ATTR_ADMINISTRATOR = "administrator";
	private static final String ATTR_AUTHOR = "author";
	private static final String ATTR_AUTO = "auto";
	private static final String ATTR_BACKGROUND = "background";
	private static final String ATTR_COLOUR = "colour";
	private static final String ATTR_JUSTIFY = "just";
	private static final String ATTR_PUBLIC_DOMAIN = "publicDomain";
	private static final String ATTR_TITLE = "title";
	private static final String ATTR_YEAR = "year";

	/** Justification types */
	public static final int DEFAULT_JUSTIFICATION = 0;
	public static final int JUSTIFY_LEFT = 1;
	public static final int JUSTIFY_CENTRE = 2;

	/** The source of this song. This will be null if it's a new song. */
	File file;
	/** The title of the song. It's a matter of convention whether this should be the first line. Not null. */
	public String title;
	/** A CollationKey for the title. */
	private transient CollationKey titleCK;
	/** The author of the song. May be null. */
	public String author;
	/** The song's copyright administrator. May be null. */
	public String administrator;
	/** The year in which the song was written. A value of -1 indicates that it's unknown. */
	public int year = -1;
	/** Whether the song is in the public domain. This is important for the copyright log. */
	public boolean publicDomain = false;
	/** Automatic transition: how many seconds to show each slide. 0 for manual transition. */
	public int transition = 0;
	/** Justification */
	public int just = DEFAULT_JUSTIFICATION;
	/** The slides. Not null. */
	public List<Slide> slides = new ArrayList<Slide>();
	/** Background image. */
	public String backgroundSrc;
	public BufferedImage background;
	/** Text colour */
	public String colourName;
	public Color colour = Color.WHITE;

	/** Loads a song from a file, or returns null on error. */
	public static Song loadSong(File file) {
		// Some editors create backup files with ~ on the end of the filename. Ignore them.
		if (file.getName().endsWith("~")) return null;

		try {
			Document doc = db.parse(file);
			Song s = new Song();
			s.file = file;
			s.read(doc.getDocumentElement());
			return s;
		}
		catch (Exception e) {
			// TODO Better logging
			System.err.println("Error loading song from " + file + ": " + e.getMessage());
			return null;
		}
	}

	/**
	 * TODO: Use visitor pattern? TODO Validate document against XSD
	 */
	private void read(Node node) {
		// We expect node to be an Element with name "song" and one attribute: the title.
		if (!ELT_SONG.equals(node.getNodeName())) throw new IllegalStateException("Main node is not " + ELT_SONG);
		NamedNodeMap attrs = node.getAttributes();
		if (attrs == null) throw new IllegalStateException("<" + ELT_SONG + "> has no attributes");
		for (int i = attrs.getLength() - 1; i >= 0; i--) {
			Attr attr = (Attr)attrs.item(i);
			String attrName = attr.getName();
			String attrValue = attr.getValue();
			if (ATTR_TITLE.equals(attrName)) {
				title = attrValue;
			}
			else if (ATTR_BACKGROUND.equals(attrName)) {
				backgroundSrc = attrValue;
				try {
					background = ImageIO.read(Arfaxad.toFile(attrValue));
				}
				catch (Exception ex) {
					// TODO i18n
					System.err.println("Error loading background " + attrValue + ": " + ex);
				}
			}
			else if (ATTR_COLOUR.equals(attrName)) {
				colourName = attrValue;
				colour = Arfaxad.parseColour(attrValue);
			}
			else throw new IllegalStateException("Unrecognised attribute " + attrName + " on <" + ELT_SONG + ">");
		}

		if (title == null) throw new IllegalStateException("<" + ELT_SONG + "> lacks attribute '" + ATTR_TITLE + "'");

		// The children are an optional "info" and a series of slides of various kinds
		NodeList nodes = node.getChildNodes();
		int numNodes = nodes.getLength();
		boolean firstChild = true;
		for (int i = 0; i < numNodes; i++) {
			node = nodes.item(i);
			if (node instanceof Text) {
				if ("".equals(((Text)node).getWholeText().trim())) continue;
				throw new IllegalStateException("Text outside slide: " + node);
			}
			String type = node.getNodeName();
			if (ELT_INFO.equals(type)) {
				if (!firstChild) throw new IllegalStateException(
					    "If there is an <" + ELT_INFO + ">, it must be the first child of the song");
				attrs = node.getAttributes();
				int attrLen = attrs.getLength();
				for (int j = 0; j < attrLen; j++) {
					Attr a = (Attr)attrs.item(j);
					String aname = a.getName();
					String aval = a.getValue();
					if (ATTR_AUTHOR.equals(aname)) author = aval;
					else if (ATTR_YEAR.equals(aname)) {
						try {
							year = Integer.parseInt(aval);
						}
						catch (NumberFormatException nfe) {
							// TODO dialog
							throw new RuntimeException("Unparseable year in song '" + title + "'", nfe);
						}
					}
					else if (ATTR_ADMINISTRATOR.equals(aname)) administrator = aval;
					else if (ATTR_PUBLIC_DOMAIN.equals(aname)) publicDomain = Boolean.parseBoolean(aval);
					else if (ATTR_AUTO.equals(aname)) {
						try {
							transition = Integer.parseInt(aval);
						}
						catch (NumberFormatException nfe) {
							// TODO dialog
							throw new RuntimeException("Unrecognised transition in song '" + title + "'", nfe);
						}
					}
					else if (ATTR_JUSTIFY.equals(aname)) {
						try {
							just = Integer.parseInt(aval);
						}
						catch (NumberFormatException nfe) {
							// TODO dialog
							throw new RuntimeException("Unrecognised justification in song '" + title + "'", nfe);
						}
					}
					else throw new RuntimeException("Unrecognised info attribute '" + aname + "' in song '" + title + "'");
				}
			}
			else {
				SlideType st = SlideType.fromXmlTag(type);
				if (st != null) parseSlide(st, node);
				else throw new IllegalStateException("Unrecognised node type " + node);
			}

			firstChild = false;
		}
	}

	private void parseSlide(SlideType type, Node node) {
		Slide sl = new Slide(type);
		Node nodeBg = node.getAttributes().getNamedItem(ATTR_BACKGROUND);
		if (nodeBg != null) sl.setProperties(nodeBg.getNodeValue());

		NodeList children = node.getChildNodes();
		int num = children.getLength();
		if (num == 1) {
			Node child = children.item(0);
			if (child instanceof Text) {
				String text = ((Text)child).getWholeText();
				for (String line : text.split("\n"))
					sl.lines.add(AttributedText.decode(line));
			}
			else throw new IllegalStateException("Slide which child which isn't Text");
		}
		else if (!(nodeBg != null && num == 0))
			throw new IllegalStateException("Slide with " + children.getLength() + " children");

		slides.add(sl);
	}

	// TODO Use a library rather than build strings
	public void write(OutputStream os) throws IOException {
		PrintWriter w = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
		w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		w.print("<" + ELT_SONG + " " + ATTR_TITLE + "=\"" + xmlEscape(title) + "\"");
		if (backgroundSrc != null) w.print(" " + ATTR_BACKGROUND + "=\"" + backgroundSrc + "\"");
		if (colourName != null) w.print(" " + ATTR_COLOUR + "=\"" + colourName + "\"");
		w.println(">");
		if (author != null || administrator != null || year != -1 || publicDomain || transition > 0
		        || just != DEFAULT_JUSTIFICATION) {
			w.print("<" + ELT_INFO + " ");
			if (author != null) w.print(ATTR_AUTHOR + "=\"" + xmlEscape(author) + "\" ");
			if (administrator != null) w.print(ATTR_ADMINISTRATOR + "=\"" + xmlEscape(administrator) + "\" ");
			if (year != -1) w.print(ATTR_YEAR + "=\"" + year + "\" ");
			if (publicDomain) w.print(ATTR_PUBLIC_DOMAIN + "=\"true\" ");
			if (transition > 0) w.print(ATTR_AUTO + "=\"" + transition + "\" ");
			if (just != DEFAULT_JUSTIFICATION) w.print(ATTR_JUSTIFY + "=\"" + just + "\" ");
			w.println("/>");
		}
		for (Slide sl : slides) {
			w.print("<" + sl.type.xmlTag);
			if (sl.backgroundSrc != null) w.print(" " + ATTR_BACKGROUND + "=\"" + sl.backgroundSrc + "\"");
			w.print(">");
			boolean first = true;
			for (AttributedText line : sl.lines) {
				if (first) first = false;
				else w.println();
				w.print(xmlEscape(line.encode()));
			}
			w.println("</" + sl.type.xmlTag + ">");
		}
		w.println("</" + ELT_SONG + ">");
		w.flush();
	}

	private static String xmlEscape(String str) {
		return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

	public Collection<Bookmark> bookmarks() {
		Collection<Bookmark> bookmarks = new ArrayList<Bookmark>(1 + slides.size());
		bookmarks.add(this);
		for (Slide slide : slides) {
			// Special-case first slide if it coincides with the title.
			if (slide.slide() == 0 && collationKey().compareTo(slide.collationKey()) == 0) continue;

			bookmarks.add(slide);
		}
		return bookmarks;
	}

	public Song song() {
		return this;
	}

	public CollationKey collationKey() {
		if (titleCK == null) titleCK = Collation.active().getCollationKey(title);
		return titleCK;
	}

	public int slide() {
		return 0;
	}

	public int slide(String cmd) {
		if (cmd == null || "".equals(cmd)) return -1;

		SlideType type = SlideType.TYPE_VERSE;
		int num;
		char ch0 = cmd.charAt(0);
		if (ch0 < '0' || ch0 > '9') {
			type = SlideType.fromLetter(ch0);
			if (type != null && cmd.length() == 1) num = 1;
			else {
				if (type == null) type = SlideType.TYPE_VERSE;
				try {
					num = Integer.parseInt(cmd.substring(1));
				}
				catch (NumberFormatException nfe) {
					return -1;
				}
			}
		}
		else try {
				num = Integer.parseInt(cmd);
			}
			catch (NumberFormatException nfe) {
				return -1;
			}

		int slNum = 0;
		for (Slide sl : slides) {
			if (sl.type == type) {
				num--;
				if (num == 0) return slNum;
			}
			slNum++;
		}
		return -1;
	}

	public String getCopyrightString() {
		StringBuilder sb = new StringBuilder();
		sb.append("copyright_");
		sb.append(author == null ? '0' : '1');
		sb.append(year == -1 ? '0' : '1');
		sb.append(administrator == null ? '0' : '1');
		sb.append(!publicDomain ? '0' : '1');
		String format = Arfaxad.resources.getString(sb.toString());
		sb.setLength(0);
		Formatter form = new Formatter(sb);
		form.format(format, author, year, administrator);
		form.flush();
		return sb.toString();
	}

	public BufferedImage getBackground(int slide) {
		BufferedImage img = slides.get(slide).background;
		return img == null ? background : img;
	}

	@Override
	public String toString() {
		return title;
	}

	/**
	 * Each song consists of various slides.
	 */
	public class Slide implements Bookmark
	{
		public SlideType type;
		public List<AttributedText> lines = new LinkedList<AttributedText>();
		public String backgroundSrc;
		public BufferedImage background;
		private transient CollationKey collationKey;

		public Slide(SlideType t) {
			type = t;
		}

		@Override
		public String toString() {
			if (lines.isEmpty()) return backgroundSrc;
			return lines.get(0).toString();
		}

		public Song song() {
			return Song.this;
		}

		public CollationKey collationKey() {
			if (collationKey == null) collationKey = Collation.active().getCollationKey(toString());
			return collationKey;
		}

		public int slide() {
			int i = 0;
			for (Slide slide : Song.this.slides) {
				if (this == slide) return i;
				i++;
			}
			return -1;
		}

		public void setProperties(String props) {
			// For now, just the image name.
			if (props != null) {
				backgroundSrc = props;
				try {
					background = ImageIO.read(Arfaxad.toFile(props));
				}
				catch (Exception ex) {
					// TODO i18n
					System.err.println("Error reading background " + props + ": " + ex);
				}
			}
		}
	}

	public static enum SlideType
	{
		TYPE_VERSE(ELT_VERSE),
		TYPE_PRECHORUS(ELT_PRECHORUS),
		TYPE_CHORUS(ELT_CHORUS),
		TYPE_BRIDGE(ELT_BRIDGE),
		TYPE_GENERAL(ELT_GENERAL);

		public final String xmlTag;

		private SlideType(String tag) {
			xmlTag = tag;
		}

		@Override
		public String toString() {
			return Arfaxad.resources.getString("label." + xmlTag);
		}

		public static SlideType fromXmlTag(String tag) {
			for (SlideType td : values()) {
				if (td.xmlTag.equals(tag)) return td;
			}
			return null;
		}

		public static SlideType fromLetter(char letter) {
			for (SlideType td : values()) {
				String l = Arfaxad.resources.getString("key.slide." + td.xmlTag);
				if (l.charAt(0) == letter) return td;
			}
			return null;
		}
	}
}
