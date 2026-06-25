package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.skanga.jsoneditor.util.MessageBundle;

public class EditorOpenSelectionTest {
	@TempDir
	Path tempDir;

	@Test
	public void openingExistingJsonFileSelectsRootNode() throws Exception {
		MessageBundle.loadResources();
		Path file = tempDir.resolve("opened.json");
		Files.writeString(file, "{\"name\":\"Ada\"}");
		Editor editor = setupEditor();
		try {
			editor.importJsonFile(file, true);

			assertRootSelected(editor);
		} finally {
			editor.dispose();
		}
	}

	@Test
	public void importingProjectJsonSelectsRootNode() throws Exception {
		MessageBundle.loadResources();
		Path file = tempDir.resolve("project.json");
		Files.writeString(file, "{\"name\":\"Ada\"}");
		Editor editor = setupEditor();
		try {
			editor.importProject(file, true);

			assertRootSelected(editor);
		} finally {
			editor.dispose();
		}
	}

	@Test
	public void openingJsonFileLazilyRecordsDuplicateKeys() throws Exception {
		MessageBundle.loadResources();
		Path file = tempDir.resolve("duplicates.json");
		Files.writeString(file, "{\"role\":\"user\",\"role\":\"admin\"}");
		Editor editor = setupEditor();
		try {
			editor.importJsonFile(file, true);

			waitForDuplicateKeyScan(editor);

			assertEquals("/role", editor.getProject().getResource().getDuplicateKeys().getFirst().path());
		} finally {
			editor.dispose();
		}
	}

	@Test
	public void creatingNewJsonFileStillSelectsRootNode() throws Exception {
		MessageBundle.loadResources();
		Path file = tempDir.resolve("created.json");
		Editor editor = setupEditor();
		try {
			editor.createJsonFile(file);

			assertRootSelected(editor);
		} finally {
			editor.dispose();
		}
	}

	private static Editor setupEditor() throws Exception {
		Editor editor = new Editor();
		EditorSettings settings = (EditorSettings) getField(editor, "settings");
		settings.setHistory(new ArrayList<>());
		var method = Editor.class.getDeclaredMethod("setupUI");
		method.setAccessible(true);
		method.invoke(editor);
		return editor;
	}

	private static void assertRootSelected(Editor editor) throws Exception {
		JsonTree tree = (JsonTree) getField(editor, "jsonTree");
		JsonTreeNode selected = tree.getSelectionNode();
		assertNotNull(selected);
		assertEquals("", selected.getKey());
	}

	private static void waitForDuplicateKeyScan(Editor editor) throws Exception {
		long deadline = System.currentTimeMillis() + 2_000;
		while (System.currentTimeMillis() < deadline) {
			SwingUtilities.invokeAndWait(() -> {});
			if (editor.getProject().getResource().hasDuplicateKeys()) {
				return;
			}
			Thread.sleep(25);
		}
		assertTrue(editor.getProject().getResource().hasDuplicateKeys());
	}

	private static Object getField(Editor editor, String name) throws Exception {
		Field field = Editor.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(editor);
	}
}
