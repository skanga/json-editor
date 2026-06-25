package com.skanga.jsoneditor.editor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.io.Serial;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatClientProperties;
import com.skanga.jsoneditor.util.Colors;
import com.skanga.jsoneditor.util.MessageBundle;

class EditorToolBar extends JToolBar {
	@Serial
    private static final long serialVersionUID = -514469725911640894L;
	private static final Dimension BUTTON_SIZE = new Dimension(34, 34);
	private static final int RIGHT_PADDING = 8;
	
	private final Editor editor;
	private final JsonTree tree;
	private JButton saveButton;
	private JButton closeButton;
	private JButton expandAllButton;
	private JButton collapseAllButton;
	private JButton findButton;
	private JButton addKeyButton;
	private JButton undoButton;
	private JButton redoButton;
	private JButton themeButton;

	EditorToolBar(Editor editor, JsonTree tree) {
		super();
		this.editor = editor;
		this.tree = tree;
		setupUI();
		setProjectOpen(false);
		setSavable(false);
	}

	void setProjectOpen(boolean open) {
		closeButton.setEnabled(open);
		expandAllButton.setEnabled(open);
		collapseAllButton.setEnabled(open);
		findButton.setEnabled(open);
		addKeyButton.setEnabled(open);
	}

	void setSavable(boolean savable) {
		saveButton.setEnabled(savable);
	}

	void setUndoRedoEnabled(boolean canUndo, boolean canRedo) {
		undoButton.setEnabled(canUndo);
		redoButton.setEnabled(canRedo);
	}
	
