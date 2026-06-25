package com.skanga.jsoneditor.editor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import com.skanga.jsoneditor.util.Colors;
import com.skanga.jsoneditor.util.MessageBundle;

/**
 * Text field with a small inline clear affordance for tree filtering.
 */
class TreeFilterField extends JTextField {
	@Serial
    private static final long serialVersionUID = 1906722103756909669L;
	private static final int CLEAR_SIZE = 8;
	private static final int CLEAR_HIT_PADDING = 6;
	private static final Color ACTIVE_HIGHLIGHT_LIGHT = new Color(255, 255, 200);
	private static final Color ACTIVE_HIGHLIGHT_DARK = new Color(90, 80, 30);

	TreeFilterField() {
		setToolTipText(MessageBundle.get("tree.filter.tooltip"));
		addMouseListener(new ClearMouseListener());
		addMouseMotionListener(new ClearMouseListener());
		getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearFilter");
		getActionMap().put("clearFilter", new AbstractAction() {
			@Serial
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				setText("");
			}
		});
	}
	
	@Override
	public void setText(String text) {
		super.setText(text);
		boolean active = text != null && !text.isEmpty();
		Color background = UIManager.getColor("TextField.background");
		setBackground(active
			? (Colors.isDark(background) ? ACTIVE_HIGHLIGHT_DARK : ACTIVE_HIGHLIGHT_LIGHT)
			: background);
		repaint();
	}

	/** Muted foreground for the placeholder text and clear glyph, derived from the theme. */
	private static Color placeholderColor() {
		Color c = UIManager.getColor("TextField.inactiveForeground");
		if (c == null) {
			c = UIManager.getColor("Label.disabledForeground");
		}
		return c != null ? c : new Color(160, 160, 160);
	}

	boolean isClearButtonVisible() {
		return !getText().isEmpty();
	}
	
	Rectangle getClearButtonBounds() {
		int x = getWidth() - getInsets().right + (getInsets().right - CLEAR_SIZE) / 2;
		int y = (getHeight() - CLEAR_SIZE) / 2;
		return new Rectangle(x, y, CLEAR_SIZE, CLEAR_SIZE);
	}

	/** Clickable hit area, enlarged with padding around the drawn glyph for easier targeting. */
	Rectangle getClearButtonHitBounds() {
		Rectangle r = getClearButtonBounds();
		return new Rectangle(
				r.x - CLEAR_HIT_PADDING,
				r.y - CLEAR_HIT_PADDING,
				r.width + 2 * CLEAR_HIT_PADDING,
				r.height + 2 * CLEAR_HIT_PADDING);
	}

	boolean clearAt(int x, int y) {
		if (!isClearButtonVisible() || !getClearButtonHitBounds().contains(x, y)) {
			return false;
		}
		setText("");
		repaint();
		return true;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (getText().isEmpty()) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(placeholderColor());
			g2.setFont(getFont());
			FontMetrics fm = g2.getFontMetrics();
			Insets insets = getInsets();
			int x = insets.left + 2;
			int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
			g2.drawString(MessageBundle.get("tree.filter.placeholder"), x, y);
			g2.dispose();
			return;
		}
		// draw × clear button (existing code below)
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(placeholderColor());
		g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Rectangle r = getClearButtonBounds();
		g2.drawLine(r.x, r.y, r.x + r.width, r.y + r.height);
		g2.drawLine(r.x + r.width, r.y, r.x, r.y + r.height);
		g2.dispose();
	}
	
	private class ClearMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			clearAt(e.getX(), e.getY());
		}
		
		@Override
		public void mouseMoved(MouseEvent e) {
			setCursor(isClearButtonVisible() && getClearButtonHitBounds().contains(e.getPoint())
					? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
					: Cursor.getDefaultCursor());
		}
		
		@Override
		public void mouseExited(MouseEvent e) {
			setCursor(Cursor.getDefaultCursor());
		}
	}
}
