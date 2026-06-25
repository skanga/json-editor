package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.Main;
import com.skanga.jsoneditor.util.Colors;
import com.skanga.jsoneditor.util.MessageBundle;

/** Verifies tree node text color follows the light/dark look and feel rather than being pinned. */
public class JsonTreeCellRendererThemeTest {

	private Color renderedNodeForeground() {
		MessageBundle.loadResources();
		JsonTreeModel model = new JsonTreeModel(Map.of("/name", "\"Ada\""));
		JsonTree tree = new JsonTree();
		tree.setModel(model);
		JsonTreeNode node = model.getNodeByKey("/name");
		JsonTreeCellRenderer renderer = new JsonTreeCellRenderer();
		JPanel panel = (JPanel) renderer.getTreeCellRendererComponent(tree, node, false, false, true, 1, false);
		// nodePanel layout is [status, text, type]; the text label carries the node name color.
		return ((JLabel) panel.getComponent(1)).getForeground();
	}

	@Test
	public void nodeTextIsDarkOnLightThemeAndLightOnDarkTheme() {
		Main.setupLookAndFeel(false);
		Color light = renderedNodeForeground();
		assertEquals(UIManager.getColor("Tree.foreground"), light);
		assertTrue(Colors.isDark(light), "node text should be dark on the light theme");

		Main.setupLookAndFeel(true);
		Color dark = renderedNodeForeground();
		assertEquals(UIManager.getColor("Tree.foreground"), dark);
		assertFalse(Colors.isDark(dark), "node text should be light on the dark theme");
	}
}
