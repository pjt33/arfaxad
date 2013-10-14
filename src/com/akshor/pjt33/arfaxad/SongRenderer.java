package com.akshor.pjt33.arfaxad;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;
import java.util.*;

public class SongRenderer extends Renderer
{
	private Font f100;
	private FontMetrics fm100;
	private Map<Rectangle2D, Font> scaledFonts = new HashMap<Rectangle2D, Font>();

	/**
	 * Clears the cached font sizes for the current song.
	 */
	void reset() {
		scaledFonts.clear();
	}

	@Override
	public void render(Graphics2D g2d, Profile p, ModelControl mc) {
		Song song = mc.song();
		if (song == null) return;

		int slideIdx = mc.slide();
		boolean showCopyright = mc.isShowingCopyright();

		Rectangle2D r2d = g2d.getClipBounds();
		float wWithoutMargin = (float)r2d.getWidth();
		float hWithoutMargin = (float)r2d.getHeight();

		renderBackground(g2d, song.getBackground(slideIdx));

		boolean justifyLeft = p.justifyLeft;
		if (song.just == Song.JUSTIFY_LEFT) justifyLeft = true;
		if (song.just == Song.JUSTIFY_CENTRE) justifyLeft = false;

		if (f100 == null) {
			try {
				f100 = g2d.getFont().deriveFont(100.f);
				fm100 = g2d.getFontMetrics(f100);
			}
			catch (Exception e) {
				System.err.println(e);
				return;
			}
		}
		Font f = selectFont(g2d, p, song);

		g2d.setFont(f);
		g2d.setColor(song.colour);
		Song.Slide slide = song.slides.get(slideIdx);
		float ratio = f.getSize2D() / 100;

		// Top-align. (TODO Make this configurable in the profile).
		float h = fm100.getHeight() * ratio;
		float y = fm100.getAscent() * ratio;
		FontRenderContext frc = g2d.getFontRenderContext();
		for (AttributedText line : slide.lines) {
			float x;
			if (justifyLeft) x = 0;
			else {
				float w = new TextLayout(line.iterator(f), frc).getVisibleAdvance();
				x = (wWithoutMargin - w) / 2;
			}
			g2d.drawString(line.iterator(f), x, y);
			g2d.setColor(song.colour);
			y += h;
		}

		// Show copyright information
		if (showCopyright) {
			String copyrightString = song.getCopyrightString();
			if (copyrightString.length() > 0) {
				float copyrightSize = p.copyrightSize * wWithoutMargin / 1024f;
				g2d.setFont(f.deriveFont(copyrightSize));
				// Position: left, bottom.
				// TODO May cause problems for some songs. Need better (factored out) layout engine.
				float x = 0;
				y = hWithoutMargin - fm100.getDescent() * copyrightSize / 100f;
				g2d.drawString(copyrightString, x, y);
			}
		}
	}

	private Font selectFont(Graphics2D g2d, Profile p, Song song) {
		Rectangle2D r2d = g2d.getClipBounds();
		Font f = scaledFonts.get(r2d);
		if (f != null) return f;

		float wWithoutMargin = (float)r2d.getWidth();
		float hWithoutMargin = (float)r2d.getHeight();
		FontRenderContext frc = g2d.getFontMetrics(f100).getFontRenderContext();

		// Obtain a suitably sized font
		int h = fm100.getHeight();
		int hmax = 0;
		float wmax = 0;
		for (Song.Slide slide : song.slides) {
			int hSlide = h * slide.lines.size();
			if (hSlide > hmax) hmax = hSlide;
			for (AttributedText line : slide.lines) {
				float w = new TextLayout(line.iterator(f100), frc).getVisibleAdvance();
				if (w > wmax) wmax = w;
			}
		}

		// Calculate the font size which we can use.
		// 99 rather than 100 to allow some margin for font scaling issues
		float sizeW = 99 * wWithoutMargin / wmax;
		float sizeH = 99 * hWithoutMargin / hmax;
		float size = sizeW < sizeH ? sizeW : sizeH;

		// TODO This calculation should take into account the screen size vs the assumed 1024x768
		if (size > p.maxSize) size = p.maxSize;

		f = f100.deriveFont(size);
		scaledFonts.put(r2d, f);
		return f;
	}
}
