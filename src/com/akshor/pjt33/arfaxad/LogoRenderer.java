package com.akshor.pjt33.arfaxad;

import java.awt.Graphics2D;

public class LogoRenderer extends Renderer
{
	@Override
	public void render(Graphics2D g, Profile profile, ModelControl mc) {
		renderBackground(g, profile.logo);
	}
}
