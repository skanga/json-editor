package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.model.JsonDocument;
import com.skanga.jsoneditor.util.DuplicateJsonKey;
import com.skanga.jsoneditor.util.MessageBundle;

public class EditorLayoutTest {
	@Test
	public void jsonKeyFieldSpansBelowWholeSplitPane() {
		MessageBundle.loadResources();
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane(), new JScrollPane());
		JsonKeyField keyField = new JsonKeyField();
		
		JPanel panel = Editor.createProjectPanel(splitPane, keyField);
		BorderLayout layout = (BorderLayout) panel.getLayout();
		
		assertSame(splitPane, layout.getLayoutComponent(BorderLayout.CENTER));
		assertSame(keyField, layout.getLayoutComponent(BorderLayout.SOUTH));
	}
	
	@Test
	public void treeFilterFieldSitsAboveTreeScrollPaneWithoutViewButtons() {
		JTextField filterField = new JTextField();
		JScrollPane treeScrollPane = new JScrollPane();
		JsonTree tree = new JsonTree();
		
		JPanel panel = Editor.createJsonTreePanel(filterField, tree, treeScrollPane, Color.GRAY);
		BorderLayout layout = (BorderLayout) panel.getLayout();
		JPanel fixedPanel = (JPanel) layout.getLayoutComponent(BorderLayout.NORTH);
		JPanel searchPanel = (JPanel) ((BorderLayout) fixedPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
		
		assertSame(filterField, ((BorderLayout) searchPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER));
		assertNull(((BorderLayout) fixedPanel.getLayout()).getLayoutComponent(BorderLayout.SOUTH));
		assertSame(treeScrollPane, layout.getLayoutComponent(BorderLayout.CENTER));
	}
	
	@Test
	public void jsonTreePanelOwnsSingleContinuousRightDivider() {
		MessageBundle.loadResources();
		
		JPanel panel = Editor.createJsonTreePanel(new JTextField(), new JsonTree(), new JScrollPane(), Color.GRAY);
		MatteBorder border = (MatteBorder) panel.getBorder();
		Insets insets = border.getBorderInsets(panel);
		
		assertEquals(Color.GRAY, border.getMatteColor());
		assertEquals(0, insets.top);
		assertEquals(0, insets.left);
		assertEquals(0, insets.bottom);
		assertEquals(1, insets.right);
	}
	
	@Test
	public void treeScrollPaneDoesNotDrawDuplicateDivider() {
		MessageBundle.loadResources();
		
		JScrollPane scrollPane = Editor.createJsonTreeScrollPane(new JsonTree());
		
		assertEquals(null, scrollPane.getBorder());
	}
	
	@Test
	public void treeFilterRowHasSearchLabelAndPadding() {
		JTextField filterField = new JTextField();
		
		JPanel searchPanel = Editor.createTreeFilterPanel(filterField);
		BorderLayout layout = (BorderLayout) searchPanel.getLayout();
		JLabel label = (JLabel) layout.getLayoutComponent(BorderLayout.WEST);
		EmptyBorder border = (EmptyBorder) searchPanel.getBorder();
		Insets insets = border.getBorderInsets(searchPanel);
		
		assertEquals("Find", label.getText());
		assertSame(filterField, layout.getLayoutComponent(BorderLayout.CENTER));
		assertEquals(12, insets.top);
		assertEquals(20, insets.left);
		assertEquals(10, insets.bottom);
		assertEquals(20, insets.right);
		assertEquals(new Dimension(56, 34), label.getPreferredSize());
		assertEquals(34, filterField.getPreferredSize().height);
	}
	
	@Test
	public void menuBarDoesNotDrawExtraBottomLine() {
		JMenuBar menuBar = new JMenuBar();
		
		EditorMenuBar.configureBorder(menuBar);
		
		assertEquals(null, menuBar.getBorder());
		assertFalse(menuBar.isBorderPainted());
	}
	
	@Test
	public void treeFilterFieldUsesFullOnePixelOuterBorder() {
		MessageBundle.loadResources();
		JTextField filterField = Editor.createTreeFilterField(Color.GRAY);
		CompoundBorder border = (CompoundBorder) filterField.getBorder();
		MatteBorder outerBorder = (MatteBorder) border.getOutsideBorder();
		
		Insets insets = outerBorder.getBorderInsets(filterField);
		
		assertEquals(Color.GRAY, outerBorder.getMatteColor());
		assertEquals(1, insets.top);
		assertEquals(1, insets.left);
		assertEquals(1, insets.bottom);
		assertEquals(1, insets.right);
	}

	@Test
	public void treeFilterFieldKeepsRichLocalizedTooltip() {
		MessageBundle.loadResources();

		JTextField filterField = Editor.createTreeFilterField(Color.GRAY);

		assertEquals(MessageBundle.get("tree.filter.tooltip"), filterField.getToolTipText());
		assertTrue(filterField.getToolTipText().startsWith("<html>"));
		assertTrue(filterField.getToolTipText().contains("type:string"));
		assertTrue(filterField.getToolTipText().contains("path:/foo/bar"));
		assertTrue(filterField.getToolTipText().contains("Esc"));
	}
	
	@Test
	public void treeFilterFieldShowsClearButtonWhenTextIsPresent() {
		MessageBundle.loadResources();
		TreeFilterField filterField = (TreeFilterField) Editor.createTreeFilterField(Color.GRAY);
		filterField.setSize(180, 28);
		
		filterField.setText("");
		assertFalse(filterField.isClearButtonVisible());
		
		filterField.setText("email");
		
		assertTrue(filterField.isClearButtonVisible());
		Rectangle clearButton = filterField.getClearButtonBounds();
		assertTrue(clearButton.x > 150);
	}
	
	@Test
	public void treeFilterFieldClearButtonClearsText() {
		MessageBundle.loadResources();
		TreeFilterField filterField = (TreeFilterField) Editor.createTreeFilterField(Color.GRAY);
		filterField.setSize(180, 28);
		filterField.setText("email");
		
		Rectangle clearButton = filterField.getClearButtonBounds();
		filterField.clearAt(clearButton.x + clearButton.width / 2, clearButton.y + clearButton.height / 2);
		
		assertEquals("", filterField.getText());
	}

	@Test
	public void duplicateKeyWarningPanelUsesRedWarningPrefixAndCloseButton() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = new JsonDocument(null);
		jsonDocument.setDuplicateKeys(java.util.List.of(new DuplicateJsonKey("role", "/role")));

		JPanel panel = Editor.createDuplicateKeyWarningPanel(jsonDocument, () -> {});
		JLabel message = (JLabel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
		JPanel closeHolder = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.EAST);
		JButton close = (JButton) ((BorderLayout) closeHolder.getLayout()).getLayoutComponent(BorderLayout.NORTH);

		assertTrue(message.getText().contains("<span style=\"color:red\">WARNING: </span>"));
		assertTrue(message.getFont().getSize2D() > new JLabel().getFont().getSize2D());
		assertEquals("X", close.getText());
		assertEquals(MessageBundle.get("dialogs.duplicatekeys.dismiss.tooltip"), close.getToolTipText());
		assertTrue(close.getPreferredSize().height < 40);
		panel.setSize(900, 110);
		panel.doLayout();
		closeHolder.doLayout();
		assertTrue(close.getHeight() < 40);
	}

	@Test
	public void duplicateKeyWarningCloseActionRunsDismissCallback() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = new JsonDocument(null);
		jsonDocument.setDuplicateKeys(java.util.List.of(new DuplicateJsonKey("role", "/role")));
		java.util.concurrent.atomic.AtomicInteger dismissals = new java.util.concurrent.atomic.AtomicInteger();

		JPanel panel = Editor.createDuplicateKeyWarningPanel(jsonDocument, dismissals::incrementAndGet);
		JPanel closeHolder = (JPanel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.EAST);
		JButton close = (JButton) ((BorderLayout) closeHolder.getLayout()).getLayoutComponent(BorderLayout.NORTH);
		close.doClick();

		assertEquals(1, dismissals.get());
	}

	@Test
	public void duplicateKeyWarningAndTitleDividerUseSameDarkerBorderColor() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = new JsonDocument(null);
		jsonDocument.setDuplicateKeys(java.util.List.of(new DuplicateJsonKey("role", "/role")));
		JPanel warning = Editor.createDuplicateKeyWarningPanel(jsonDocument, () -> {});
		JsonValueEditorPanel valuePanel = new JsonValueEditorPanel(new NoopJsonValueActions());

		MatteBorder warningBorder = (MatteBorder) ((CompoundBorder) warning.getBorder()).getOutsideBorder();
		Color warningDivider = warningBorder.getMatteColor();
		Color titleDivider = valuePanel.titleDividerColor();
		java.awt.Insets insets = warningBorder.getBorderInsets();

		assertEquals(2, insets.top);
		assertEquals(2, insets.left);
		assertEquals(2, warningBorder.getBorderInsets().bottom);
		assertEquals(2, insets.right);
		assertEquals(warningDivider, titleDivider);
	}

	private static class NoopJsonValueActions implements JsonValueEditorPanel.Actions {
		@Override public void setLiteral(String key, String literal) {}
		@Override public void addChild(String key) {}
		@Override public boolean changeType(String key, JsonNodeType type) { return true; }
		@Override public void delete(String key) {}
		@Override public void duplicate(String key) {}
		@Override public void rename(String key) {}
		@Override public void moveUp(String key) {}
		@Override public void moveDown(String key) {}
	}
	
}
