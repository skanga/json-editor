package com.skanga.jsoneditor.editor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.UIManager;

import com.skanga.jsoneditor.util.Colors;

/**
 * Compact tree icon for the JSON type represented by a node.
 */
public class JsonTypeIcon implements Icon {
	private static final int MIN_WIDTH = 44;
	private static final int MIN_HEIGHT = 20;
	private static final int H_PADDING = 8;
	private static final int V_PADDING = 4;

	private final JsonNodeType type;
	private final int childCount;
	private final Color foreground;
	
	public JsonTypeIcon(JsonNodeType type) {
		this(type, -1, null);
	}
	
	public JsonTypeIcon(JsonNodeType type, int childCount) {
		this(type, childCount, null);
	}
	
	public JsonTypeIcon(JsonNodeType type, Color foreground) {
		this(type, -1, foreground);
	}
	
	public JsonTypeIcon(JsonNodeType type, int childCount, Color foreground) {
		this.type = type == null ? JsonNodeType.Unknown : type;
		this.childCount = childCount;
		this.foreground = foreground;
	}
	
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setFont(glyphFont(c));

		Color color = foreground == null ? colorFor(type) : foreground;
		g2.setColor(color);
		g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		drawText(g2, glyphText(), x, y, getIconWidth(), getIconHeight());
		g2.dispose();
	}

	@Override
	public int getIconWidth() {
		FontMetrics metrics = glyphMetrics();
		return Math.max(MIN_WIDTH, metrics.stringWidth(glyphText()) + 2 * H_PADDING);
	}

	@Override
	public int getIconHeight() {
		FontMetrics metrics = glyphMetrics();
		return Math.max(MIN_HEIGHT, metrics.getHeight() + 2 * V_PADDING);
	}

	private Font glyphFont(Component c) {
		Font font = c == null || c.getFont() == null ? baseFont() : c.getFont();
		return font.deriveFont(Font.BOLD);
	}

	private static Font baseFont() {
		Font font = UIManager.getFont("Tree.font");
		if (font == null) {
			font = UIManager.getFont("Label.font");
		}
		return font == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 12) : font;
	}

	private FontMetrics glyphMetrics() {
		Font font = baseFont().deriveFont(Font.BOLD);
		java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
				1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		try {
			return g.getFontMetrics(font);
		} finally {
			g.dispose();
		}
	}

	private String glyphText() {
		switch (type) {
			case Boolean:
				return "T/F";
			case Null:
				return "∅";
			case Object:
				return childCount < 0 ? "{}" : collectionGlyph("{}", childCount);
			case Array:
				return childCount < 0 ? "[]" : collectionGlyph("[]", childCount);
			case Number:
				return "#";
			case String:
				return "Aa";
			default:
				return "?";
		}
	}

	private void drawText(Graphics2D g2, String text, int x, int y, int width, int height) {
		FontMetrics metrics = g2.getFontMetrics();
		int textX = x + (width - metrics.stringWidth(text)) / 2;
		int textY = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
		g2.drawString(text, textX, textY);
	}

	static String collectionGlyph(String delimiters, int childCount) {
		String countText = childCount > 99 ? "99+" : String.valueOf(childCount);
		if (delimiters.length() != 2) {
			return delimiters + countText;
		}
		return "" + delimiters.charAt(0) + countText + delimiters.charAt(1);
	}
	
	/**
	 * Shared, theme-aware source of truth for the per-type color palette used by
	 * both the tree type badge and the inspector header badge. On dark themes the
	 * base color is brightened so it stays legible.
	 *
	 * @param 	type the JSON node type
	 * @return	the color for the type, adapted to the current theme background
	 */
	public static Color colorFor(JsonNodeType type) {
		Color base = baseColor(type);
		Color bg = UIManager.getColor("Tree.background");
		if (bg == null) {
			bg = UIManager.getColor("Panel.background");
		}
		return Colors.forBackground(base, bg);
	}

	private static Color baseColor(JsonNodeType type) {
        return switch (type) {
            case Object -> new Color(57, 119, 180);
            case Array -> new Color(114, 94, 170);
            case String -> new Color(27, 132, 90);
            case Number -> new Color(188, 107, 0);
            case Boolean -> new Color(44, 126, 159);
            case Null -> new Color(112, 112, 112);
            default -> new Color(130, 130, 130);
        };
	}
}
