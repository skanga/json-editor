package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.skanga.jsoneditor.model.JsonDocument;
import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.MessageBundle;

public class EditorArrayItemTest {

	@Test
	public void removeSelectedArrayItemReindexesRemainingItems() throws Exception {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = new JsonDocument(null);
		Map<String,String> entries = new LinkedHashMap<>();
		entries.put("", "object");
		entries.put("/items", "array");
		entries.put("/items/0", "\"a\"");
		entries.put("/items/1", "\"b\"");
		entries.put("/items/2", "\"c\"");
		jsonDocument.setEntries(entries);
		EditorProject project = new EditorProject(Path.of("items.json"));
		project.setResource(jsonDocument);
		Editor editor = new Editor();
		invokeSetupUI(editor);
		JsonTree tree = (JsonTree) getField(editor, "jsonTree");
		tree.setModel(new JsonTreeModel(jsonDocument.getEntries()));
		tree.setSelectionNode(tree.getNodeByKey("/items/1"));
		setField(editor, "project", project);
		AtomicReference<JsonTreeNode> confirmedNode = new AtomicReference<>();
		editor.setDeleteConfirmation(node -> {
			confirmedNode.set(node);
			return true;
		});

		editor.removeSelectedKey();

		assertEquals("/items/1", confirmedNode.get().getKey());
		assertEquals(List.of("", "/items", "/items/0", "/items/1"), List.copyOf(jsonDocument.getEntries().keySet()));
		assertEquals("\"a\"", jsonDocument.getEntry("/items/0"));
		assertEquals("\"c\"", jsonDocument.getEntry("/items/1"));
	}

	@Test
	public void cancelledDeleteLeavesSelectedPropertyUntouched() throws Exception {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = new JsonDocument(null);
		Map<String,String> entries = new LinkedHashMap<>();
		entries.put("", "object");
		entries.put("/name", "\"Ada\"");
		entries.put("/settings", "object");
		entries.put("/settings/enabled", "true");
		jsonDocument.setEntries(entries);
		EditorProject project = new EditorProject(Path.of("items.json"));
		project.setResource(jsonDocument);
		Editor editor = new Editor();
		invokeSetupUI(editor);
		JsonTree tree = (JsonTree) getField(editor, "jsonTree");
		tree.setModel(new JsonTreeModel(jsonDocument.getEntries()));
		tree.setSelectionNode(tree.getNodeByKey("/settings"));
		setField(editor, "project", project);
		AtomicReference<JsonTreeNode> confirmedNode = new AtomicReference<>();
		editor.setDeleteConfirmation(node -> {
			confirmedNode.set(node);
			return false;
		});

		editor.removeSelectedKey();

		assertSame(tree.getNodeByKey("/settings"), confirmedNode.get());
		assertEquals(List.of("", "/name", "/settings", "/settings/enabled"),
				List.copyOf(jsonDocument.getEntries().keySet()));
		assertSame(tree.getNodeByKey("/settings"), tree.getSelectionNode());
	}

	private static void invokeSetupUI(Editor editor) throws Exception {
		var method = Editor.class.getDeclaredMethod("setupUI");
		method.setAccessible(true);
		method.invoke(editor);
	}

	private static Object getField(Editor editor, String name) throws Exception {
		Field field = Editor.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(editor);
	}

	private static void setField(Editor editor, String name, Object value) throws Exception {
		Field field = Editor.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(editor, value);
	}
}
