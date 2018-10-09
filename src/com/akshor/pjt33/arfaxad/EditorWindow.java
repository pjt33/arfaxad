package com.akshor.pjt33.arfaxad;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class EditorWindow extends JFrame
{
	private static final long serialVersionUID = -3432825546577865198L;

	private Song original;
	private JTextField name;
	private JTextField author;
	private JTextField year;
	private JTextField administrator;
	private JCheckBox publicDomain;
	private String hiddenAdmin = "";
	private JEditorPane editor;
	private Font font;
	private Font f100;
	private FontMetrics fm100;
	private int wWithoutMargin;
	private int hWithoutMargin;
	private final Profile p = Arfaxad.profile;

	@SuppressWarnings("deprecation")
	public EditorWindow(Song song) {
		// TODO i18n
		super("Arfaxad: " + (song == null ? "<new song>" : song.title));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// Ask what to do with the changes
				int option = JOptionPane.showOptionDialog(EditorWindow.this,
				             Arfaxad.resources.getString("dialog.closing.editor.msg"),
				             Arfaxad.resources.getString("dialog.closing.editor.title"),
				             JOptionPane.YES_NO_CANCEL_OPTION,
				             JOptionPane.QUESTION_MESSAGE, null, new String[] {
				                 Arfaxad.resources.getString("action.save"),
				                 Arfaxad.resources.getString("action.discard"),
				                 Arfaxad.resources.getString("action.leave.open")
				             },
				             Arfaxad.resources.getString("action.leave.open"));
				if (option == 0) saveChanges();
				else if (option == 1) discardChanges();
			}
		});

		original = song;

		// Initialise the values used to measure font size
		font = Arfaxad.defaultFont;
		if (font == null) throw new IllegalStateException();
		f100 = font.deriveFont(100.f);
		fm100 = getToolkit().getFontMetrics(f100);
		wWithoutMargin = 1024 * (100 - p.marginW - p.marginE) / 100;
		hWithoutMargin = 768 * (100 - p.marginN - p.marginS) / 100;

		setLayout(new GridBagLayout());
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.gridx = 0;
		gbc1.fill = GridBagConstraints.HORIZONTAL;
		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.gridx = 1;
		gbc2.fill = GridBagConstraints.HORIZONTAL;
		gbc2.weightx = 1;
		add(new JLabel(Arfaxad.resources.getString("editor.label.title")), gbc1);
		name = new JTextField();
		name.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				update();
			}

			public void insertUpdate(DocumentEvent e) {
				update();
			}

			public void removeUpdate(DocumentEvent e) {
				update();
			}

			private void update() {
				String title = name.getText();
				if (title.equals("")) {
					// TODO i18n
					title = original == null ? "<new song>" : "<song editor>";
				}
				EditorWindow.this.setTitle("Arfaxad: " + title);
			}
		});
		if (original != null) name.setText(original.title);
		add(name, gbc2);

		add(new JLabel(Arfaxad.resources.getString("editor.label.author")), gbc1);
		author = new JTextField();
		if (original != null) author.setText(original.author);
		add(author, gbc2);

		add(new JLabel(Arfaxad.resources.getString("editor.label.year")), gbc1);
		year = new JTextField();
		if (original != null && original.year != -1) year.setText(Integer.toString(original.year));
		add(year, gbc2);

		add(new JLabel(Arfaxad.resources.getString("editor.label.administrator")), gbc1);
		administrator = new JTextField();
		if (original != null) administrator.setText(original.administrator);
		add(administrator, gbc2);

		add(new JLabel(Arfaxad.resources.getString("editor.label.public.domain")), gbc1);
		publicDomain = new JCheckBox();
		publicDomain.setSelected(original != null ? original.publicDomain : false);
		if (publicDomain.isSelected()) {
			hiddenAdmin = administrator.getText();
			administrator.setText("");
			administrator.setEnabled(false);
		}
		publicDomain.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean b = publicDomain.isSelected();
				administrator.setEnabled(!b);
				if (b) {
					hiddenAdmin = administrator.getText();
					administrator.setText("");
				}
				else {
					administrator.setText(hiddenAdmin);
				}
			}
		});
		add(publicDomain, gbc2);

		gbc1.gridwidth = 2;
		gbc1.weighty = 1;
		gbc1.fill = GridBagConstraints.BOTH;
		editor = new JEditorPane();
		editor.setEditorKit(new StyledEditorKit());
		editor.setDocument(new SongDocument(original));
		add(new JScrollPane(editor), gbc1);

		JPanel jp = new JPanel();
		gbc1.weighty = 0;
		add(jp, gbc1);

		JButton save = new JButton(Arfaxad.resources.getString("action.save.changes"));
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveChanges();
			}
		});
		JButton discard = new JButton(Arfaxad.resources.getString("action.discard.changes"));
		discard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				discardChanges();
			}
		});
		jp.setLayout(new GridBagLayout());
		gbc1.gridwidth = 1;
		gbc1.gridx = GridBagConstraints.RELATIVE;
		jp.add(save, gbc1);
		jp.add(discard, gbc1);

		pack();
		Dimension d = getPreferredSize();
		if (d.width < 640) d.width = 640;
		if (d.height < 480) d.height = 480;
		setSize(d);

		// Centre on the main window
		Rectangle position = Arfaxad.mainWindow.getBounds();
		setLocation(new Point(position.x + ((position.width - d.width) >> 1),
		                      position.y + ((position.height - d.height) >> 1)));
	}

	private void discardChanges() {
		EditorWindow.this.setVisible(false);
		EditorWindow.this.dispose();
	}

	private void saveChanges() {
		Song song = ((SongDocument)editor.getDocument()).getSong();
		if (song.slides.size() > 0 && !song.equals(original)) {
			MainWindow mw = Arfaxad.mainWindow;

			if (original != null) {
				mw.deleteSongFromLists(original);

				// Replace in the song schedule
				DefaultListModel<Song> lm = mw.scheduleLM;
				for (int i = lm.getSize() - 1; i >= 0; i--) {
					if (original.equals(lm.getElementAt(i))) lm.set(i, song);
				}

				// Replace in next song's model-control
				Arfaxad.nextMC.replace(original, song);
			}

			Arfaxad.songs.add(song);
			DefaultListModel<Bookmark> bookmarksLM = (DefaultListModel<Bookmark>)mw.bookmarks.getModel();
			for (Bookmark bookmark : song.bookmarks())
				insert(bookmarksLM, bookmark);

			// Select song
			mw.bookmarks.setSelectedValue(song, true);

			// Store to disk
			if (original != null) song.file = original.file;
			else {
				File dir = Arfaxad.profile.songDir;
				// TODO Improve handling of non-ASCII chars
				String base = song.title.replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]", "-");
				File file = new File(dir, base + ".xml");
				if (file.exists()) {
					int i = 1;
					do {
						file = new File(dir, base + "-" + i + ".xml");
						i++;
					} while (file.exists());
				}
				song.file = file;
			}
			if (song.file != null) {
				try {
					OutputStream os = new BufferedOutputStream(new FileOutputStream(song.file));
					song.write(os);
					os.close();
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
		EditorWindow.this.setVisible(false);
		EditorWindow.this.dispose();
	}

	@SuppressWarnings(value = { "unchecked" })
	private void insert(DefaultListModel<Bookmark> lm, Bookmark obj) {
		int len = lm.getSize();
		if (len == 0) {
			lm.addElement(obj);
			return;
		}
		if (Bookmark.COMPARATOR.compare(obj, lm.get(0)) < 0) lm.insertElementAt(obj, 0);
		else if (Bookmark.COMPARATOR.compare(obj, lm.get(len - 1)) > 0) lm.insertElementAt(obj, len);
		else {
			int lt = 0;
			int gt = len - 1;
			while (gt - lt > 1) {
				int mid = (lt + gt) >> 1;
				int cmp = Bookmark.COMPARATOR.compare(obj, lm.get(mid));
				if (cmp == 0) return;
				else if (cmp < 0) gt = mid;
				else lt = mid;
			}
			lm.insertElementAt(obj, gt);
		}
	}

	private class SongDocument extends DefaultStyledDocument
	{
		private static final long serialVersionUID = -548191334027132864L;

		private Map<String, Song.SlideType> labels;
		{
			labels = new HashMap<String, Song.SlideType>();
			for (Song.SlideType type : Song.SlideType.values()) {
				labels.put(type.toString(), type);
			}
		}

		public SongDocument(Song song) {
			if (song == null) return;
			StringBuilder sb = new StringBuilder();
			for (Song.Slide slide : song.slides) {
				sb.append(slide.type.toString());
				if (slide.backgroundSrc != null) sb.append("{" + slide.backgroundSrc + "}");
				sb.append("\n");
				for (AttributedText line : slide.lines)
					sb.append(line.encode() + "\n");
				sb.append("\n");
			}
			try {
				insertString(0, sb.toString().trim(), null);
			}
			catch (BadLocationException ble) {
				throw new IllegalStateException(ble);
			}
		}

		@Override
		public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
			super.insertString(offset, str, a);
			refresh();
		}

		@Override
		public void remove(int off, int len) throws BadLocationException {
			super.remove(off, len);
			refresh();
		}

		private void refresh() {
			AttributeSet emptyStyle = new SimpleAttributeSet();
			MutableAttributeSet labelStyle = new SimpleAttributeSet();
			StyleConstants.setBold(labelStyle, true);
			Graphics2D g2d = (Graphics2D)getGraphics();
			FontRenderContext frc = g2d != null ? g2d.getFontRenderContext()
			                                    : new FontRenderContext(new AffineTransform(), true, false);

			int len = getLength();
			setParagraphAttributes(0, len + 1, emptyStyle, true);

			int h = fm100.getHeight();

			try {
				String str = getText(0, len);
				// Start and end of the current line
				int start = 0, end;
				// Start, end, and line count of the current slide
				int slideStart = -1, slideEnd = -1, slideLines = 0, emptyLines = 0;
				do {
					end = str.indexOf('\n', start);
					if (end == -1) end = len;
					String sub = str.substring(start, end);
					boolean lineIsLabel = labels.containsKey(sub);
					if (!lineIsLabel) {
						int idx = sub.indexOf("{");
						if (idx > -1 && sub.endsWith("}") && labels.containsKey(sub.substring(0, idx))) {
							lineIsLabel = true;
						}
					}
					if (lineIsLabel) {
						setParagraphAttributes(start, end - start, labelStyle, false);
						if (slideStart >= 0) {
							colourText(slideStart, slideEnd, 100 * hWithoutMargin / (float)(h * slideLines));
						}
						slideStart = start;
						slideEnd = end;
						slideLines = emptyLines = 0;
					}
					else if (sub.length() > 0) {
						slideLines += 1 + emptyLines;
						emptyLines = 0;

						colourText(start, end,
						    100 * wWithoutMargin / new TextLayout(AttributedText.decode(sub).iterator(f100), frc).getVisibleAdvance());
					}
					else emptyLines++;
					start = end + 1;
				} while (end < len);

				if (slideStart >= 0) colourText(slideStart, slideEnd, 100 * hWithoutMargin / (float)(h * slideLines));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void colourText(int start, int end, float size) {
			MutableAttributeSet lineStyle = new SimpleAttributeSet();
			int min80pc = p.minSize * 4 / 5;
			if (size < min80pc) {
				StyleConstants.setForeground(lineStyle, Color.RED);
				StyleConstants.setBold(lineStyle, true);
			}
			else if (size < p.minSize) {
				int gb = (int)(128 * (size - min80pc) / (p.minSize - min80pc));
				StyleConstants.setForeground(lineStyle, new Color(255, gb, gb));
				StyleConstants.setBold(lineStyle, true);
			}
			else if (size < p.maxSize) {
				int rg = (int)(128 * (p.maxSize - size) / (p.maxSize - p.minSize));
				StyleConstants.setForeground(lineStyle, new Color(rg, rg, 255));
			}
			else return;

			setParagraphAttributes(start, end - start, lineStyle, false);
		}

		public Song getSong() {
			Song song = new Song();
			song.title = name.getText();
			song.author = author.getText();
			if ("".equals(song.author)) song.author = null;
			song.administrator = administrator.getText();
			if ("".equals(song.administrator)) song.administrator = null;
			song.publicDomain = publicDomain.isSelected();
			if ("".equals(year.getText())) song.year = -1;
			else {
				try {
					song.year = Integer.parseInt(year.getText());
				}
				catch (NumberFormatException nfe) {
					// TODO i18n
					JOptionPane.showMessageDialog(EditorWindow.this, "Illegal year: " + year.getText(), "Error",
					                              JOptionPane.ERROR_MESSAGE);
					song.year = -1;
				}
			}
			if (original != null) {
				song.transition = original.transition;
				song.just = original.just;
				song.backgroundSrc = original.backgroundSrc;
				song.background = original.background;
				song.colourName = original.colourName;
				song.colour = original.colour;
			}

			boolean firstLine = true;
			Song.Slide slide = null;
			int len = getLength();
			try {
				String str = getText(0, len);
				int start = 0, end;
				do {
					end = str.indexOf('\n', start);
					if (end == -1) end = len;
					String sub = str.substring(start, end);

					String labelType = sub;
					String props = null;
					boolean lineIsLabel = labels.containsKey(sub);
					if (!lineIsLabel) {
						int idx = sub.indexOf("{");
						if (idx > -1 && sub.endsWith("}") && labels.containsKey(sub.substring(0, idx))) {
							lineIsLabel = true;
							labelType = sub.substring(0, idx);
							props = sub.substring(idx + 1, sub.length() - 1);
						}
					}

					if (lineIsLabel) {
						slide = song.new Slide(labels.get(labelType));
						slide.setProperties(props);
						song.slides.add(slide);
					}
					else {
						if (firstLine) {
							slide = song.new Slide(Song.SlideType.TYPE_GENERAL);
							song.slides.add(slide);
						}
						slide.lines.add(AttributedText.decode(sub));
					}
					firstLine = false;
					start = end + 1;
				} while (end < len);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			// "Trim"
			Iterator<Song.Slide> slideIt = song.slides.iterator();
			while (slideIt.hasNext()) {
				Song.Slide d = slideIt.next();
				java.util.List<AttributedText> lines = d.lines;
				int sz = lines.size();
				while (sz > 0 && "".equals(lines.get(sz - 1).toString())) {
					lines.remove(sz - 1);
					sz--;
				}
				while (sz > 0 && "".equals(lines.get(0).toString())) {
					lines.remove(0);
					sz--;
				}
				// We permit empty slides with backgrounds, because it's the only way to
				// show an image other than the logo
				if (lines.isEmpty() && d.background == null) slideIt.remove();
			}

			return song;
		}
	}
}
