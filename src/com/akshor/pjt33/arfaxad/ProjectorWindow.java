package com.akshor.pjt33.arfaxad;

import java.awt.*;
import javax.swing.*;

public class ProjectorWindow extends JWindow
{
	private static final long serialVersionUID = -1953342518132567978L;

	public ProjectorWindow(ModelControl mc) {
		super(chooseScreen());

		// Use full screen if there are two screens.
		// If there's only one screen, use a quarter. This is for debugging.
		GraphicsConfiguration gc = getGraphicsConfiguration();
		Rectangle area = gc.getBounds();
		if (Arfaxad.mainWindow.getGraphicsConfiguration().getDevice() == gc.getDevice()) {
			setBounds(area.x, area.y, area.height * 2 / 3, area.height >> 1);
		}
		else {
			setBounds(area);
		}

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = gbc.weighty = 1;
		Profile p = Arfaxad.profile;
		JComponent contents = mc.new SlideView(1, p.maxSize);
		add(contents, gbc);

		setBackground(Color.BLACK);
		contents.setBackground(Color.BLACK);
	}

	private static GraphicsConfiguration chooseScreen() {
		GraphicsDevice gd = Arfaxad.mainWindow.getGraphicsConfiguration().getDevice();
		if (gd == Arfaxad.screens[0]) gd = Arfaxad.screens[1];
		else gd = Arfaxad.screens[0];
		// Handle the case where there's only one screen
		if (gd == null) gd = Arfaxad.screens[0];

		return gd.getDefaultConfiguration();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void show() {
		GraphicsDevice gd = getGraphicsConfiguration().getDevice();
		if (gd.isFullScreenSupported()) gd.setFullScreenWindow(this);
		super.show();
	}
}
