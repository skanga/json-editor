package com.skanga.jsoneditor.editor;

import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.io.Serial;

/**
 * Right-click context menu for a single node in the JSON tree.
 */
public class JsonTreeNodeMenu extends JPopupMenu {
	@Serial
    private final static long serialVersionUID = -8450484152294368841L;

	public JsonTreeNodeMenu(Editor editor, JsonTreeNode node) {
		super();
		add(EditorMenuItems.addKey(editor, node, true));
		if (!node.isRoot()) {
			addSeparator();
			add(createChangeTypeMenu(editor));
			add(EditorMenuItems.renameKey(editor, true));
			add(EditorMenuItems.duplicateKey(editor, true));
			add(EditorMenuItems.removeKey(editor, true));
			add(EditorMenuItems.copyKey(editor, true));
		}
	}

	private JMenu createChangeTypeMenu(Editor editor) {
		JMenu menu = new JMenu("Change Type...");
		for (JsonNodeType type : new JsonNodeType[] {
				JsonNodeType.String, JsonNodeType.Number, JsonNodeType.Boolean,
				JsonNodeType.Null, JsonNodeType.Object, JsonNodeType.Array }) {
			JMenuItem item = new JMenuItem(type.getLabel());
			item.addActionListener(e -> editor.changeSelectedType(type));
			menu.add(item);
		}
		return menu;
	}
}
