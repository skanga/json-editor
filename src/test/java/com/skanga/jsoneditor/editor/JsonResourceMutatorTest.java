package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.skanga.jsoneditor.model.JsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

public class JsonResourceMutatorTest {
	private JsonDocument jsonDocument;
	private JsonResourceMutator mutator;
	
	@BeforeEach
	public void setup() {
		jsonDocument = new JsonDocument(null);
		jsonDocument.setEntries(orderedMap(
				"", "object",
				"/z", "1",
				"/settings", "object",
				"/settings/b", "2",
				"/settings/a", "\"x\"",
				"/items", "array",
				"/items/0", "\"first\"",
				"/items/1", "object",
				"/items/1/name", "\"Ada\""));
		mutator = new JsonResourceMutator(jsonDocument);
	}
	
	@Test
	public void addRenameAndMoveObjectPropertiesPreservesOrder() {
		mutator.addObjectProperty("/settings", "c");
		mutator.renameObjectProperty("/settings/a", "alpha");
		mutator.moveObjectProperty("/settings/c", -1);
		
		assertEquals(List.of("", "/z", "/settings", "/settings/b", "/settings/c", "/settings/alpha",
				"/items", "/items/0", "/items/1", "/items/1/name"),
				List.copyOf(jsonDocument.getEntries().keySet()));
		assertEquals("\"\"", jsonDocument.getEntry("/settings/c"));
		assertEquals("\"x\"", jsonDocument.getEntry("/settings/alpha"));
	}
	
	@Test
	public void arrayInsertDeleteMoveAndDuplicateReindexesDescendants() {
		mutator.insertArrayItem("/items", 1);
		mutator.duplicateArrayItem("/items/2");
		mutator.moveArrayItem("/items/3", -1);
		mutator.deleteArrayItem("/items/0");
		
		Map<String,String> values = jsonDocument.getEntries();
		assertFalse(values.containsKey("/items/3"));
		assertEquals("\"\"", values.get("/items/0"));
		assertEquals("object", values.get("/items/1"));
		assertEquals("\"Ada\"", values.get("/items/1/name"));
		assertEquals("object", values.get("/items/2"));
		assertEquals("\"Ada\"", values.get("/items/2/name"));
	}

	@Test
	public void moveObjectPropertyAtBoundariesIsNoOp() {
		assertDoesNotThrow(() -> mutator.moveObjectProperty("/settings/b", -1));
		assertEquals(List.of("", "/z", "/settings", "/settings/b", "/settings/a",
				"/items", "/items/0", "/items/1", "/items/1/name"),
				List.copyOf(jsonDocument.getEntries().keySet()));

		assertDoesNotThrow(() -> mutator.moveObjectProperty("/settings/a", 1));
		assertEquals(List.of("", "/z", "/settings", "/settings/b", "/settings/a",
				"/items", "/items/0", "/items/1", "/items/1/name"),
				List.copyOf(jsonDocument.getEntries().keySet()));
	}

	@Test
	public void moveArrayItemAtBoundariesIsNoOp() {
		assertDoesNotThrow(() -> mutator.moveArrayItem("/items/0", -1));
		assertEquals(List.of("", "/z", "/settings", "/settings/b", "/settings/a",
				"/items", "/items/0", "/items/1", "/items/1/name"),
				List.copyOf(jsonDocument.getEntries().keySet()));

		assertDoesNotThrow(() -> mutator.moveArrayItem("/items/1", 1));
		assertEquals(List.of("", "/z", "/settings", "/settings/b", "/settings/a",
				"/items", "/items/0", "/items/1", "/items/1/name"),
				List.copyOf(jsonDocument.getEntries().keySet()));
	}
	
	@Test
	public void changingContainerToPrimitiveRemovesDescendants() {
		mutator.changeType("/settings", JsonNodeType.Boolean);
		
		Map<String,String> values = jsonDocument.getEntries();
		assertEquals("false", values.get("/settings"));
		assertFalse(values.containsKey("/settings/a"));
		assertFalse(values.containsKey("/settings/b"));
	}
	
	@Test
	public void changingPrimitiveToContainerCreatesEmptySentinel() {
		mutator.changeType("/z", JsonNodeType.Array);
		
		Map<String,String> values = jsonDocument.getEntries();
		assertEquals("array", values.get("/z"));
		assertTrue(values.containsKey("/z"));
	}
	
	@Test
	public void rawObjectPasteReplacesSubtreeWithFlattenedEntries() {
		mutator.setNodeLiteral("/settings", "{\"next\":[1,2]}");
		
		assertEquals(List.of("", "/z", "/settings", "/settings/next", "/settings/next/0",
				"/settings/next/1", "/items", "/items/0", "/items/1", "/items/1/name"),
				List.copyOf(jsonDocument.getEntries().keySet()));
		assertEquals("array", jsonDocument.getEntry("/settings/next"));
		assertEquals("2", jsonDocument.getEntry("/settings/next/1"));
	}
	
	private static Map<String,String> orderedMap(String... pairs) {
		Map<String,String> result = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			result.put(pairs[i], pairs[i + 1]);
		}
		return result;
	}
}
