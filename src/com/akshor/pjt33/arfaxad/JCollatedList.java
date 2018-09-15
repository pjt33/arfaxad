package com.akshor.pjt33.arfaxad;

import java.text.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.text.Position;

/**
 * JList allows typing a prefix of an element to search for the element, but the way it does it isn't suited to
 * languages with accents. This class redefines the method that does the search to use a Collator.
 */
public class JCollatedList<E> extends JList<E>
{
	private static final long serialVersionUID = -4706509093188259378L;

	private RuleBasedCollator c = (RuleBasedCollator)Collator.getInstance();
	{
		c.setStrength(Collator.PRIMARY);
	}

	public JCollatedList() {}

	public JCollatedList(ListModel<E> dataModel) {
		super(dataModel);
	}

	public JCollatedList(E[] listData) {
		super(listData);
	}

	public JCollatedList(Vector<? extends E> listData) {
		super(listData);
	}

	@Override
	public int getNextMatch(String prefix, int startIdx, Position.Bias dir) {
		ListModel<E> model = getModel();
		int max = model.getSize();
		if (prefix == null) throw new IllegalArgumentException();
		if (startIdx < 0 || startIdx >= max) throw new IllegalArgumentException();
		CollationElementIterator preCEI = c.getCollationElementIterator(prefix);
		CollationElementIterator cei = c.getCollationElementIterator("");

		int incr = (dir == Position.Bias.Forward) ? 1 : -1;
		int idx = startIdx;
		do {
			Object o = model.getElementAt(idx);
			if (o != null) {
				String string = o.toString();
				if (string != null) {
					preCEI.reset();
					cei.setText(string);
					while (true) {
						// HACK required because decomposition fails
						int i1;
						do {
							i1 = preCEI.next();
						} while (i1 == 0);
						if (i1 == CollationElementIterator.NULLORDER) return idx;
						int i2;
						do {
							i2 = cei.next();
						} while (i2 == 0);
						if (CollationElementIterator.primaryOrder(i1) != CollationElementIterator.primaryOrder(i2)) break;
					}
				}
			}

			idx += incr;
			if (idx == max) idx = 0;
			if (idx < 0) idx += max;
		} while (idx != startIdx);

		return -1;
	}
}
