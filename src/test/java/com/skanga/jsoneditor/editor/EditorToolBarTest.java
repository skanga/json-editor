package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.Main;
import com.skanga.jsoneditor.util.MessageBundle;

public class EditorToolBarTest {
	@Test
	public void toolbarExposesFirstPassEditorCommands() {
		MessageBundle.loadResources();
		
		EditorToolBar toolbar = new EditorToolBar(null, new JsonTree());
		
		assertEquals("NEW", button(toolbar, 0).getClientProperty("toolbarAction"));
		assertEquals("NEW", button(toolbar, 0).getClientProperty("editorCommand"));
		assertEquals("OPEN", button(toolbar, 1).getClientProperty("toolbarAction"));
		assertEquals("OPEN", button(toolbar, 1).getClientProperty("editorCommand"));
		assertEquals("SAVE", button(toolbar, 2).getClientProperty("toolbarAction"));
		assertEquals("SAVE", button(toolbar, 2).getClientProperty("editorCommand"));
		assertEquals("CLOSE", button(toolbar, 3).getClientProperty("toolbarAction"));
		assertEquals("CLOSE", button(toolbar, 3).getClientProperty("editorCommand"));
		assertTrue(toolbar.getComponent(4) instanceof JToolBar.Separator);
		assertEquals("UNDO", button(toolbar, 5).getClientProperty("toolbarAction"));
		assertEquals("UNDO", button(toolbar, 5).getClientProperty("editorCommand"));
		assertEquals("REDO", button(toolbar, 6).getClientProperty("toolbarAction"));
		assertEquals("REDO", button(toolbar, 6).getClientProperty("editorCommand"));
		assertTrue(toolbar.getComponent(7) instanceof JToolBar.Separator);
		assertEquals("EXPAND", button(toolbar, 8).getClientProperty("toolbarAction"));
		assertEquals("EXPAND", button(toolbar, 8).getClientProperty("editorCommand"));
		assertEquals("COLLAPSE", button(toolbar, 9).getClientProperty("toolbarAction"));
		assertEquals("COLLAPSE", button(toolbar, 9).getClientProperty("editorCommand"));
		assertFalse(toolbar.isFloatable());
	}
	
	@Test
	public void toolbarUsesIconOnlyButtonsWithTooltips() {
		MessageBundle.loadResources();
		
		EditorToolBar toolbar = new EditorToolBar(null, new JsonTree());
		
		for (Component component : toolbar.getComponents()) {
			if (component instanceof JButton button) {
				assertEquals("", button.getText());
				assertNotNull(button.getIcon());
				assertNotNull(button.getToolTipText());
				assertEquals(34, button.getPreferredSize().width);
				assertEquals(34, button.getPreferredSize().height);
				assertEquals(24, button.getIcon().getIconWidth());
				assertEquals(24, button.getIcon().getIconHeight());
			}
		}
	}
	
	@Test
	public void themeToggleIsRightAlignedAtToolbarEnd() {
		MessageBundle.loadResources();
		Main.setupLookAndFeel(false);

		EditorToolBar toolbar = new EditorToolBar(null, new JsonTree());

		int last = toolbar.getComponentCount() - 1;
		JButton theme = (JButton) toolbar.getComponent(last);
		assertEquals("THEME", theme.getClientProperty("toolbarAction"));
		assertNotNull(theme.getIcon());
		// A horizontal glue filler precedes the toggle to push it to the right edge.
		assertTrue(toolbar.getComponent(last - 1) instanceof Box.Filler);

		toolbar.setSize(800, 40);
		toolbar.doLayout();
		assertTrue(theme.getX() > 600, "theme toggle should be right-aligned, was at x=" + theme.getX());
		assertTrue(toolbar.getInsets().right >= 8, "theme toggle needs right-side toolbar padding");
		assertTrue(toolbar.getWidth() - (theme.getX() + theme.getWidth()) >= 8,
				"theme toggle should not hug the window edge");
	}

	@Test
	public void toolbarDisablesProjectScopedCommandsUntilProjectIsOpen() {
		MessageBundle.loadResources();
		
		EditorToolBar toolbar = new EditorToolBar(null, new JsonTree());
		
		assertTrue(button(toolbar, 0).isEnabled());
		assertTrue(button(toolbar, 1).isEnabled());
		assertFalse(button(toolbar, 2).isEnabled());
		assertFalse(button(toolbar, 3).isEnabled());
		assertTrue(button(toolbar, 5).isEnabled());
		assertTrue(button(toolbar, 6).isEnabled());
		assertFalse(button(toolbar, 8).isEnabled());
		assertFalse(button(toolbar, 9).isEnabled());
		
		toolbar.setProjectOpen(true);
		toolbar.setSavable(true);
		
		assertTrue(button(toolbar, 2).isEnabled());
		assertTrue(button(toolbar, 3).isEnabled());
		assertTrue(button(toolbar, 8).isEnabled());
		assertTrue(button(toolbar, 9).isEnabled());
	}

