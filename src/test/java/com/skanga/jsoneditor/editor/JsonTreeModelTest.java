package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.MessageBundle;

public class JsonTreeModelTest {
	@Test
	public void jsonPathKeysPreserveJsonNodeIdentityInTree() {
		MessageBundle.loadResources();

		JsonTreeModel model = new JsonTreeModel(List.of(
				"",
				"/a.b",
				"/a.b/items",
				"/a.b/items/0",
				"/a.b/items/1",
				"/slash~1key"));

		JsonTreeNode dottedKey = model.getNodeByKey("/a.b");
		JsonTreeNode firstArrayItem = model.getNodeByKey("/a.b/items/0");
		JsonTreeNode slashKey = model.getNodeByKey("/slash~1key");

		assertNotNull(dottedKey);
		assertEquals("a.b", dottedKey.toString());
		assertNotNull(firstArrayItem);
		assertEquals("0", firstArrayItem.toString());
		assertNotNull(slashKey);
		assertEquals("slash/key", slashKey.toString());
	}

	@Test
	public void findMatchesPathNodeNameAndValue() {
		MessageBundle.loadResources();

		JsonTreeModel model = new JsonTreeModel(List.of(
				"",
				"/users",
				"/users/0",
				"/users/0/email",
				"/settings"));

		assertEquals("/users/0/email", model.findNode("email", key -> null).getKey());
		assertEquals("/users/0/email", model.findNode("/users/0/email", key -> null).getKey());
		assertEquals("/users/0/email", model.findNode("person@example.com", key -> {
			return "/users/0/email".equals(key) ? "\"person@example.com\"" : null;
		}).getKey());
		assertNull(model.findNode("missing", key -> null));
	}

	@Test
	public void jsonTreePreservesSourceOrderAndAllNodes() {
		MessageBundle.loadResources();

		JsonTreeModel model = new JsonTreeModel(List.of(
				"",
				"/z",
				"/a",
				"/nested",
				"/nested/b",
				"/nested/a",
				"/arr",
				"/arr/0",
				"/arr/0/y",
				"/arr/0/x"));

		JsonTreeNode root = (JsonTreeNode) model.getRoot();
		JsonTreeNode nested = model.getNodeByKey("/nested");
		JsonTreeNode arrayItem = model.getNodeByKey("/arr/0");

		assertEquals(10, model.getNodeCount());
		assertEquals(List.of("z", "a", "nested", "arr"),
				root.getChildren().stream().map(JsonTreeNode::getName).toList());
		assertEquals(List.of("b", "a"),
				nested.getChildren().stream().map(JsonTreeNode::getName).toList());
		assertEquals(List.of("y", "x"),
				arrayItem.getChildren().stream().map(JsonTreeNode::getName).toList());
	}

	@Test
	public void jsonNodeTypesAreDerivedFromNodeValues() {
		MessageBundle.loadResources();

		JsonTreeModel model = new JsonTreeModel(Map.of(
				"", "object",
				"/name", "\"Ada\"",
				"/count", "3",
				"/enabled", "true",
				"/empty", "null",
				"/items", "array"));

		assertSame(JsonNodeType.Object, model.getNodeByKey("").getJsonType());
		assertSame(JsonNodeType.String, model.getNodeByKey("/name").getJsonType());
		assertSame(JsonNodeType.Number, model.getNodeByKey("/count").getJsonType());
		assertSame(JsonNodeType.Boolean, model.getNodeByKey("/enabled").getJsonType());
		assertSame(JsonNodeType.Null, model.getNodeByKey("/empty").getJsonType());
		assertSame(JsonNodeType.Array, model.getNodeByKey("/items").getJsonType());
	}

	@Test
	public void jsonNodeTypesAreDerivedFromJsonLiterals() {
		assertSame(JsonNodeType.Object, JsonNodeType.fromJsonValue("{\"name\":\"Ada\"}"));
		assertSame(JsonNodeType.Array, JsonNodeType.fromJsonValue("[1,2,3]"));
		assertSame(JsonNodeType.Number, JsonNodeType.fromJsonValue("-12.5e2"));
		assertSame(JsonNodeType.Boolean, JsonNodeType.fromJsonValue("false"));
		assertSame(JsonNodeType.String, JsonNodeType.fromJsonValue(""));
		assertSame(JsonNodeType.Unknown, JsonNodeType.fromJsonValue("not-json"));
	}

