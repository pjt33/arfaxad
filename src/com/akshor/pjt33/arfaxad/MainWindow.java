package com.akshor.pjt33.arfaxad;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import static java.awt.event.KeyEvent.*;

public class MainWindow extends JFrame
{
	private static final long serialVersionUID = -2149499588279041235L;

	JList<Bookmark> bookmarks;
	JTabbedPane search;
	DefaultListModel<Song> scheduleLM;
	JList<Song> scheduleList;
	int scheduleIdx;
	// Important: this is the only hard reference to this object
	transient SongListener songListener;

	public MainWindow() {
		super("Arfaxad: " + Arfaxad.profile.name);

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		// Basic structure: three columns
		JPanel containerLeft = new JPanel();
		containerLeft.setName("containerLeft");
		JSeparator leftCentre = new JSeparator(SwingConstants.VERTICAL);
		leftCentre.setName("leftCentre");

		JToolBar toolsCentre = new JToolBar("toolsCentre");
		JComponent containerCentre = Arfaxad.nextMC.complexView("centre", toolsCentre);
		containerCentre.setName("containerCentre");

		JSeparator rightCentre = new JSeparator(SwingConstants.VERTICAL);
		rightCentre.setName("rightCentre");

		JToolBar toolsRight = new JToolBar("toolsRight");
		JComponent containerRight = Arfaxad.currentMC.complexView("right", toolsRight);
		containerRight.setName("containerRight");

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
		gbc.weightx = gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;

		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.gridy = 0;
		gbc2.weighty = 1;
		gbc2.fill = GridBagConstraints.VERTICAL;

		setLayout(new GridBagLayout());
		add(containerLeft, gbc);
		add(leftCentre, gbc2);
		add(containerCentre, gbc);
		add(rightCentre, gbc2);
		add(containerRight, gbc);
		containerLeft.setPreferredSize(new Dimension(0, 0));
		containerCentre.setPreferredSize(new Dimension(0, 0));
		containerRight.setPreferredSize(new Dimension(0, 0));

		ListPopupListener popupLeft = new ListPopupListener();

		// Left column
		// Tools
		JPanel toolsLeft = new JPanel();
		toolsLeft.setLayout(new FlowLayout(FlowLayout.LEFT));
		Action newSong = new AbstractAction(Arfaxad.resources.getString("action.new"),
		Arfaxad.loadIcon("resources/new.png")) {
			private static final long serialVersionUID = -2416909803205370578L;
			{
				putValue(SHORT_DESCRIPTION, getValue(NAME));
			}

			public void actionPerformed(ActionEvent e) {
				new EditorWindow(null).setVisible(true);
			}
		};
		JButton btnNewSong = new JButton(newSong);
		btnNewSong.setText("");
		toolsLeft.add(btnNewSong);

		final Action editSong = new AbstractAction(Arfaxad.resources.getString("action.edit"),
		Arfaxad.loadIcon("resources/edit.png")) {
			private static final long serialVersionUID = -7056472347919811943L;
			{
				putValue(SHORT_DESCRIPTION, getValue(NAME));
			}

			public void actionPerformed(ActionEvent e) {
				new EditorWindow(Arfaxad.nextMC.song()).setVisible(true);
			}
		};
		editSong.setEnabled(false);
		JButton btnEditSong = new JButton(editSong);
		btnEditSong.setText("");
		toolsLeft.add(btnEditSong);

		final Action deleteSong = new AbstractAction(Arfaxad.resources.getString("action.delete"),
		Arfaxad.loadIcon("resources/delete.png")) {
			private static final long serialVersionUID = 2455021502863009535L;
			{
				putValue(SHORT_DESCRIPTION, getValue(NAME));
			}

			public void actionPerformed(ActionEvent e) {
				int sure = JOptionPane.showConfirmDialog(MainWindow.this,
				           Arfaxad.resources.getString("dialog.confirm.delete.msg"),
				           Arfaxad.resources.getString("dialog.confirm.delete.title"),
				           JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (sure == JOptionPane.YES_OPTION) {
					Song song = Arfaxad.nextMC.song();
					// Delete song from the lists
					deleteSongFromLists(song);
					// From the schedule
					DefaultListModel lm = scheduleLM;
					for (int i = lm.getSize() - 1; i >= 0; i--) {
						if (song.equals(lm.getElementAt(i))) lm.remove(i);
					}
					// From nextMC
					Arfaxad.nextMC.replace(song, null);
					// From the disk.
					if (song.file != null) song.file.delete();
				}
			}
		};
		deleteSong.setEnabled(false);
		JButton btnDeleteSong = new JButton(deleteSong);
		btnDeleteSong.setText("");
		toolsLeft.add(btnDeleteSong);

		songListener = new SongListener() {
			public void songChanged(SongEvent evt) {
				editSong.setEnabled(evt.newSong != null);
				deleteSong.setEnabled(evt.newSong != null);
			}
		};
		Arfaxad.nextMC.addSongListener(songListener);

		Dimension imgBtnPreferredSize = new Dimension(22, 22);
		btnNewSong.setPreferredSize(imgBtnPreferredSize);
		btnEditSong.setPreferredSize(imgBtnPreferredSize);
		btnDeleteSong.setPreferredSize(imgBtnPreferredSize);

		// Schedule
		JPanel schedule = new JPanel();
		scheduleLM = new DefaultListModel<Song>();
		scheduleList = new JList<Song>(scheduleLM);
		scheduleList.setName("scheduleList");
		scheduleList.addMouseListener(popupLeft);
		scheduleList.addFocusListener(new BorderChanger(schedule));
		schedule.setLayout(new GridBagLayout());
		schedule.add(scheduleList, gbc);
		JPanel scheduleButtons = new JPanel();
		schedule.add(scheduleButtons, gbc2);
		scheduleButtons.setLayout(new GridBagLayout());
		GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.gridx = 0;
		Dimension buttonDimension = new Dimension(34, 34);
		JButton add = new JButton(Arfaxad.loadIcon("resources/add.png"));
		add.setPreferredSize(buttonDimension);
		scheduleButtons.add(add, gbc3);
		add.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				addSelected();
			}
		});
		JButton remove = new JButton(Arfaxad.loadIcon("resources/remove.png"));
		remove.setPreferredSize(buttonDimension);
		scheduleButtons.add(remove, gbc3);
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				removeSelected();
			}
		});
		JButton up = new JButton(Arfaxad.loadIcon("resources/up.png"));
		up.setPreferredSize(buttonDimension);
		scheduleButtons.add(up, gbc3);
		up.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scheduleUp();
			}
		});
		JButton down = new JButton(Arfaxad.loadIcon("resources/down.png"));
		down.setPreferredSize(buttonDimension);
		scheduleButtons.add(down, gbc3);
		down.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				scheduleDown();
			}
		});
		gbc3.weighty = 1;
		scheduleButtons.add(new JPanel(), gbc3);
		scheduleList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				scheduleIdx = scheduleList.getSelectedIndex();
				if (scheduleIdx != -1) Arfaxad.nextMC.setSong(scheduleList.getSelectedValue(), 0, null);
			}
		});
		scheduleList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					Arfaxad.currentMC.setSong(scheduleList.getSelectedValue(), 0, null);
				}
			}
		});
		scheduleList.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int c = e.getKeyCode();
				int m = e.getModifiers();
				switch (c) {
				case VK_UP:
					if (m == 0) return;
					if (m == SHIFT_MASK) scheduleUp();
					break;

				case VK_DOWN:
					if (m == 0) return;
					if (m == SHIFT_MASK) scheduleDown();
					break;

				case VK_MINUS:
				case VK_DELETE:
					removeSelected();
					break;

				case VK_PAGE_DOWN:
					nextSong();
					break;

				default:
					if (e.getKeyChar() == '-') {
						removeSelected();
						break;
					}
					return;
				}

				e.consume();
			}
		});
		java.util.List<Bookmark> allBookmarks = new ArrayList<Bookmark>();
		for (Song song : Arfaxad.songs)
			allBookmarks.addAll(song.bookmarks());
		Collections.sort(allBookmarks, Bookmark.COMPARATOR);
		DefaultListModel<Bookmark> lmBookmarks = new DefaultListModel<Bookmark>();
		for (Bookmark bookmark : allBookmarks)
			lmBookmarks.addElement(bookmark);
		bookmarks = new JCollatedList<Bookmark>(lmBookmarks);
		bookmarks.setName("bookmarks");
		bookmarks.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bookmarks.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				Bookmark bookmark = bookmarks.getSelectedValue();
				if (bookmark != null) Arfaxad.nextMC.setSong(bookmark.song(), bookmark.slide(), null);
				scheduleIdx = -1;
				scheduleList.clearSelection();
			}
		});
		bookmarks.addMouseListener(popupLeft);
		bookmarks.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					Bookmark bookmark = bookmarks.getSelectedValue();
					if (bookmark != null) Arfaxad.currentMC.setSong(bookmark.song(), bookmark.slide(), null);
				}
			}
		});
		bookmarks.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int c = e.getKeyCode();
				char ch = e.getKeyChar();
				if (c == VK_PAGE_DOWN) {
					nextSong();
					e.consume();
				}
				else if (c == VK_PLUS || ch == '+') {
					addSelected();
					e.consume();
				}
			}
		});
		JScrollPane jspSongs = new JScrollPane(bookmarks,
		                                       JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		                                       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		bookmarks.addFocusListener(new BorderChanger(jspSongs));

		JSplitPane jspLeft = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, schedule, jspSongs);
		jspLeft.setDividerLocation(0.6);
		jspLeft.setResizeWeight(0.6);
		containerLeft.setLayout(new GridBagLayout());
		GridBagConstraints gbcLeft1 = new GridBagConstraints();
		gbcLeft1.gridy = 0;
		gbcLeft1.weightx = 1;
		gbcLeft1.fill = GridBagConstraints.HORIZONTAL;
		containerLeft.add(toolsLeft, gbcLeft1);
		GridBagConstraints gbcLeft2 = new GridBagConstraints();
		gbcLeft2.gridy = 1;
		gbcLeft2.weightx = 1;
		gbcLeft2.weighty = 1;
		gbcLeft2.fill = GridBagConstraints.BOTH;
		containerLeft.add(jspLeft, gbcLeft2);

		// Central column
		toolsCentre.setLayout(new GridBagLayout());
		toolsCentre.add(new JPanel(), gbc);
		JButton goLive = new JButton(Arfaxad.resources.getString("action.live"));
		goLive.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				nextSong();
			}
		});
		toolsCentre.add(goLive, gbc2);

		// Right column: active song
		final JToggleButton btnBlack = new JToggleButton();
		final JToggleButton btnLogo = new JToggleButton();
		Action actionBlack = new AbstractAction(Arfaxad.resources.getString("action.black")) {
			private static final long serialVersionUID = -1541218364645036776L;
			private final BlackRenderer mn = new BlackRenderer();

			public void actionPerformed(ActionEvent evt) {
				boolean showBlack = Arfaxad.currentMC.toggleSpecialRenderer(mn);
				btnBlack.setSelected(showBlack);
				btnLogo.setSelected(false);
			}
		};
		actionBlack.putValue(Action.ACCELERATOR_KEY,
		                     KeyStroke.getKeyStroke(Arfaxad.resources.getString("key.action.black")));
		btnBlack.setAction(actionBlack);
		btnBlack.setSelected(false);

		Action actionLogo = new AbstractAction(Arfaxad.resources.getString("action.logo")) {
			private static final long serialVersionUID = 8956920887251552945L;
			private final LogoRenderer ml = new LogoRenderer();

			public void actionPerformed(ActionEvent evt) {
				boolean showLogo = Arfaxad.currentMC.toggleSpecialRenderer(ml);
				btnBlack.setSelected(false);
				btnLogo.setSelected(showLogo);
			}
		};
		actionLogo.putValue(Action.ACCELERATOR_KEY,
		                    KeyStroke.getKeyStroke(Arfaxad.resources.getString("key.action.logo")));
		btnLogo.setAction(actionLogo);
		actionLogo.actionPerformed(null);

		Action actionCopyright = new AbstractAction("\u00a9") {
			private static final long serialVersionUID = -7719720342565451840L;

			public void actionPerformed(ActionEvent e) {
				Arfaxad.currentMC.showCopyright();
			}
		};
		JButton btnCopyright = new JButton(actionCopyright);

		toolsRight.add(btnBlack);
		toolsRight.add(btnLogo);
		toolsRight.add(btnCopyright);

		// Menus
		JMenuBar menu = new JMenuBar();

		JMenu fileMenu = new JMenu(Arfaxad.resources.getString("menu.file"));
		fileMenu.add(newSong);
		fileMenu.add(editSong);
		fileMenu.add(deleteSong);
		fileMenu.add(new JSeparator());
		JMenuItem exit = new JMenuItem(Arfaxad.resources.getString("action.exit"));
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				System.exit(0);
			}
		});
		fileMenu.add(exit);
		menu.add(fileMenu);

		JMenu projectionMenu = new JMenu(Arfaxad.resources.getString("menu.projection"));
		Action actionShow = new AbstractAction(Arfaxad.resources.getString("action.show")) {
			private static final long serialVersionUID = -5372785380308430051L;

			public void actionPerformed(ActionEvent evt) {
				if (Arfaxad.projectorWindow == null) {
					Arfaxad.projectorWindow = new ProjectorWindow(Arfaxad.currentMC);
					Arfaxad.projectorWindow.show();
				}
				else {
					Arfaxad.projectorWindow.setVisible(false);
					Arfaxad.projectorWindow.dispose();
					Arfaxad.projectorWindow = null;
				}
			}
		};
		actionShow.putValue(Action.ACCELERATOR_KEY,
		                    KeyStroke.getKeyStroke(Arfaxad.resources.getString("key.action.show")));

		projectionMenu.add(actionShow);
		projectionMenu.add(actionBlack);
		projectionMenu.add(actionLogo);
		menu.add(projectionMenu);
		setJMenuBar(menu);

		implementFocusPolicy(this);

		// Done
		pack();
		setSize(1024, 720);
	}

	/**
	 * Implement a simple policy: the JButtons can't receive focus. So the components which can receive it are the
	 * JLists and the JEditorPanes.
	 */
	private static void implementFocusPolicy(Component c) {
		if (c instanceof AbstractButton) c.setFocusable(false);
		if (c instanceof Container) {
			for (Component c2 : ((Container)c).getComponents())
				implementFocusPolicy(c2);
		}
	}

	public void nextSong() {
		Arfaxad.currentMC.setSong(Arfaxad.nextMC);
		if (scheduleIdx >= 0) {
			scheduleIdx++;
			if (scheduleIdx < scheduleLM.size()) {
				scheduleList.setSelectedIndex(scheduleIdx);
				Arfaxad.nextMC.setSong(scheduleList.getSelectedValue(), 0, null);
			}
			else {
				scheduleIdx = -1;
				scheduleList.clearSelection();
				Arfaxad.nextMC.setSong(null, 0, null);
			}
		}
	}

	private void scheduleUp() {
		int i = scheduleList.getSelectedIndex();
		if (i > 0) {
			// Better like this than with remove and add because DefaultListModel uses Vector
			Song a = scheduleLM.get(i - 1);
			Song b = scheduleLM.get(i);
			scheduleLM.set(i, a);
			scheduleLM.set(i - 1, b);
			scheduleList.setSelectedIndex(i - 1);
			scheduleIdx = i - 1;
		}
	}

	private void scheduleDown() {
		int i = scheduleList.getSelectedIndex();
		if (i < scheduleLM.size() - 1) {
			// Better like this than with remove and add because DefaultListModel uses Vector
			Song a = scheduleLM.get(i + 1);
			Song b = scheduleLM.get(i);
			scheduleLM.set(i, a);
			scheduleLM.set(i + 1, b);
			scheduleList.setSelectedIndex(i + 1);
			scheduleIdx = i + 1;
		}
	}

	private void addSelected() {
		Bookmark bookmark = bookmarks.getSelectedValue();
		if (bookmark != null) scheduleLM.addElement(bookmark.song());
	}

	private void removeSelected() {
		int i = scheduleList.getSelectedIndex();
		if (i >= 0 && i < scheduleLM.size()) scheduleLM.remove(i);
	}

	void deleteSongFromLists(Song song) {
		// TODO Make this more efficient (log n).
		// Remove song from bookmarks
		DefaultListModel<Bookmark> lmSong = (DefaultListModel<Bookmark>)bookmarks.getModel(); // TODO Why not store a direct reference?
		for (int i = lmSong.getSize() - 1; i >= 0; i--) {
			if (song.equals(lmSong.getElementAt(i).song())) lmSong.removeElementAt(i);
		}
	}
}
