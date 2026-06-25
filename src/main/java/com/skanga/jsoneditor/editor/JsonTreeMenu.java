package com.skanga.jsoneditor.editor;

import javax.swing.JPopupMenu;
import java.io.Serial;

/**
 * Right-click context menu for the JSON tree (no node selected).
 */
public class JsonTreeMenu extends JPopupMenu {
	@Serial
    private final static long serialVersionUID = -8450484152294368841L;

	public JsonTreeMenu(Editor editor, JsonTree tree) {
		super();
		add(EditorMenuItems.addKey(editor, tree, true));
		add(EditorMenuItems.findKey(editor, true));
		addSeparator();
		add(EditorMenuItems.expandAll(tree));
		add(EditorMenuItems.collapseAll(tree));
	}
}
