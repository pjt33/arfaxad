package com.akshor.pjt33.arfaxad;

import java.awt.Color;
import java.awt.event.*;
import javax.swing.*;

public class BorderChanger implements FocusListener
{
	private final JComponent bordered;

	public BorderChanger(JComponent bordered) {
		this.bordered = bordered;
		focusLost(null);
	}

	public void focusGained(FocusEvent e) {
		bordered.setBorder(BorderFactory.createLineBorder(Color.RED));
	}

	public void focusLost(FocusEvent e) {
		bordered.setBorder(BorderFactory.createLineBorder(Color.GRAY));
	}
}
