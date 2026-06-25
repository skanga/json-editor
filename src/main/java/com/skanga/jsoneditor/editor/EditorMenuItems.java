package com.skanga.jsoneditor.editor;

import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

final class EditorMenuItems {
	private EditorMenuItems() {
	}
	
	static JMenuItem addKey(Editor editor, JsonTreeNode node, boolean enabled) {
		return item(EditorCommand.ADD_KEY, shortcut(KeyEvent.VK_T), enabled,
				e -> editor.showAddDialog(node));
	}
	
	static JMenuItem addKey(Editor editor, JsonTree tree, boolean enabled) {
		return item(EditorCommand.ADD_KEY, shortcut(KeyEvent.VK_T), enabled,
				e -> editor.showAddDialog(tree.getSelectionNode()));
	}
	
	static JMenuItem findKey(Editor editor, boolean enabled) {
		return item(EditorCommand.FIND_KEY, shortcut(KeyEvent.VK_F), enabled,
				e -> editor.showFindDialog());
	}
	
	static JMenuItem renameKey(Editor editor, boolean enabled) {
		return renameKey(editor, enabled, shortcut(KeyEvent.VK_R));
	}
	
	static JMenuItem renameKey(Editor editor, boolean enabled, KeyStroke accelerator) {
		return item(EditorCommand.RENAME_KEY, accelerator, enabled,
				e -> editor.renameSelectedKey());
	}
	
	static JMenuItem duplicateKey(Editor editor, boolean enabled) {
		return item(EditorCommand.DUPLICATE_KEY, shortcut(KeyEvent.VK_D), enabled,
				e -> editor.duplicateSelectedKey());
	}
	
	static JMenuItem removeKey(Editor editor, boolean enabled) {
		return item(EditorCommand.DELETE_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), enabled,
				e -> editor.removeSelectedKey());
	}
	
	static JMenuItem copyKey(Editor editor, boolean enabled) {
		return item(EditorCommand.COPY_KEY, shortcut(KeyEvent.VK_C), enabled,
				e -> editor.copySelectedKey());
	}
	
	static JMenuItem expandAll(JsonTree tree) {
		return item(EditorCommand.EXPAND, null, true, e -> tree.expandAll());
	}
	
	static JMenuItem collapseAll(JsonTree tree) {
		return item(EditorCommand.COLLAPSE, null, true, e -> tree.collapseAll());
	}
	
	private static JMenuItem item(EditorCommand command, KeyStroke accelerator, boolean enabled, ActionListener action) {
		JMenuItem item = new JMenuItem(command.text());
		item.setAccelerator(accelerator);
		item.setEnabled(enabled);
		command.applyTo(item);
		item.addActionListener(action);
		return item;
	}
	
	private static KeyStroke shortcut(int keyCode) {
		return KeyStroke.getKeyStroke(keyCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
	}
}
