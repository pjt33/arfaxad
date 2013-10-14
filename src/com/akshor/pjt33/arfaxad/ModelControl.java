package com.akshor.pjt33.arfaxad;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;
import static java.awt.event.KeyEvent.*;

public class ModelControl
{
	private Song song;
	private int slide;
	private boolean showCopyright;
	private javax.swing.Timer timer;
	private Font font;
	private final SongRenderer mainRenderer = new SongRenderer();
	private Renderer specialRenderer;
	// WeakHashSets.
	private WeakHashMap<View, Void> views = new WeakHashMap<View, Void>();
	private WeakHashMap<SongListener, Void> listeners = new WeakHashMap<SongListener, Void>();

	public void setSong(Song s, int initialSlide, Font f) {
		if (s != null) {
			// It would be very easy to say that we want to set showCopyright=true.
			// However, there is a use case when we correct the lyrics of the current song, and in this case
			// we want to treat setSong like setSlide.
			// So there are three cases:
			// 1. Different song: showCopyright=true
			// 2. Same song, different slide: showCopyright=false
			// 3. Same song, same slide: showCopyright doesn't change
			// But how can we tell if it's the same song, given that it's changed?
			// We can't compare the lyrics, but we can compare the title and the copyright information.
			if (song == null) showCopyright = true;
			else if (!Arfaxad.equal(song.title, s.title)) showCopyright = true;
			else if (song.year != s.year) showCopyright = true;
			else if (!Arfaxad.equal(song.administrator, s.administrator)) showCopyright = true;
			else if (song.publicDomain != s.publicDomain) showCopyright = true;
			else if (slide != initialSlide) showCopyright = false;
		}

		song = s;
		slide = initialSlide;
		font = f;
		for (View view : views.keySet())
			view.refresh(false);
		SongEvent evt = new SongEvent(s);
		for (SongListener cl : listeners.keySet())
			cl.songChanged(evt);
		mainRenderer.reset();

		// Log copyright.
		if (this == Arfaxad.currentMC && getRenderer() instanceof SongRenderer &&
		    Arfaxad.profile != null && Arfaxad.profile.log != null) {
			Arfaxad.profile.log.log(song);
		}

		// Update Timer.
		if (timer != null) {
			timer.stop();
			timer = null;
		}

		if (this == Arfaxad.currentMC && s.transition > 0) {
			timer = new javax.swing.Timer(s.transition * 1000, new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					int d = slide + 1;
					if (d >= song.slides.size()) d = 0;
					setSlide(d, true);
				}
			});
			timer.start();
		}
	}

	public void setSong(ModelControl mc) {
		if (mc.song != null) setSong(mc.song, mc.slide, mc.font);
	}

	public Song song() {
		return song;
	}

	public void addSongListener(SongListener songListener) {
		listeners.put(songListener, null);
	}

	public void replace(Song oldSong, Song newSong) {
		if (song.equals(oldSong)) setSong(newSong, 0, font);
	}

	public boolean isShowingCopyright() {
		return showCopyright;
	}

	public void showCopyright() {
		showCopyright = true;
		for (View view : views.keySet())
			view.refresh(false);
	}

	void setSlide(int _slide, boolean automatic) {
		if (song == null) return;
		if (_slide < 0) return;
		if (_slide >= song.slides.size()) return;
		// If we see a change of slide, we assume that the copyright information has already been displayed.
		// It's important to check renderer because we want the information to be shown once.
		if (_slide != slide && getRenderer() instanceof SongRenderer) showCopyright = false;
		slide = _slide;
		for (View view : views.keySet())
			view.refresh(automatic);
	}

	public int slide() {
		return slide;
	}

	public Renderer getRenderer() {
		return specialRenderer == null ? mainRenderer : specialRenderer;
	}

	public boolean toggleSpecialRenderer(Renderer renderer) {
		specialRenderer = renderer.equals(specialRenderer) ? null : renderer;
		for (View view : views.keySet())
			view.refresh(false);
		return renderer.equals(specialRenderer);
	}

	public JComponent complexView(String name, JToolBar tools) {
		JPanel view = new JPanel();
		view.setLayout(new GridBagLayout());
		tools.setFloatable(false);
		ListView lv = new ListView();
		lv.setName(name + ".list");
		JScrollPane jsp = new JScrollPane(lv, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		SlideView sv = new SlideView();
		JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, jsp, sv);
		main.setDividerLocation(0.8);
		main.setResizeWeight(0.8);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.BOTH;
		view.add(tools, gbc);
		gbc.weighty = 1;
		view.add(main, gbc);

		StringBuilder sb = new StringBuilder();
		processKeys(view, sb);
		processKeys(lv, sb);
		processKeys(sv, sb);

		lv.addFocusListener(new BorderChanger(view));

		return view;
	}

	private void processKeys(JComponent view, final StringBuilder cmd) {
		// Given how Swing works, we should use InputMap and ActionMap to handle keys.
		// However, working out which InputMap to change is a nightmare, so we fall back on a technique which works.
		view.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent evt) {
				if ((evt.getModifiers() & CTRL_MASK) != 0) return;

				int tec = evt.getKeyCode();
				if (tec >= VK_0 && tec <= VK_9) cmd.append((char)('0' + tec - VK_0));
				else if (tec >= VK_A && tec <= VK_Z) {
					cmd.setLength(0);
					cmd.append((char)('a' + tec - VK_A));
				}
				else if (tec == VK_ENTER) {
					if (song != null) setSlide(song.slide(cmd.toString()), false);
					cmd.setLength(0);
				}
				else if (tec == VK_UP) setSlide(slide - 1, false);
				else if (tec == VK_DOWN) setSlide(slide + 1, false);
				else if (tec == VK_PAGE_DOWN) Arfaxad.mainWindow.nextSong();
				else return;

				evt.consume();
			}
		});
	}

	static interface View
	{
		public void refresh(boolean automatic);
	}

	class SlideView extends JPanel implements View
	{
		private static final long serialVersionUID = 3479780504057451490L;

		public SlideView() {
			views.put(this, null);
			refresh(false);

			addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent ce) {
					refresh(true);
				}
			});
		}

		public SlideView(int min_font, int max_font) {
			this();
		}

		public void refresh(boolean automatic) {
			repaint();
		}

		@Override
		public void paintComponent(Graphics g) {
			if (!(g instanceof Graphics2D)) throw new IllegalArgumentException("Graphics2D needed");
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Font f = font;
			if (f == null) f = getFont();
			if (Arfaxad.defaultFont == null) Arfaxad.defaultFont = getFont();
			if (!isVisible() || f == null) return;

			int myW = getWidth(), myH = getHeight();
			int wAvailable = myW, hAvailable = myH;
			// Maintain 4:3 ratio
			if (wAvailable > hAvailable * 4 / 3) wAvailable = hAvailable * 4 / 3;
			else if (hAvailable > wAvailable * 3 / 4) hAvailable = wAvailable * 3 / 4;

			// Clear background. It's very very important to do it without antialiasing.
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setColor(getBackground());
			g.fillRect(0, 0, myW, myH);
			g.setColor(Color.BLACK);
			g.fillRect((myW - wAvailable) >> 1, (myH - hAvailable) >> 1, wAvailable, hAvailable);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Discount margins
			Profile p = Arfaxad.profile;
			float offsetLeft = (myW - wAvailable) * 0.5f + wAvailable * p.marginW / 100f;
			float offsetTop = (myH - hAvailable) * 0.5f + hAvailable * p.marginN / 100f;
			float wWithoutMargin = wAvailable * (100 - p.marginW - p.marginE) / 100f;
			float hWithoutMargin = hAvailable * (100 - p.marginN - p.marginS) / 100f;

			// Clip and translate
			g2d.setClip(new Rectangle2D.Float(offsetLeft, offsetTop, wWithoutMargin, hWithoutMargin));
			g2d.translate(offsetLeft, offsetTop);

			Renderer renderer = getRenderer();
			if (renderer != null) renderer.render(g2d, p, ModelControl.this);
		}
	}

	class ListView extends JEditorPane implements View
	{
		private static final long serialVersionUID = -3108305768114723026L;

		private Song prevSong;

		ListView() {
			setEditorKit(new StyledEditorKit());

			views.put(this, null);
			refresh(false);

			setEditable(false);
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent evt) {
					if (song == null) return;
					int pos = viewToModel(new Point(evt.getX(), evt.getY()));
					StyledDocument doc = (StyledDocument)getDocument();
					Element elt = doc.getParagraphElement(pos);
					AttributeSet attr = elt.getAttributes();
					Integer slideAttr = (Integer)attr.getAttribute(KEY_SLIDE);
					if (slideAttr == null) return;
					setSlide(slideAttr.intValue(), false);
					for (View view : views.keySet())
						view.refresh(false);
				}
			});
		}

		public void refresh(boolean automatic) {
			// HACK
			if (!automatic && "right.list".equals(getName())) requestFocus();

			int dot = getCaret().getDot();
			setText("");
			if (song != null) {
				int[] count = new int[Song.SlideType.values().length];
				int[] totalCount = new int[count.length];
				for (Song.Slide slide : song.slides)
					totalCount[slide.type.ordinal()]++;

				try {
					StyledDocument doc = (StyledDocument)getDocument();
					MutableAttributeSet titleStyle = new SimpleAttributeSet();
					StyleConstants.setBold(titleStyle, true);
					StyleConstants.setUnderline(titleStyle, true);
					MutableAttributeSet labelStyle = new SimpleAttributeSet();
					StyleConstants.setBold(labelStyle, true);
					doc.putProperty(Document.TitleProperty, song.title);
					doc.insertString(0, song.title + "\n\n", titleStyle);
					int i = 0;
					for (Song.Slide slide : song.slides) {
						int start = doc.getLength();
						String slideLabel = slide.type.toString();
						if (totalCount[slide.type.ordinal()] > 1) slideLabel += " " + (++count[slide.type.ordinal()]);
						doc.insertString(doc.getLength(), slideLabel + "\n", labelStyle);

						MutableAttributeSet slideAttr = null;
						if (i == ModelControl.this.slide) {
							slideAttr = new SimpleAttributeSet();
							StyleConstants.setBackground(slideAttr, Color.LIGHT_GRAY);
						}
						// TODO Copy attributes?
						for (AttributedText line : slide.lines) {
							doc.insertString(doc.getLength(), line + "\n", slideAttr);
						}
						doc.insertString(doc.getLength(), "\n", slideAttr);

						MutableAttributeSet foo = new SimpleAttributeSet();
						foo.addAttribute(KEY_SLIDE, Integer.valueOf(i++));
						doc.setParagraphAttributes(start, doc.getLength() - start, foo, false);
					}

					MutableAttributeSet foo = new SimpleAttributeSet();
					foo.addAttribute(KEY_SLIDE, Integer.valueOf(i - 1));
					doc.setParagraphAttributes(doc.getLength(), 1, foo, false);

					getCaret().setDot(song.equals(prevSong) ? dot : 0);
				}
				catch (BadLocationException ble) {
					throw new AssertionError(ble);
				}
			}

			prevSong = song;
		}
	}

	public static final Object KEY_SLIDE = new AttributeSet.ParagraphAttribute() {};
}