	private void setupUI() {
		setFloatable(false);
		setRollover(true);
		setOpaque(true);
		applyBottomBorder();
		
		int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

		add(button(ToolIcon.Kind.NEW, EditorCommand.NEW,
				KeyStroke.getKeyStroke(KeyEvent.VK_N, keyMask),
				e -> editor.showCreateJsonFileDialog()));
		add(button(ToolIcon.Kind.OPEN, EditorCommand.OPEN,
				KeyStroke.getKeyStroke(KeyEvent.VK_O, keyMask),
				e -> editor.showImportProjectDialog()));
		saveButton = button(ToolIcon.Kind.SAVE, EditorCommand.SAVE,
				KeyStroke.getKeyStroke(KeyEvent.VK_S, keyMask),
				e -> editor.saveProject());
		add(saveButton);
		closeButton = button(ToolIcon.Kind.CLOSE, EditorCommand.CLOSE,
				KeyStroke.getKeyStroke(KeyEvent.VK_W, keyMask),
				e -> editor.closeFile());
		add(closeButton);
		addSeparator();
		undoButton = button(ToolIcon.Kind.UNDO, EditorCommand.UNDO,
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, keyMask),
				e -> { if (editor != null) editor.undo(e); });
		add(undoButton);
		redoButton = button(ToolIcon.Kind.REDO, EditorCommand.REDO,
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, keyMask | InputEvent.SHIFT_DOWN_MASK),
				e -> { if (editor != null) editor.redo(e); });
		add(redoButton);
		addSeparator();
		expandAllButton = button(ToolIcon.Kind.EXPAND, EditorCommand.EXPAND, null,
				e -> tree.expandAll());
		add(expandAllButton);
		collapseAllButton = button(ToolIcon.Kind.COLLAPSE, EditorCommand.COLLAPSE, null,
				e -> tree.collapseAll());
		add(collapseAllButton);
		addSeparator();
		findButton = button(ToolIcon.Kind.FIND, EditorCommand.FIND_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_F, keyMask),
				e -> editor.showFindDialog());
		add(findButton);
		addKeyButton = button(ToolIcon.Kind.ADD, EditorCommand.ADD_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_T, keyMask),
				e -> editor.showAddDialog(tree.getSelectionNode()));
		add(addKeyButton);

		// Push the theme toggle to the right edge of the toolbar.
		add(Box.createHorizontalGlue());
		themeButton = new JButton();
		themeButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
		themeButton.putClientProperty("toolbarAction", "THEME");
		themeButton.setFocusable(false);
		themeButton.setPreferredSize(BUTTON_SIZE);
		themeButton.setMinimumSize(BUTTON_SIZE);
		themeButton.setMaximumSize(BUTTON_SIZE);
		themeButton.addActionListener(e -> {
			if (editor != null) {
				editor.applyTheme(!editor.getSettings().isDarkTheme());
				refreshThemeButton();
			}
		});
		add(themeButton);
		refreshThemeButton();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		// Re-derive the separator from the (possibly new) theme so it isn't frozen at the
		// original look and feel after a runtime light/dark switch.
		applyBottomBorder();
		// Keep the toggle's sun/moon icon in sync however the theme was changed
		// (toolbar button or the Settings checkbox). themeButton is null during construction.
		if (themeButton != null) {
			refreshThemeButton();
		}
	}

	private void applyBottomBorder() {
		Color base = UIManager.getColor("Panel.background");
		if (base != null) {
			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.scale(base, .84f)),
					BorderFactory.createEmptyBorder(0, 0, 0, RIGHT_PADDING)));
		}
	}

	/** Updates the theme toggle to show the icon/tooltip for the theme it will switch to. */
	void refreshThemeButton() {
		boolean dark = editor != null && editor.getSettings().isDarkTheme();
		themeButton.setIcon(new ToolIcon(dark ? ToolIcon.Kind.THEME_LIGHT : ToolIcon.Kind.THEME_DARK));
		themeButton.setToolTipText(MessageBundle.get(dark ? "toolbar.theme.light" : "toolbar.theme.dark"));
	}

	private JButton button(ToolIcon.Kind icon, EditorCommand command, KeyStroke accelerator,
			ActionListener action) {
		JButton button = new JButton(new ToolIcon(icon));
		button.setToolTipText(tooltipText(command, accelerator));
		button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
		button.putClientProperty("toolbarAction", icon.name());
		command.applyTo(button);
		button.setFocusable(false);
		button.setPreferredSize(BUTTON_SIZE);
		button.setMinimumSize(BUTTON_SIZE);
		button.setMaximumSize(BUTTON_SIZE);
		button.addActionListener(action);
		return button;
	}

	private static String tooltipText(EditorCommand command, KeyStroke accelerator) {
		String text = command.text();
		String shortcut = shortcutText(accelerator);
		return shortcut.isEmpty() ? text : text + " (" + shortcut + ")";
	}

	private static String shortcutText(KeyStroke accelerator) {
		if (accelerator == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		String modifiers = InputEvent.getModifiersExText(accelerator.getModifiers());
		if (!modifiers.isEmpty()) {
			sb.append(modifiers).append('+');
		}
		sb.append(KeyEvent.getKeyText(accelerator.getKeyCode()));
		return sb.toString();
	}

	private record ToolIcon(Kind kind) implements Icon {
			private static final int BASE_SIZE = 18;
			private static final int SIZE = EditorIconStyle.ICON_SIZE;
			private static final double SCALE = (double) SIZE / BASE_SIZE;
			private static final float STROKE_WIDTH = EditorIconStyle.STROKE_WIDTH;

		enum Kind {
				NEW, OPEN, SAVE, CLOSE, UNDO, REDO, EXPAND, COLLAPSE, FIND, ADD, THEME_LIGHT, THEME_DARK
			}

		@Override
			public int getIconWidth() {
				return SIZE;
			}

		@Override
			public int getIconHeight() {
				return SIZE;
			}

		@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.translate(x, y);
				g2.scale(SCALE, SCALE);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setStroke(stroke());
				g2.setColor(c.isEnabled() ? UIManager.getColor("Button.foreground") : UIManager.getColor("Label.disabledForeground"));
				switch (kind) {
					case NEW -> paintNew(g2);
					case OPEN -> paintOpen(g2);
					case SAVE -> paintSave(g2);
					case CLOSE -> paintClose(g2);
					case UNDO -> paintUndo(g2);
					case REDO -> paintRedo(g2);
					case EXPAND -> paintExpand(g2);
					case COLLAPSE -> paintCollapse(g2);
					case FIND -> paintFind(g2);
					case ADD -> paintAdd(g2);
					case THEME_LIGHT -> paintSun(g2);
					case THEME_DARK -> paintMoon(g2);
				}
				g2.dispose();
			}

		private void paintNew(Graphics2D g) {
				g.drawRoundRect(4, 2, 10, 14, 2, 2);
				g.drawLine(7, 9, 11, 9);
				g.drawLine(9, 7, 9, 11);
			}

		private void paintOpen(Graphics2D g) {
				g.drawLine(2, 6, 6, 6);
				g.drawLine(6, 6, 8, 8);
				g.drawLine(8, 8, 16, 8);
				g.drawLine(3, 6, 3, 15);
				g.drawLine(3, 15, 15, 15);
				g.drawLine(15, 15, 16, 8);
			}

		private void paintSave(Graphics2D g) {
				g.drawRoundRect(3, 3, 12, 12, 2, 2);
				g.drawLine(6, 3, 6, 7);
				g.drawLine(6, 7, 13, 7);
				g.drawRect(6, 11, 6, 4);
			}

		private void paintClose(Graphics2D g) {
				g.drawRoundRect(3, 3, 12, 12, 2, 2);
				g.drawLine(6, 6, 12, 12);
				g.drawLine(12, 6, 6, 12);
			}

		private void paintUndo(Graphics2D g) {
				Path2D arrow = new Path2D.Double();
				arrow.moveTo(4, 8);
				arrow.lineTo(11, 8);
				arrow.curveTo(13, 8, 15, 10, 15, 12);
				arrow.curveTo(15, 13, 14, 14, 13, 15);
				g.draw(arrow);
				g.drawLine(6, 5, 4, 8);
				g.drawLine(4, 8, 6, 11);
			}

		private void paintRedo(Graphics2D g) {
				Path2D arrow = new Path2D.Double();
				arrow.moveTo(14, 8);
				arrow.lineTo(7, 8);
				arrow.curveTo(5, 8, 3, 10, 3, 12);
				arrow.curveTo(3, 13, 4, 14, 5, 15);
				g.draw(arrow);
				g.drawLine(12, 5, 14, 8);
				g.drawLine(14, 8, 12, 11);
			}

		private void paintFind(Graphics2D g) {
				g.drawOval(3, 3, 8, 8);
				g.drawLine(10, 10, 15, 15);
			}

		private void paintAdd(Graphics2D g) {
				g.drawLine(9, 4, 9, 14);
				g.drawLine(4, 9, 14, 9);
			}

		private void paintSun(Graphics2D g) {
				g.drawOval(6, 6, 6, 6);
				g.drawLine(9, 1, 9, 3);
				g.drawLine(9, 15, 9, 17);
				g.drawLine(1, 9, 3, 9);
				g.drawLine(15, 9, 17, 9);
				g.drawLine(3, 3, 4, 4);
				g.drawLine(14, 14, 15, 15);
				g.drawLine(15, 3, 14, 4);
				g.drawLine(4, 14, 3, 15);
			}

		private void paintMoon(Graphics2D g) {
				Area moon = new Area(new Ellipse2D.Double(3, 3, 12, 12));
				moon.subtract(new Area(new Ellipse2D.Double(6, 3, 12, 12)));
				Shape tiltedMoon = AffineTransform.getRotateInstance(Math.toRadians(-40), 9, 9)
						.createTransformedShape(moon);
				g.draw(tiltedMoon);
			}

		private void paintExpand(Graphics2D g) {
				paintTreeWithChevrons(g, true);
			}

		private void paintCollapse(Graphics2D g) {
				paintTreeWithChevrons(g, false);
			}

		// Tree bars on the left; a bold double chevron on the right makes the two icons
		// read at a glance: down = expand (reveal), up = collapse (hide).
		private void paintTreeWithChevrons(Graphics2D g, boolean expand) {
				g.setStroke(stroke());
				g.drawLine(3, 3, 3, 15);
				g.drawLine(3, 5, 8, 5);
				g.drawLine(3, 9, 8, 9);
				g.drawLine(3, 13, 8, 13);

				if (expand) {
					paintDownChevron(g, 6);
					paintDownChevron(g, 10);
				} else {
					paintUpChevron(g, 9);
					paintUpChevron(g, 13);
				}
			}

		private void paintDownChevron(Graphics2D g, int yTop) {
				g.drawLine(11, yTop, 14, yTop + 3);
				g.drawLine(14, yTop + 3, 17, yTop);
			}

		private void paintUpChevron(Graphics2D g, int yBottom) {
				g.drawLine(11, yBottom, 14, yBottom - 3);
				g.drawLine(14, yBottom - 3, 17, yBottom);
			}

		private BasicStroke stroke() {
				return EditorIconStyle.stroke((float) (STROKE_WIDTH / SCALE));
			}
		}
}