	@Test
	public void expandAndCollapseToolbarIconsUseDistinctChevrons() {
		MessageBundle.loadResources();

		EditorToolBar toolbar = new EditorToolBar(null, new JsonTree());
		toolbar.setProjectOpen(true);

		BufferedImage expand = paintIcon(button(toolbar, 8).getIcon());
		BufferedImage collapse = paintIcon(button(toolbar, 9).getIcon());

		// The chevron apex sits on the x=14 column. Expand points its chevrons down, so
		// that column is painted lower than higher; collapse points them up, the mirror.
		// This asymmetry is what makes the two icons distinguishable at a glance.
		int apexColumn = scaledIconPoint(14);
		int expandTop = countPaintedInColumn(expand, apexColumn, scaledIconPoint(2), scaledIconPoint(7));
		int expandBottom = countPaintedInColumn(expand, apexColumn, scaledIconPoint(11), scaledIconPoint(16));
		int collapseTop = countPaintedInColumn(collapse, apexColumn, scaledIconPoint(2), scaledIconPoint(7));
		int collapseBottom = countPaintedInColumn(collapse, apexColumn, scaledIconPoint(11), scaledIconPoint(16));

		assertTrue(expandBottom > expandTop, "expand chevrons should point down");
		assertTrue(collapseTop > collapseBottom, "collapse chevrons should point up");
	}

	@Test
	public void undoAndRedoToolbarIconsUseReturnArrowStems() {
		MessageBundle.loadResources();

		EditorToolBar toolbar = new EditorToolBar(null, new JsonTree());

		BufferedImage undo = paintIcon(button(toolbar, 5).getIcon());
		BufferedImage redo = paintIcon(button(toolbar, 6).getIcon());

		assertTrue(countPaintedInRow(undo, scaledIconPoint(8), scaledIconPoint(4), scaledIconPoint(11)) >= 7);
		assertTrue(countPaintedInRow(redo, scaledIconPoint(8), scaledIconPoint(7), scaledIconPoint(14)) >= 7);
		assertFalse(isPainted(undo, scaledIconPoint(8), scaledIconPoint(10)));
		assertFalse(isPainted(redo, scaledIconPoint(10), scaledIconPoint(10)));
	}

	@Test
	public void themeDarkToolbarIconUsesRoundCrescentAtChosenAngle() {
		MessageBundle.loadResources();
		Main.setupLookAndFeel(false);

		EditorToolBar toolbar = new EditorToolBar(null, new JsonTree());
		JButton theme = (JButton) toolbar.getComponent(toolbar.getComponentCount() - 1);

		BufferedImage moon = paintIcon(theme.getIcon());
		Rectangle bounds = paintedBounds(moon);
		double angle = paintedAxisAngleDegrees(moon);

		assertTrue(Math.abs(bounds.width - bounds.height) <= 3,
				"round crescent should fit a nearly square moon, bounds=" + bounds);
		assertTrue(angle >= 35 && angle <= 60, "moon should be rotated near -40 degrees, painted axis=" + angle);
	}
	
	private static JButton button(EditorToolBar toolbar, int index) {
		return (JButton) toolbar.getComponent(index);
	}

	private static BufferedImage paintIcon(Icon icon) {
		BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		icon.paintIcon(new JLabel(), graphics, 0, 0);
		graphics.dispose();
		return image;
	}

	private static int scaledIconPoint(int point) {
		return Math.round(point * 24f / 18f);
	}

	private static int countPaintedInColumn(BufferedImage image, int x, int fromY, int toY) {
		int painted = 0;
		for (int y = fromY; y <= toY; y++) {
			if (isPainted(image, x, y)) {
				painted++;
			}
		}
		return painted;
	}

	private static int countPaintedInRow(BufferedImage image, int y, int fromX, int toX) {
		int painted = 0;
		for (int x = fromX; x <= toX; x++) {
			if (isPainted(image, x, y)) {
				painted++;
			}
		}
		return painted;
	}

	private static boolean isPainted(BufferedImage image, int x, int y) {
		return (image.getRGB(x, y) >>> 24) > 0;
	}

	private static Rectangle paintedBounds(BufferedImage image) {
		int minX = image.getWidth();
		int minY = image.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (isPainted(image, x, y)) {
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}
		return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}

	private static double paintedAxisAngleDegrees(BufferedImage image) {
		double count = 0;
		double sumX = 0;
		double sumY = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (isPainted(image, x, y)) {
					count++;
					sumX += x;
					sumY += y;
				}
			}
		}
		double meanX = sumX / count;
		double meanY = sumY / count;
		double xx = 0;
		double yy = 0;
		double xy = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (isPainted(image, x, y)) {
					double dx = x - meanX;
					double dy = y - meanY;
					xx += dx * dx;
					yy += dy * dy;
					xy += dx * dy;
				}
			}
		}
		double angle = Math.toDegrees(0.5 * Math.atan2(2 * xy, xx - yy));
		return angle < 0 ? angle + 180 : angle;
	}
}
