package com.akshor.pjt33.arfaxad;

import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;

public class ListPopupListener extends MouseAdapter
{
	private JPopupMenu jpm;
	private Song song;

	public ListPopupListener() {
		jpm = new JPopupMenu();
		JMenuItem edit = new JMenuItem(Arfaxad.resources.getString("action.edit"));
		edit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				new EditorWindow(song).setVisible(true);
			}
		});
		jpm.add(edit);
	}

	@Override
	public void mousePressed(MouseEvent evt) {
		if (evt.isPopupTrigger()) show(evt);
	}

	@Override
	public void mouseReleased(MouseEvent evt) {
		if (evt.isPopupTrigger()) show(evt);
	}

	private void show(MouseEvent evt) {
		Component c = evt.getComponent();
		if (!(c instanceof JList)) return;
		JList list = (JList)c;
		int idx = list.locationToIndex(evt.getPoint());
		if (idx == -1) return;
		list.setSelectedIndex(idx);
		Object obj = list.getModel().getElementAt(idx);
		song = null;
		if (obj instanceof Song) song = (Song)obj;
		else if (obj instanceof Song.Slide) song = ((Song.Slide)obj).song();
		if (song == null) return;

		jpm.show(c, evt.getX(), evt.getY());
	}
}
