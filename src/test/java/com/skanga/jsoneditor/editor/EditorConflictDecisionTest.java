package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.MessageBundle;

public class EditorConflictDecisionTest {
	@Test
	public void conflictWithMissingOldNodeDoesNotThrowAndUsesNewNodeShape() {
		MessageBundle.loadResources();
		java.util.LinkedHashMap<String,String> entries = new java.util.LinkedHashMap<>();
		entries.put("", "object");
		entries.put("/existing", "\"value\"");
		entries.put("/group", "object");
		entries.put("/group/child", "\"value\"");
		JsonTreeModel model = new JsonTreeModel(entries);

		assertTrue(Editor.isReplaceConflict(null, model.getNodeByKey("/existing")));
		assertFalse(Editor.isReplaceConflict(null, model.getNodeByKey("/group")));
	}

	@Test
	public void conflictWithExistingOldLeafIsReplace() {
		MessageBundle.loadResources();
		java.util.LinkedHashMap<String,String> entries = new java.util.LinkedHashMap<>();
		entries.put("", "object");
		entries.put("/old", "\"value\"");
		entries.put("/group", "object");
		entries.put("/group/child", "\"value\"");
		JsonTreeModel model = new JsonTreeModel(entries);

		assertTrue(Editor.isReplaceConflict(model.getNodeByKey("/old"), model.getNodeByKey("/group")));
	}
}