	@Test
	public void rendererPlacesJsonTypeIconAfterNodeName() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setFont(tree.getFont().deriveFont(17f));
		tree.setModel(new JsonTreeModel(Map.of("/name", "\"Ada\"")));
		JsonTreeNode node = ((JsonTreeModel) tree.getModel()).getNodeByKey("/name");

		Component component = tree.getCellRenderer().getTreeCellRendererComponent(
				tree, node, false, false, true, 1, false);

		assertTrue(component instanceof JPanel);
		JPanel panel = (JPanel) component;
		assertEquals("name", ((JLabel) panel.getComponent(1)).getText());
		assertNotNull(((JLabel) panel.getComponent(2)).getIcon());
		assertEquals(((JLabel) panel.getComponent(1)).getFont(), ((JLabel) panel.getComponent(2)).getFont());
		EmptyBorder iconGap = (EmptyBorder) ((JLabel) panel.getComponent(2)).getBorder();
		int leftGap = iconGap.getBorderInsets(panel.getComponent(2)).left;
		assertTrue(leftGap >= 1);
		assertTrue(leftGap <= 3);
	}

	@Test
	public void rendererShowsFolderIconOnRootNode() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		JsonTreeNode root = (JsonTreeNode) tree.getModel().getRoot();

		Component component = tree.getCellRenderer().getTreeCellRendererComponent(
				tree, root, false, true, false, 0, false);

		JPanel panel = (JPanel) component;
		assertEquals(3, panel.getComponentCount());
		JLabel status = (JLabel) panel.getComponent(0);
		assertNotNull(status.getIcon());
		assertTrue(status.getPreferredSize().width >= status.getIcon().getIconWidth() + 8);
		JLabel text = (JLabel) panel.getComponent(1);
		assertEquals("JSON", text.getText());
		assertTrue(text.getBorder() instanceof EmptyBorder);
		EmptyBorder rootNameGap = (EmptyBorder) text.getBorder();
		assertEquals(4, rootNameGap.getBorderInsets(text).left);
	}

	@Test
	public void rendererShowsCollectionGlyphOnRootObjectNode() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"", "object",
				"/name", "\"Ada\"",
				"/count", "2")));
		JsonTreeNode root = (JsonTreeNode) tree.getModel().getRoot();

		Component component = tree.getCellRenderer().getTreeCellRendererComponent(
				tree, root, false, true, false, 0, false);

		JPanel panel = (JPanel) component;
		JLabel type = (JLabel) panel.getComponent(2);
		assertNotNull(type.getIcon());
		assertEquals("Object (2 properties)", type.getToolTipText());
		assertEquals("Object (2 properties)", panel.getToolTipText());
		assertTrue(iconsPaintDiffer((JsonTypeIcon) type.getIcon(), new JsonTypeIcon(JsonNodeType.Object)));
	}
	
	@Test
	public void jsonTreeUsesFlatLafTreeDefaultsForBaseStyling() {
		Object previousRowHeight = UIManager.get("Tree.rowHeight");
		Object previousBackground = UIManager.get("Tree.background");
		Object previousForeground = UIManager.get("Tree.foreground");
		try {
			Color background = new Color(246, 248, 250);
			Color foreground = new Color(31, 35, 40);
			UIManager.put("Tree.rowHeight", 31);
			UIManager.put("Tree.background", background);
			UIManager.put("Tree.foreground", foreground);
			
			JsonTree tree = new JsonTree();
			
			assertEquals(31, tree.getRowHeight());
			assertEquals(background, tree.getBackground());
			assertEquals(foreground, tree.getForeground());
			assertTrue(tree.isOpaque());
			assertEquals("None", tree.getClientProperty("JTree.lineStyle"));
			EmptyBorder border = (EmptyBorder) tree.getBorder();
			Insets insets = border.getBorderInsets(tree);
			assertEquals(8, insets.top);
			assertEquals(14, insets.left);
			assertEquals(8, insets.bottom);
			assertEquals(14, insets.right);
			BasicTreeUI treeUI = (BasicTreeUI) tree.getUI();
			assertEquals(10, treeUI.getLeftChildIndent());
			assertEquals(12, treeUI.getRightChildIndent());
		} finally {
			UIManager.put("Tree.rowHeight", previousRowHeight);
			UIManager.put("Tree.background", previousBackground);
			UIManager.put("Tree.foreground", previousForeground);
		}
	}
	
	@Test
	public void rendererUsesFlatLafSelectionColors() {
		MessageBundle.loadResources();
		Object previousBackground = UIManager.get("Tree.background");
		Object previousSelectionBackground = UIManager.get("Tree.selectionBackground");
		Object previousSelectionForeground = UIManager.get("Tree.selectionForeground");
		try {
			Color background = new Color(250, 250, 250);
			Color selectionBackground = new Color(216, 235, 255);
			Color selectionForeground = new Color(12, 24, 36);
			UIManager.put("Tree.background", background);
			UIManager.put("Tree.selectionBackground", selectionBackground);
			UIManager.put("Tree.selectionForeground", selectionForeground);
			
			JsonTree tree = new JsonTree();
			tree.setModel(new JsonTreeModel(Map.of("/name", "\"Ada\"")));
			JsonTreeNode node = ((JsonTreeModel) tree.getModel()).getNodeByKey("/name");
			
			Component component = tree.getCellRenderer().getTreeCellRendererComponent(
					tree, node, true, false, true, 1, false);
			JPanel panel = (JPanel) component;
			
			assertEquals(selectionBackground, panel.getBackground());
			assertEquals(selectionForeground, ((JLabel) panel.getComponent(1)).getForeground());
			assertEquals(selectionForeground, ((JLabel) panel.getComponent(2)).getForeground());
			assertTrue(iconPaintsColor(((JLabel) panel.getComponent(2)).getIcon(),
					(JLabel) panel.getComponent(2), selectionForeground));
		} finally {
			UIManager.put("Tree.background", previousBackground);
			UIManager.put("Tree.selectionBackground", previousSelectionBackground);
			UIManager.put("Tree.selectionForeground", previousSelectionForeground);
		}
	}

	@Test
	public void treePaintsSubtleAlternatingRowStripes() {
		MessageBundle.loadResources();

		Object previousBackground = UIManager.get("Tree.background");
		Object previousForeground = UIManager.get("Tree.foreground");
		try {
			Color background = new Color(250, 250, 250);
			UIManager.put("Tree.background", background);
			UIManager.put("Tree.foreground", new Color(40, 40, 40));

			JsonTree tree = new JsonTree();
			tree.setModel(new JsonTreeModel(Map.of(
					"/name", "\"Ada\"",
					"/count", "3",
					"/enabled", "true")));
			tree.setSize(260, 160);
			tree.doLayout();
			tree.expandAll();
			tree.clearSelection();

			BufferedImage image = new BufferedImage(260, 160, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = image.createGraphics();
			tree.paint(graphics);
			graphics.dispose();

			Rectangle evenRow = tree.getRowBounds(0);
			Rectangle oddRow = tree.getRowBounds(1);
			Color evenColor = new Color(image.getRGB(250, evenRow.y + evenRow.height / 2));
			Color oddColor = new Color(image.getRGB(250, oddRow.y + oddRow.height / 2));

			assertEquals(background, evenColor);
			assertFalse(background.equals(oddColor));
			assertTrue(colorDistance(background, oddColor) >= 8);
			assertTrue(colorDistance(background, oddColor) <= 14);
		} finally {
			UIManager.put("Tree.background", previousBackground);
			UIManager.put("Tree.foreground", previousForeground);
		}
	}
	
	@Test
	public void treeToggleIconUsesDisclosureGlyphWithoutBoxBackground() {
		Icon icon = new JsonTreeToggleIcon(JsonTreeToggleIcon.ToggleIconType.Collapsed);
		BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		
		icon.paintIcon(new JLabel(), graphics, 4, 4);
		graphics.dispose();
		
		assertEquals(16, icon.getIconWidth());
		assertEquals(16, icon.getIconHeight());
		assertEquals(0, image.getRGB(4, 4) >>> 24);
		assertTrue(countPaintedPixels(image) > 0);
	}

	@Test
	public void rootControlsExpandAndCollapseTree() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"",
				"/settings", "object",
				"/settings/theme", "\"dark\"")));
		tree.setSize(300, 200);
		tree.doLayout();
		tree.expandAll();
		JsonTreeNode users = ((JsonTreeModel) tree.getModel()).getNodeByKey("/users");
		TreePath usersPath = new TreePath(users.getPath());
		tree.collapsePath(usersPath);
		assertTrue(tree.isCollapsed(usersPath));

		tree.expandAll();

		assertTrue(tree.isExpanded(usersPath));

		tree.collapseAll();

		assertTrue(tree.isCollapsed(usersPath));
	}
	
	@Test
	public void rootRowDoesNotHaveInvisibleExpandCollapseClickTargets() {
		MessageBundle.loadResources();
		
		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"")));
		tree.setSize(320, 160);
		tree.doLayout();
		tree.expandAll();
		JsonTreeNode users = ((JsonTreeModel) tree.getModel()).getNodeByKey("/users");
		TreePath usersPath = new TreePath(users.getPath());
		tree.collapsePath(usersPath);
		assertTrue(tree.isCollapsed(usersPath));
		
		Rectangle rootBounds = tree.getRowBounds(0);
		int rootActionX = 24 + tree.getFontMetrics(tree.getFont()).stringWidth("JSON") + 10;
		MouseEvent pressed = new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
				0, rootActionX, rootBounds.y + rootBounds.height / 2, 1, false, MouseEvent.BUTTON1);
		MouseEvent released = new MouseEvent(tree, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
				0, rootActionX, rootBounds.y + rootBounds.height / 2, 1, false, MouseEvent.BUTTON1);
		for (MouseListener listener : tree.getMouseListeners()) {
			listener.mousePressed(pressed);
			listener.mouseReleased(released);
		}
		
		assertTrue(tree.isCollapsed(usersPath));
	}

	@Test
	public void rendererExposesJsonTypeTooltips() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of("/name", "\"Ada\"")));
		JsonTreeNode node = ((JsonTreeModel) tree.getModel()).getNodeByKey("/name");

		Component component = tree.getCellRenderer().getTreeCellRendererComponent(
				tree, node, false, false, true, 1, false);

		JPanel panel = (JPanel) component;
		assertEquals("String", ((JComponent) component).getToolTipText());
		assertEquals("String", ((JLabel) panel.getComponent(2)).getToolTipText());
	}

	@Test
	public void arrayAndObjectTooltipsIncludeChildCounts() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/items", "array",
				"/items/0", "\"first\"",
				"/items/1", "\"second\"",
				"/settings", "object",
				"/settings/name", "\"Ada\"",
				"/settings/count", "2")));

		assertEquals("Array (2 items)", getRenderedTooltip(tree, "/items"));
		assertEquals("Object (2 properties)", getRenderedTooltip(tree, "/settings"));
	}

	@Test
	public void filteredArrayAndObjectTooltipsKeepSourceChildCounts() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/items", "array",
				"/items/0", "\"first\"",
				"/items/1", "\"second\"",
				"/settings", "object",
				"/settings/name", "\"Ada\"",
				"/settings/count", "2")));

		tree.setFilterText("name");

		assertEquals("Object (2 properties)", getRenderedTooltip(tree, "/settings"));

		tree.setFilterText("0");

		assertEquals("Array (2 items)", getRenderedTooltip(tree, "/items"));
	}

	@Test
	public void treeFiltersNodesByNameAsTextChanges() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"",
				"/users/0/name", "\"Ada\"",
				"/settings", "object")));

		tree.setFilterText("email");

		JsonTreeModel filtered = (JsonTreeModel) tree.getModel();
		assertNotNull(filtered.getNodeByKey("/users"));
		assertNotNull(filtered.getNodeByKey("/users/0"));
		assertNotNull(filtered.getNodeByKey("/users/0/email"));
		assertNull(filtered.getNodeByKey("/users/0/name"));
		assertNull(filtered.getNodeByKey("/settings"));

		tree.setFilterText("");

		JsonTreeModel unfiltered = (JsonTreeModel) tree.getModel();
		assertNotNull(unfiltered.getNodeByKey("/users/0/name"));
		assertNotNull(unfiltered.getNodeByKey("/settings"));
	}

	@Test
	public void treeFiltersByJsonTypeOperator() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/name", "\"Ada\"",
				"/enabled", "true")));

		tree.setFilterText("type:array");

		JsonTreeModel filtered = (JsonTreeModel) tree.getModel();
		assertNotNull(filtered.getNodeByKey("/users"));
		assertNull(filtered.getNodeByKey("/users/0"));
		assertNull(filtered.getNodeByKey("/enabled"));

		tree.setFilterText("type:string");

		filtered = (JsonTreeModel) tree.getModel();
		assertNotNull(filtered.getNodeByKey("/users/0/name"));
		assertNull(filtered.getNodeByKey("/enabled"));
	}

	@Test
	public void treeFiltersByJsonPathOperator() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/name", "\"Ada\"",
				"/settings", "object")));

		tree.setFilterText("path:/users");

		JsonTreeModel filtered = (JsonTreeModel) tree.getModel();
		assertNotNull(filtered.getNodeByKey("/users"));
		assertNotNull(filtered.getNodeByKey("/users/0/name"));
		assertNull(filtered.getNodeByKey("/settings"));
	}

	@Test
	public void matchingFilteredNodeKeepsItsDescendantsExpandable() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/agent", "object",
				"/agent/config", "object",
				"/agent/config/prompt_variables", "object",
				"/agent/config/prompt_variables/temperature", "0.7",
				"/agent/config/prompt_variables/max_tokens", "1024",
				"/agent/config/prompt", "\"Answer clearly\"")));

		tree.setFilterText("prom");

		JsonTreeModel filtered = (JsonTreeModel) tree.getModel();
		JsonTreeNode promptVariables = filtered.getNodeByKey("/agent/config/prompt_variables");
		assertNotNull(promptVariables);
		assertFalse(promptVariables.isLeaf());
		assertNotNull(filtered.getNodeByKey("/agent/config/prompt_variables/temperature"));
		assertNotNull(filtered.getNodeByKey("/agent/config/prompt_variables/max_tokens"));
	}

	@Test
	public void filteringKeepsSourceModelUnchanged() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"",
				"/users/0/name", "\"Ada\"")));

		tree.setFilterText("email");

		assertNull(((JsonTreeModel) tree.getModel()).getNodeByKey("/users/0/name"));
		assertNotNull(tree.getSourceModel().getNodeByKey("/users/0/name"));
	}

	@Test
	public void treeMutationsUpdateSourceModelAndVisibleModel() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"",
				"/settings", "object",
				"/settings/theme", "\"dark\"")));

		tree.renameNodeByKey("/users/0/email", "/users/0/contact");
		tree.duplicateNodeByKey("/users/0/contact", "/users/0/contactCopy");
		tree.updateNodeJsonType("/users/0/contactCopy", JsonNodeType.Number);
		tree.removeNodeByKey("/users/0/contact");

		assertNull(tree.getSourceModel().getNodeByKey("/users/0/email"));
		assertNull(tree.getSourceModel().getNodeByKey("/users/0/contact"));
		assertNotNull(tree.getSourceModel().getNodeByKey("/users/0/contactCopy"));
		assertSame(JsonNodeType.Number, tree.getSourceModel().getNodeByKey("/users/0/contactCopy").getJsonType());
		assertNotNull(((JsonTreeModel) tree.getModel()).getNodeByKey("/users/0/contactCopy"));
	}

	@Test
	public void collapseAllStartsAtLastValidRow() {
		MessageBundle.loadResources();

		RecordingCollapseTree tree = new RecordingCollapseTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"",
				"/settings", "object")));
		tree.expandAll();
		int rowCount = tree.getRowCount();

		tree.clearCollapsedRows();
		tree.collapseAll();

		assertFalse(tree.getCollapsedRows().contains(rowCount));
		assertEquals(rowCount - 1, tree.getCollapsedRows().getFirst());
	}

	@Test
	public void duplicateNodeByKeyIgnoresMissingSourceKey() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"")));

		assertDoesNotThrow(() -> tree.duplicateNodeByKey("/users/0/missing", "/users/0/missingCopy"));

		assertNull(tree.getSourceModel().getNodeByKey("/users/0/missingCopy"));
		assertNotNull(tree.getSourceModel().getNodeByKey("/users/0/email"));
	}

	@Test
	public void addNodeByKeyIgnoresKeyWhenParentCannotBeResolved() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel());

		assertDoesNotThrow(() -> tree.addNodeByKey(null));
	}

	@Test
	public void addingJsonPathNodeCreatesEscapedSourceAndVisibleKeys() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"",
				"object",
				"/a.b",
				"object")));

		JsonTreeNode added = tree.addNodeByKey("/a.b/slash~1key");

		assertEquals("slash/key", added.getName());
		assertEquals("/a.b/slash~1key", added.getKey());
		assertSame(JsonNodeType.String, added.getJsonType());
		assertNotNull(tree.getSourceModel().getNodeByKey("/a.b/slash~1key"));
		assertNotNull(((JsonTreeModel) tree.getModel()).getNodeByKey("/a.b/slash~1key"));
	}

	@Test
	public void sourceModelNodeSelectionUsesVisibleNodeWhenFiltered() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"",
				"/users/0/name", "\"Ada\"")));
		tree.setFilterText("email");

		tree.setSelectionNode(tree.getNodeByKey("/users/0/email"));

		assertEquals("/users/0/email", tree.getSelectionNode().getKey());
		assertTrue(tree.getRowForPath(tree.getSelectionPath()) >= 0);
	}

	@Test
	public void hiddenSourceModelSelectionIsIgnoredWhenFiltered() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"",
				"/users/0/name", "\"Ada\"")));
		tree.setFilterText("email");
		tree.setSelectionNode(tree.getNodeByKey("/users/0/email"));

		tree.setSelectionNode(tree.getNodeByKey("/users/0/name"));

		assertEquals("/users/0/email", tree.getSelectionNode().getKey());
		assertTrue(tree.getRowForPath(tree.getSelectionPath()) >= 0);
	}

	@Test
	public void filteredIndividualCollapseSurvivesModelRefresh() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"",
				"/users/0/profile", "object",
				"/users/0/profile/name", "\"Ada\"")));
		tree.setFilterText("email");
		JsonTreeNode visibleNode = ((JsonTreeModel) tree.getModel()).getNodeByKey("/users");
		TreePath path = new TreePath(visibleNode.getPath());
		tree.collapsePath(path);

		// Re-applying the same filter text triggers a collapse-preserving model refresh.
		tree.setFilterText("email");

		JsonTreeNode refreshedNode = ((JsonTreeModel) tree.getModel()).getNodeByKey("/users");
		TreePath refreshedPath = new TreePath(refreshedNode.getPath());
		assertTrue(tree.isCollapsed(refreshedPath));
	}

	@Test
	public void arrowKeysMoveTreeSelectionUpAndDown() {
		MessageBundle.loadResources();
		JsonTree tree = createKeyboardNavigationTree();
		tree.setSelectionRow(1);

		performTreeKey(tree, KeyEvent.VK_DOWN);

		assertEquals(2, tree.getSelectionRows()[0]);

		performTreeKey(tree, KeyEvent.VK_UP);

		assertEquals(1, tree.getSelectionRows()[0]);
	}

	@Test
	public void arrowKeysClampTreeSelectionAtVisibleBounds() {
		MessageBundle.loadResources();
		JsonTree tree = createKeyboardNavigationTree();
		tree.setSelectionRow(0);

		assertDoesNotThrow(() -> performTreeKey(tree, KeyEvent.VK_UP));

		assertEquals(0, tree.getSelectionRows()[0]);

		tree.setSelectionRow(tree.getRowCount() - 1);

		assertDoesNotThrow(() -> performTreeKey(tree, KeyEvent.VK_DOWN));

		assertEquals(tree.getRowCount() - 1, tree.getSelectionRows()[0]);
	}

	@Test
	public void homeAndEndKeysMoveTreeSelectionToVisibleBounds() {
		MessageBundle.loadResources();
		JsonTree tree = createKeyboardNavigationTree();
		tree.setSelectionRow(1);

		performTreeKey(tree, KeyEvent.VK_END);

		assertEquals(tree.getRowCount() - 1, tree.getSelectionRows()[0]);

		performTreeKey(tree, KeyEvent.VK_HOME);

		assertEquals(0, tree.getSelectionRows()[0]);
	}

	@Test
	public void leftAndRightKeysCollapseAndExpandSelectedTreeNode() {
		MessageBundle.loadResources();
		JsonTree tree = createKeyboardNavigationTree();
		JsonTreeNode users = ((JsonTreeModel) tree.getModel()).getNodeByKey("/users");
		TreePath usersPath = new TreePath(users.getPath());
		tree.collapsePath(usersPath);
		tree.setSelectionPath(usersPath);

		performTreeKey(tree, KeyEvent.VK_RIGHT);

		assertTrue(tree.isExpanded(usersPath));

		performTreeKey(tree, KeyEvent.VK_LEFT);

		assertTrue(tree.isCollapsed(usersPath));
	}

	@Test
	public void enterKeyTogglesSelectedExpandableTreeNode() {
		MessageBundle.loadResources();
		JsonTree tree = createKeyboardNavigationTree();
		JsonTreeNode users = ((JsonTreeModel) tree.getModel()).getNodeByKey("/users");
		TreePath usersPath = new TreePath(users.getPath());
		tree.collapsePath(usersPath);
		tree.setSelectionPath(usersPath);

		performTreeKey(tree, KeyEvent.VK_ENTER);

		assertTrue(tree.isExpanded(usersPath));

		performTreeKey(tree, KeyEvent.VK_ENTER);

		assertTrue(tree.isCollapsed(usersPath));
	}

	@Test
	public void enterKeyLeavesSelectedLeafNodeUnchanged() {
		MessageBundle.loadResources();
		JsonTree tree = createKeyboardNavigationTree();
		JsonTreeNode email = ((JsonTreeModel) tree.getModel()).getNodeByKey("/users/0/email");
		TreePath emailPath = new TreePath(email.getPath());
		tree.setSelectionPath(emailPath);

		assertDoesNotThrow(() -> performTreeKey(tree, KeyEvent.VK_ENTER));

		assertEquals(emailPath, tree.getSelectionPath());
		assertTrue(email.isLeaf());
	}

	@Test
	public void addInitialKeyUsesJsonPathSeparatorForJsonProjects() {
		MessageBundle.loadResources();
		EditorProject project = new EditorProject(null);
		JsonTreeNode node = new JsonTreeModel(List.of("/users")).getNodeByKey("/users");

		assertEquals("/users/", Editor.createAddInitialKey(project, node));
	}

	@Test
	public void paintingTreeWithoutSelectionDoesNotThrow() {
		MessageBundle.loadResources();

		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of("/name", "\"Ada\"")));
		tree.setSize(240, 160);
		tree.clearSelection();
		BufferedImage image = new BufferedImage(240, 160, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();

		assertDoesNotThrow(() -> tree.paint(graphics));

		graphics.dispose();
	}

	@Test
	public void jsonTreeRegistersTooltips() {
		JsonTree tree = new JsonTree();

		assertTrue(Arrays.asList(tree.getMouseListeners()).contains(ToolTipManager.sharedInstance())
				|| Arrays.asList(tree.getMouseMotionListeners()).contains(ToolTipManager.sharedInstance()));
	}

	@Test
	public void jsonTypeIconDoesNotPaintBadgeBackground() {
		BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		JsonTypeIcon icon = new JsonTypeIcon(JsonNodeType.Number);
		Graphics2D graphics = image.createGraphics();

		icon.paintIcon(new JLabel(), graphics, 1, 1);
		graphics.dispose();

		int alpha = image.getRGB(4, 4) >>> 24;
		assertEquals(0, alpha);
	}

	@Test
	public void jsonTypeIconIsLargerAndBorderless() {
		BufferedImage image = new BufferedImage(48, 24, BufferedImage.TYPE_INT_ARGB);
		JsonTypeIcon icon = new JsonTypeIcon(JsonNodeType.Number);
		Graphics2D graphics = image.createGraphics();

		icon.paintIcon(new JLabel(), graphics, 1, 1);
		graphics.dispose();

		// Size is now derived from font metrics (with minimum floors) so the badge
		// grows with the font/HiDPI instead of clipping; assert the floors, not exact values.
		assertTrue(icon.getIconWidth() >= 44);
		assertTrue(icon.getIconHeight() >= 20);
		assertEquals(0, image.getRGB(1, 1) >>> 24);
	}

	@Test
	public void stringIconUsesTextGlyphInsteadOfSingleQuote() {
		assertTrue(countPaintedPixels(new JsonTypeIcon(JsonNodeType.String)) > 20);
	}

	@Test
	public void booleanIconUsesNeutralTextGlyphInsteadOfCheckmark() {
		assertTrue(imagesPaintDiffer(paintIcon(new JsonTypeIcon(JsonNodeType.Boolean)), paintBooleanCheckmarkIcon()));
	}

	@Test
	public void nullIconUsesEmptySetGlyphInsteadOfLetterN() {
		assertTrue(imagesPaintDiffer(paintIcon(new JsonTypeIcon(JsonNodeType.Null)), paintTextGlyph("N", JsonNodeType.Null)));
	}
	
	@Test
	public void arrayAndObjectIconsPaintChildCounts() {
		assertTrue(iconsPaintDiffer(new JsonTypeIcon(JsonNodeType.Array, 12), new JsonTypeIcon(JsonNodeType.Array)));
		assertTrue(iconsPaintDiffer(new JsonTypeIcon(JsonNodeType.Object, 4), new JsonTypeIcon(JsonNodeType.Object)));
	}

	@Test
	public void arrayAndObjectCountGlyphsRenderCountInsideBrackets() {
		assertEquals("{2}", JsonTypeIcon.collectionGlyph("{}", 2));
		assertEquals("[7]", JsonTypeIcon.collectionGlyph("[]", 7));
		assertEquals("{99+}", JsonTypeIcon.collectionGlyph("{}", 100));
	}

	@Test
	public void collectionTypeGlyphUsesRowFontSize() {
		assertTrue(getPaintedBounds(new JsonTypeIcon(JsonNodeType.Object, 12), 17f).height >= 12);
	}

	private String getRenderedTooltip(JsonTree tree, String key) {
		JsonTreeNode node = ((JsonTreeModel) tree.getModel()).getNodeByKey(key);
		Component component = tree.getCellRenderer().getTreeCellRendererComponent(
				tree, node, false, false, false, 1, false);
		return ((JComponent) component).getToolTipText();
	}

	private JsonTree createKeyboardNavigationTree() {
		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of(
				"/users", "array",
				"/users/0", "object",
				"/users/0/email", "\"person@example.com\"",
				"/settings", "object",
				"/settings/theme", "\"dark\"")));
		tree.expandAll();
		assertTrue(tree.getRowCount() > 3);
		return tree;
	}

	private void performTreeKey(JsonTree tree, int keyCode) {
		Object actionKey = tree.getInputMap().get(KeyStroke.getKeyStroke(keyCode, 0));
		assertNotNull(actionKey);
		Action action = tree.getActionMap().get(actionKey);
		assertNotNull(action);
		action.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, actionKey.toString()));
	}

	private int countPaintedPixels(JsonTypeIcon icon) {
		BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		icon.paintIcon(new JLabel(), graphics, 1, 1);
		graphics.dispose();
		return countPaintedPixels(image);
	}

	private int countPaintedPixels(BufferedImage image) {
		int painted = 0;
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				if ((image.getRGB(x, y) >>> 24) > 0) {
					painted++;
				}
			}
		}
		return painted;
	}

	private int colorDistance(Color first, Color second) {
		return Math.abs(first.getRed() - second.getRed())
				+ Math.abs(first.getGreen() - second.getGreen())
				+ Math.abs(first.getBlue() - second.getBlue());
	}

	private Rectangle getPaintedBounds(JsonTypeIcon icon, float fontSize) {
		JLabel label = new JLabel();
		label.setFont(label.getFont().deriveFont(fontSize));
		BufferedImage image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		icon.paintIcon(label, graphics, 1, 1);
		graphics.dispose();

		Rectangle bounds = null;
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				if ((image.getRGB(x, y) >>> 24) > 0) {
					if (bounds == null) {
						bounds = new Rectangle(x, y, 1, 1);
					} else {
						bounds.add(x, y);
					}
				}
			}
		}
		return bounds == null ? new Rectangle() : bounds;
	}

	private boolean iconsPaintDiffer(JsonTypeIcon first, JsonTypeIcon second) {
		BufferedImage firstImage = paintIcon(first);
		BufferedImage secondImage = paintIcon(second);
		return imagesPaintDiffer(firstImage, secondImage);
	}

	private boolean imagesPaintDiffer(BufferedImage firstImage, BufferedImage secondImage) {
		for (int x = 0; x < firstImage.getWidth(); x++) {
			for (int y = 0; y < firstImage.getHeight(); y++) {
				if (firstImage.getRGB(x, y) != secondImage.getRGB(x, y)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean iconPaintsColor(Icon icon, JLabel label, Color color) {
		BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		icon.paintIcon(label, graphics, 0, 0);
		graphics.dispose();
		int expectedRgb = color.getRGB() & 0x00ffffff;
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				int argb = image.getRGB(x, y);
				if ((argb >>> 24) > 0 && (argb & 0x00ffffff) == expectedRgb) {
					return true;
				}
			}
		}
		return false;
	}

	private BufferedImage paintBooleanCheckmarkIcon() {
		BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(44, 126, 159));
		graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(5, 9, 9, 13);
		graphics.drawLine(9, 13, 17, 5);
		graphics.dispose();
		return image;
	}

	private BufferedImage paintIcon(JsonTypeIcon icon) {
		BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		icon.paintIcon(new JLabel(), graphics, 1, 1);
		graphics.dispose();
		return image;
	}

	private BufferedImage paintTextGlyph(String text, JsonNodeType type) {
		JsonTypeIcon icon = new JsonTypeIcon(type);
		JLabel label = new JLabel();
		BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Font font = label.getFont().deriveFont(Font.BOLD);
		graphics.setFont(font);
		graphics.setColor(JsonTypeIcon.colorFor(type));
		FontMetrics metrics = graphics.getFontMetrics();
		int textX = 1 + (icon.getIconWidth() - metrics.stringWidth(text)) / 2;
		int textY = 1 + (icon.getIconHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
		graphics.drawString(text, textX, textY);
		graphics.dispose();
		return image;
	}

	private static class RecordingCollapseTree extends JsonTree {
		private final List<Integer> collapsedRows = new ArrayList<>();

		@Override
		public void collapseRow(int row) {
			if (collapsedRows != null) {
				collapsedRows.add(row);
			}
			super.collapseRow(row);
		}

		private void clearCollapsedRows() {
			collapsedRows.clear();
		}

		private List<Integer> getCollapsedRows() {
			return collapsedRows;
		}
	}
}
