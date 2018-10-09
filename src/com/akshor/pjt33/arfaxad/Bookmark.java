package com.akshor.pjt33.arfaxad;

import java.text.CollationKey;
import java.util.Comparator;

interface Bookmark
{
	public Song song();
	public int slide();
	public CollationKey collationKey();

	public static Comparator<Bookmark> COMPARATOR = new Comparator<Bookmark>() {
		public int compare(Bookmark a, Bookmark b) {
			if (a == b) return 0;

			int cmp = a.collationKey().compareTo(b.collationKey());
			if (cmp != 0) return cmp;

			cmp = a.song().collationKey().compareTo(b.song().collationKey());
			if (cmp != 0) return cmp;

			cmp = a.slide() - b.slide();
			if (cmp != 0) return cmp;

			if (a instanceof Song) return -1;
			if (b instanceof Song) return 1;
			return 0;
		}
	};
}
