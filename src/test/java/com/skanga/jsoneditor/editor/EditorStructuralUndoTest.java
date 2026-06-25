package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;

import com.skanga.jsoneditor.model.JsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Headless-safe test for the snapshot-based structural undo logic. The {@code Editor}
 * pushes an {@link AbstractUndoableEdit} that calls {@link JsonDocument#replaceEntries(Map)}
 * with the before/after snapshots; this exercises that same round-trip directly on a
 * {@link JsonDocument} so it does not require a full Swing frame.
 */
public class EditorStructuralUndoTest {
	private JsonDocument jsonDocument;
	private UndoManager undoManager;

	@BeforeEach
	public void setup() {
		jsonDocument = new JsonDocument(null);
		jsonDocument.setEntries(orderedMap(
				"", "object",
				"/z", "1",
				"/settings", "object",
				"/settings/a", "\"x\""));
		undoManager = new UndoManager();
	}

	@Test
	public void undoRestoresPriorEntriesAndRedoReappliesMutation() {
		Map<String,String> before = new LinkedHashMap<>(jsonDocument.getEntries());

		// Perform a structural mutation via the same mutator path the editor uses.
		new JsonResourceMutator(jsonDocument).addObjectProperty("/settings", "c");

		Map<String,String> after = new LinkedHashMap<>(jsonDocument.getEntries());
		undoManager.addEdit(new SnapshotEdit(jsonDocument, before, after));

		// The mutation added a new property.
		assertEquals(List.of("", "/z", "/settings", "/settings/a", "/settings/c"),
				List.copyOf(jsonDocument.getEntries().keySet()));
		assertTrue(undoManager.canUndo());
		assertFalse(undoManager.canRedo());

		// Undo restores the prior entries exactly (keys and values, including order).
		undoManager.undo();
		assertEquals(before, jsonDocument.getEntries());
		assertEquals(List.copyOf(before.keySet()), List.copyOf(jsonDocument.getEntries().keySet()));
		assertFalse(undoManager.canUndo());
		assertTrue(undoManager.canRedo());

		// Redo re-applies the mutation.
		undoManager.redo();
		assertEquals(after, jsonDocument.getEntries());
		assertEquals("\"\"", jsonDocument.getEntry("/settings/c"));
	}

	/** Mirrors Editor.StructuralEdit: undo/redo swap deep-copied entry snapshots. */
	private static class SnapshotEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;
		private final JsonDocument jsonDocument;
		private final Map<String,String> before;
		private final Map<String,String> after;

		SnapshotEdit(JsonDocument jsonDocument, Map<String,String> before, Map<String,String> after) {
			this.jsonDocument = jsonDocument;
			this.before = before;
			this.after = after;
		}

		@Override
		public void undo() {
			super.undo();
			jsonDocument.replaceEntries(new LinkedHashMap<>(before));
		}

		@Override
		public void redo() {
			super.redo();
			jsonDocument.replaceEntries(new LinkedHashMap<>(after));
		}
	}

	private static Map<String,String> orderedMap(String... pairs) {
		Map<String,String> result = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			result.put(pairs[i], pairs[i + 1]);
		}
		return result;
	}
}
