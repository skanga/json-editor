package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.MessageBundle;

public class EditorMenuItemsTest {
	@Test
	public void editMenuItemsKeepLabelsAcceleratorsAndEnabledState() {
		MessageBundle.loadResources();
		JsonTree tree = new JsonTree();
		int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		
		assertMenuItem(EditorMenuItems.addKey(null, tree, false),
				"Add Key...", KeyStroke.getKeyStroke(KeyEvent.VK_T, keyMask), false, "ADD_KEY");
		assertMenuItem(EditorMenuItems.findKey(null, true),
				"Find Key...", KeyStroke.getKeyStroke(KeyEvent.VK_F, keyMask), true, "FIND_KEY");
		assertMenuItem(EditorMenuItems.renameKey(null, false),
				"Rename Key...", KeyStroke.getKeyStroke(KeyEvent.VK_R, keyMask), false, "RENAME_KEY");
		assertMenuItem(EditorMenuItems.duplicateKey(null, true),
				"Duplicate Key...", KeyStroke.getKeyStroke(KeyEvent.VK_D, keyMask), true, "DUPLICATE_KEY");
		assertMenuItem(EditorMenuItems.removeKey(null, false),
				"Delete Key", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), false, "DELETE_KEY");
		assertMenuItem(EditorMenuItems.copyKey(null, true),
				"Copy Key", KeyStroke.getKeyStroke(KeyEvent.VK_C, keyMask), true, "COPY_KEY");
	}
	
	@Test
	public void viewMenuItemsKeepLabelsAndNoAccelerators() {
		MessageBundle.loadResources();
		JsonTree tree = new JsonTree();
		
		assertMenuItem(EditorMenuItems.expandAll(tree), "Expand All Keys", null, true, "EXPAND");
		assertMenuItem(EditorMenuItems.collapseAll(tree), "Collapse All Keys", null, true, "COLLAPSE");
	}
	
	private void assertMenuItem(JMenuItem item, String text, KeyStroke accelerator, boolean enabled, String command) {
		assertEquals(text, item.getText());
		assertEquals(accelerator, item.getAccelerator());
		assertEquals(enabled, item.isEnabled());
		assertEquals(command, item.getClientProperty("editorCommand"));
		assertFalse(item.getActionListeners().length == 0);
		if (accelerator == null) {
			assertNull(item.getAccelerator());
		}
	}
}
