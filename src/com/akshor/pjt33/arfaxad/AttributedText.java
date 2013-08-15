package com.akshor.pjt33.arfaxad;

import java.awt.Font;
import java.awt.font.*;
import java.awt.geom.AffineTransform;
import java.text.*;
import java.util.*;
import java.util.regex.*;

/**
 * Replacement for AttributedString.
 */
public class AttributedText
{
	private static final Pattern pattern = Pattern.compile("\\[([^]]*)\\]");
	private static final String TAG_ITALIC = "i";
	private static final String TAG_SUPERSCRIPT = "sup";
	private static final boolean JAVA16;
	static {
		boolean java16 = true;
		String specVendor = System.getProperty("java.specification.vendor");
		// I don't know the precise limits of the bug for which we need to know the version.
		// Is this necessary? Is it sufficient?
		if (!"Sun Microsystems Inc.".equals(specVendor)) java16 = false;
		String classVersion = System.getProperty("java.class.version");
		double d = Double.parseDouble(classVersion);
		if (d < 50) java16 = false;
		JAVA16 = java16;
	}

	private final String text;
	// NB Always normalised
	private final SortedSet<Attribute> attributes = new TreeSet<Attribute>();

	public AttributedText(String text) {
		this.text = text;
	}

	public AttributedText(AttributedText at) {
		this.text = at.text;
		attributes.addAll(at.attributes);
	}

	public static AttributedText decode(String coded) {
		StringBuilder sb = new StringBuilder();
		List<Attribute> attributes = new LinkedList<Attribute>();
		Map<String, Integer> tags = new HashMap<String, Integer>();
		Matcher m = pattern.matcher(coded);
		int off = 0;
		while (m.find()) {
			sb.append(coded.substring(off, m.start()));
			off = m.end();

			int pos = sb.length();
			String tag = m.group(1);
			if (tag.startsWith("/")) {
				tag = tag.substring(1);
				Integer p = tags.get(tag);
				if (p != null) {
					AttributedCharacterIterator.Attribute attr = null;
					Object val = null;
					if (TAG_ITALIC.equals(tag)) {
						attr = TextAttribute.POSTURE;
						val = TextAttribute.POSTURE_OBLIQUE;
					}
					else if (TAG_SUPERSCRIPT.equals(tag)) {
						attr = TextAttribute.SUPERSCRIPT;
						val = TextAttribute.SUPERSCRIPT_SUPER;
					}
					if (attr != null) {
						try {
							attributes.add(new Attribute(attr, val, p.intValue(), pos));
						}
						catch (IllegalArgumentException iae) {
							sb.append("[" + tag + "][/" + tag + "]");
						}
					}
				}
				tags.remove(tag);
			}
			else if (!tags.containsKey(tag)) {
				tags.put(tag, Integer.valueOf(pos));
			}
		}

		sb.append(coded.substring(off));
		for (Map.Entry<String, Integer> entry : tags.entrySet()) {
			AttributedCharacterIterator.Attribute attr = null;
			Object val = null;
			String tag = entry.getKey();
			if (TAG_ITALIC.equals(tag)) {
				attr = TextAttribute.POSTURE;
				val = TextAttribute.POSTURE_OBLIQUE;
			}
			else if (TAG_SUPERSCRIPT.equals(tag)) {
				attr = TextAttribute.SUPERSCRIPT;
				val = TextAttribute.SUPERSCRIPT_SUPER;
			}

			if (attr != null) {
				try {
					attributes.add(new Attribute(attr, val, entry.getValue().intValue(), sb.length()));
				}
				catch (IllegalArgumentException iae) {
					sb.append("[" + tag + "][/" + tag + "]");
				}
			}
		}

		AttributedText at = new AttributedText(sb.toString());
		at.attributes.addAll(attributes);
		return at;
	}

