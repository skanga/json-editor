package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.MessageBundle;

public class EditorMenuBarTest {
	@Test
	public void menuItemsExposeSharedEditorCommandIds() {
		MessageBundle.loadResources();
		
		EditorMenuBar menuBar = new EditorMenuBar(null, new JsonTree());
		
		assertCommand(menuBar, EditorCommand.NEW);
		assertCommand(menuBar, EditorCommand.OPEN);
		assertCommand(menuBar, EditorCommand.SAVE);
		assertCommand(menuBar, EditorCommand.CLOSE);
		assertCommand(menuBar, EditorCommand.UNDO);
		assertCommand(menuBar, EditorCommand.REDO);
		assertCommand(menuBar, EditorCommand.ADD_KEY);
		assertCommand(menuBar, EditorCommand.FIND_KEY);
		assertCommand(menuBar, EditorCommand.RENAME_KEY);
		assertCommand(menuBar, EditorCommand.DUPLICATE_KEY);
		assertCommand(menuBar, EditorCommand.DELETE_KEY);
		assertCommand(menuBar, EditorCommand.COPY_KEY);
		assertCommand(menuBar, EditorCommand.EXPAND);
		assertCommand(menuBar, EditorCommand.COLLAPSE);
	}
	
	@Test
	public void projectScopedMenuCommandsFollowProjectOpenState() {
		MessageBundle.loadResources();
		
		EditorMenuBar menuBar = new EditorMenuBar(null, new JsonTree());
		
		assertTrue(commandItem(menuBar, EditorCommand.NEW).isEnabled());
		assertTrue(commandItem(menuBar, EditorCommand.OPEN).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.SAVE).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.CLOSE).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.EXPAND).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.COLLAPSE).isEnabled());
		
		menuBar.setEnabled(true);
		
		assertTrue(commandItem(menuBar, EditorCommand.CLOSE).isEnabled());
		assertTrue(commandItem(menuBar, EditorCommand.EXPAND).isEnabled());
		assertTrue(commandItem(menuBar, EditorCommand.COLLAPSE).isEnabled());
		
		menuBar.setEnabled(false);
		
		assertFalse(commandItem(menuBar, EditorCommand.CLOSE).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.EXPAND).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.COLLAPSE).isEnabled());
	}

	@Test
	public void settingsMenuShowsOnlyEditorPreferencesWithoutOpenProject() {
		MessageBundle.loadResources();

		EditorMenuBar menuBar = new EditorMenuBar(null, new JsonTree());
		JMenu settingsMenu = findMenu(menuBar, "Settings");

		assertNotNull(settingsMenu);
		assertNotNull(findMenuItem(settingsMenu, "Preferences..."));
		assertNull(findMenuItem(settingsMenu, "JSON File Preferences..."));
	}

	@Test
	public void settingsMenuShowsProjectAndEditorPreferencesWithOpenProject() {
		MessageBundle.loadResources();

		EditorMenuBar menuBar = new EditorMenuBar(null, new JsonTree());
		menuBar.setEnabled(true);
		JMenu settingsMenu = findMenu(menuBar, "Settings");

		assertNotNull(settingsMenu);
		assertNotNull(findMenuItem(settingsMenu, "JSON File Preferences..."));
		assertNotNull(findMenuItem(settingsMenu, "Preferences..."));
	}
	
	@Test
	public void nodeScopedMenuCommandsRequireNonRootSelection() {
		MessageBundle.loadResources();
		JsonTree tree = new JsonTree();
		tree.setModel(new JsonTreeModel(Map.of("/name", "\"Ada\"")));
		EditorMenuBar menuBar = new EditorMenuBar(null, tree);
		menuBar.setEnabled(true);
		menuBar.setEditable(true);
		
		assertFalse(commandItem(menuBar, EditorCommand.RENAME_KEY).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.DUPLICATE_KEY).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.DELETE_KEY).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.COPY_KEY).isEnabled());
		
		JsonTreeNode name = ((JsonTreeModel) tree.getModel()).getNodeByKey("/name");
		tree.setSelectionNode(name);
		
		assertTrue(commandItem(menuBar, EditorCommand.RENAME_KEY).isEnabled());
		assertTrue(commandItem(menuBar, EditorCommand.DUPLICATE_KEY).isEnabled());
		assertTrue(commandItem(menuBar, EditorCommand.DELETE_KEY).isEnabled());
		assertTrue(commandItem(menuBar, EditorCommand.COPY_KEY).isEnabled());
		
		tree.setSelectionPath(new TreePath(((JsonTreeNode) tree.getModel().getRoot()).getPath()));
		
		assertFalse(commandItem(menuBar, EditorCommand.RENAME_KEY).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.DUPLICATE_KEY).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.DELETE_KEY).isEnabled());
		assertFalse(commandItem(menuBar, EditorCommand.COPY_KEY).isEnabled());
	}
	
	private void assertCommand(EditorMenuBar menuBar, EditorCommand command) {
		JMenuItem item = commandItem(menuBar, command);
		assertEquals(command.name(), item.getClientProperty(EditorCommand.CLIENT_PROPERTY));
	}
	
	private JMenuItem commandItem(EditorMenuBar menuBar, EditorCommand command) {
		JMenuItem item = findMenuItem(menuBar, command.text());
		assertNotNull(item);
		return item;
	}
	
	private JMenuItem findMenuItem(EditorMenuBar menuBar, String text) {
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			JMenuItem item = findMenuItem(menuBar.getMenu(i), text);
			if (item != null) {
				return item;
			}
		}
		return null;
	}

	private JMenu findMenu(EditorMenuBar menuBar, String text) {
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			JMenu menu = menuBar.getMenu(i);
			if (menu != null && text.equals(menu.getText())) {
				return menu;
			}
		}
		return null;
	}
	
	private JMenuItem findMenuItem(JMenu menu, String text) {
		for (int i = 0; i < menu.getItemCount(); i++) {
			JMenuItem item = menu.getItem(i);
			if (item == null) {
				continue;
			}
			if (text.equals(item.getText())) {
				return item;
			}
			if (item instanceof JMenu nestedMenu) {
				JMenuItem nestedItem = findMenuItem(nestedMenu, text);
				if (nestedItem != null) {
					return nestedItem;
				}
			}
		}
		return null;
	}
}
