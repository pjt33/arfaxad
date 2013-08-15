package com.akshor.pjt33.arfaxad;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public abstract class Renderer
{
	public abstract void render(Graphics2D g2d, Profile profile, ModelControl mc);

	protected void renderBackground(Graphics2D g2d, BufferedImage background) {
		if (background != null) {
			Rectangle clip = g2d.getClipBounds();
			g2d.drawImage(background, 0, 0, clip.width, clip.height, null);
		}
	}
}