	public String encode() {
		StringBuilder sb = new StringBuilder();
		Set<Attribute> active = new HashSet<Attribute>();
		int closeIdx = text.length();
		int i = 0;
		for (Attribute a : attributes) {
			while (i < a.start) {
				if (closeIdx >= a.start) {
					sb.append(text.substring(i, a.start));
					i = a.start;
				}
				else {
					sb.append(text.substring(i, closeIdx));
					i = closeIdx;
					closeIdx = text.length();
					Iterator<Attribute> it = active.iterator();
					while (it.hasNext()) {
						Attribute b = it.next();
						if (b.end == i) {
							it.remove();
							sb.append("[/");
							if (b.attr.equals(TextAttribute.POSTURE)) sb.append(TAG_ITALIC);
							else if (b.attr.equals(TextAttribute.SUPERSCRIPT)) sb.append(TAG_SUPERSCRIPT);
							else throw new IllegalStateException();
							sb.append("]");
						}
						else if (b.end < closeIdx) closeIdx = b.end;
					}
				}
			}

			active.add(a);
			if (a.end < closeIdx) closeIdx = a.end;
			sb.append("[");
			if (a.attr.equals(TextAttribute.POSTURE)) sb.append(TAG_ITALIC);
			else if (a.attr.equals(TextAttribute.SUPERSCRIPT)) sb.append(TAG_SUPERSCRIPT);
			else throw new IllegalStateException();
			sb.append("]");
		}

		while (i < text.length()) {
			sb.append(text.substring(i, closeIdx));
			i = closeIdx;
			closeIdx = text.length();
			Iterator<Attribute> it = active.iterator();
			while (it.hasNext()) {
				Attribute b = it.next();
				if (b.end == i) {
					it.remove();
					sb.append("[/");
					if (b.attr.equals(TextAttribute.POSTURE)) sb.append(TAG_ITALIC);
					else if (b.attr.equals(TextAttribute.SUPERSCRIPT)) sb.append(TAG_SUPERSCRIPT);
					else throw new IllegalStateException();
					sb.append("]");
				}
				else if (b.end < closeIdx) closeIdx = b.end;
			}
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return text;
	}

	public AttributedCharacterIterator iterator(Font font) {
		AttributedString as = new AttributedString(text);
		// To avoid IllegalArgumentException: Can't add attribute to 0-length text
		if ("".equals(text)) as = new AttributedString(" ");
		// NB: must not use TextAttribute.FONT because then it's not easy to use POSTURE, etc.
		as.addAttribute(TextAttribute.FAMILY, font.getFamily());
		float size = font.getSize2D();
		as.addAttribute(TextAttribute.SIZE, Float.valueOf(size));

		// NB: according to the doc for 1.5, it shouldn't be necessary to take the size into account;
		// however, it is necessary in 1.5 and 1.6
		float dy = -size * 0.4f;
		float scale = 0.6f;
		TransformAttribute sup = new TransformAttribute(new AffineTransform(scale, 0f, 0f, scale, 0f, dy));

		for (Attribute a : attributes) {
			if (a.attr.equals(TextAttribute.SUPERSCRIPT) && a.val.equals(TextAttribute.SUPERSCRIPT_SUPER)) {
				as.addAttribute(TextAttribute.TRANSFORM, sup, a.start, a.end);
				// Bug #6529024
				if (JAVA16 && a.end < text.length()) {
					as.addAttribute(TextAttribute.TRANSFORM, new TransformAttribute(new AffineTransform(1f, 0f, 0f, 1f,
					                0f, -dy)),
					                a.end, a.end + 1);
				}
			}
			else as.addAttribute(a.attr, a.val, a.start, a.end);
		}

		return as.getIterator();
	}

	private static class Attribute implements Comparable<Attribute>
	{
		public final AttributedCharacterIterator.Attribute attr;
		public final Object val;
		public final int start;
		public final int end;

		public Attribute(AttributedCharacterIterator.Attribute attr, Object val, int start, int end) {
			if (start >= end) throw new IllegalArgumentException();
			this.attr = attr;
			this.val = val;
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString() {
			return "{{ [" + start + "-" + end + "): " + attr + "=" + val + " }}";
		}

		public int compareTo(Attribute a) {
			if (a == this) return 0;

			// Earlier start => earlier attribute
			if (start != a.start) return start - a.start;
			// Later end => later attribute (in order that an attribute whose scope includes that of another comes
			// first)
			if (end != a.end) return a.end - end;
			// Arbitrary
			if (attr.getClass() != a.attr.getClass()) {
				return attr.getClass().getName().compareTo(a.attr.getClass().getName());
			}
			if (attr.hashCode() != a.attr.hashCode()) return attr.hashCode() - a.attr.hashCode();
			// It's almost certain that the only difference is the value
			if (val == null ^ a.val == null) return (val == null) ? -1 : 1;
			if (val == null) return 0;
			if (val.getClass() != a.val.getClass()) {
				return val.getClass().getName().compareTo(a.val.getClass().getName());
			}
			return val.hashCode() - a.val.hashCode();
		}
	}
}
