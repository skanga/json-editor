package com.skanga.jsoneditor.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonDocumentTest {
	private JsonDocument jsonDocument;

	@BeforeEach
	public void setup() {
		jsonDocument = new JsonDocument(null);
		jsonDocument.setEntries(new LinkedHashMap<>(Map.of(
				"", "object",
				"/users", "array",
				"/users/0", "object",
				"/users/0/name", "\"Ada\"")));
	}

	@Test
	public void storeEntryUpdatesValue() {
		jsonDocument.storeEntry("/users/0/name", "\"Grace\"");
		assertEquals("\"Grace\"", jsonDocument.getEntry("/users/0/name"));
	}

	@Test
	public void storeEntryWithPrimitiveRemovesChildren() {
		jsonDocument.storeEntry("/users/0", "\"text\"");
		assertEquals("\"text\"", jsonDocument.getEntry("/users/0"));
		assertNull(jsonDocument.getEntry("/users/0/name"));
	}

	@Test
	public void storeEntryWithContainerPreservesChildren() {
		jsonDocument.storeEntry("/users/0", "object");
		assertEquals("object", jsonDocument.getEntry("/users/0"));
		assertEquals("\"Ada\"", jsonDocument.getEntry("/users/0/name"));
	}

	@Test
	public void storeEntryWithInvalidKeyThrows() {
		assertThrows(IllegalArgumentException.class, () -> jsonDocument.storeEntry("not-a-path", "value"));
	}

	@Test
	public void removeEntryDeletesKeyAndChildren() {
		jsonDocument.removeEntry("/users/0");
		assertNull(jsonDocument.getEntry("/users/0"));
		assertNull(jsonDocument.getEntry("/users/0/name"));
		assertEquals("array", jsonDocument.getEntry("/users"));
	}

	@Test
	public void renameEntryMovesKeyAndChildren() {
		jsonDocument.renameEntry("/users/0", "/users/1");
		assertNull(jsonDocument.getEntry("/users/0"));
		assertNull(jsonDocument.getEntry("/users/0/name"));
		assertEquals("object", jsonDocument.getEntry("/users/1"));
		assertEquals("\"Ada\"", jsonDocument.getEntry("/users/1/name"));
	}

	@Test
	public void renameJsonPathEntryPreservesJsonPathDescendants() {
		jsonDocument.setEntries(Map.of(
				"", "object",
				"/users", "array",
				"/users/0", "object",
				"/users/0/name", "\"Ada\"",
				"/users/0/slash~1key", "\"value\""));

		jsonDocument.renameEntry("/users/0", "/users/1");

		Map<String,String> entries = jsonDocument.getEntries();
		assertNull(entries.get("/users/0"));
		assertNull(entries.get("/users/0/name"));
		assertEquals("object", entries.get("/users/1"));
		assertEquals("\"Ada\"", entries.get("/users/1/name"));
		assertEquals("\"value\"", entries.get("/users/1/slash~1key"));
		assertFalse(entries.containsKey("/users/1./name"));
	}

	@Test
	public void renameEntryWithInvalidKeyThrows() {
		assertThrows(IllegalArgumentException.class, () -> jsonDocument.renameEntry("/users/0", "bad-key"));
	}

	@Test
	public void duplicateEntryCopiesKeyAndChildren() {
		jsonDocument.duplicateEntry("/users/0", "/users/1");
		assertEquals("object", jsonDocument.getEntry("/users/0"));
		assertEquals("\"Ada\"", jsonDocument.getEntry("/users/0/name"));
		assertEquals("object", jsonDocument.getEntry("/users/1"));
		assertEquals("\"Ada\"", jsonDocument.getEntry("/users/1/name"));
	}

	@Test
	public void duplicateJsonPathEntryPreservesJsonPathDescendants() {
		jsonDocument.setEntries(Map.of(
				"", "object",
				"/users", "array",
				"/users/0", "object",
				"/users/0/name", "\"Ada\"",
				"/users/0/slash~1key", "\"value\""));

		jsonDocument.duplicateEntry("/users/0", "/users/1");

		Map<String,String> entries = jsonDocument.getEntries();
		assertEquals("object", entries.get("/users/0"));
		assertEquals("\"Ada\"", entries.get("/users/0/name"));
		assertEquals("object", entries.get("/users/1"));
		assertEquals("\"Ada\"", entries.get("/users/1/name"));
		assertEquals("\"value\"", entries.get("/users/1/slash~1key"));
		assertFalse(entries.containsKey("/users/1./name"));
	}

	@Test
	public void duplicateEntryWithInvalidKeyThrows() {
		assertThrows(IllegalArgumentException.class, () -> jsonDocument.duplicateEntry("/users/0", "bad-key"));
	}

	@Test
	public void duplicateEntryNotifiesListenersOnce() {
		Map<String,String> entries = new LinkedHashMap<>();
		for (int i = 0; i < 50; i++) {
			entries.put("/source/" + i, "\"value" + i + "\"");
		}
		jsonDocument.setEntries(entries);
		AtomicInteger events = new AtomicInteger();
		jsonDocument.addListener(e -> events.incrementAndGet());

		jsonDocument.duplicateEntry("/source", "/copy");

		assertEquals(1, events.get());
		assertEquals("\"value49\"", jsonDocument.getEntry("/copy/49"));
	}
	
	@Test
	public void listenerReceivesChangedDocument() {
		AtomicReference<JsonDocument> changedDocument = new AtomicReference<>();
		jsonDocument.addListener(changedDocument::set);

		jsonDocument.storeEntry("/users/0/name", "\"Grace\"");

		assertSame(jsonDocument, changedDocument.get());
	}

	@Test
	public void duplicateEntryCopiesNestedSubtreeUnderNewJsonPath() {
		JsonDocument r = new JsonDocument(null);
		LinkedHashMap<String,String> entries = new LinkedHashMap<>();
		entries.put("", "object");
		entries.put("/group", "object");
		entries.put("/group/name", "\"Ada\"");
		r.setEntries(entries);

		r.duplicateEntry("/group", "/copy");

		assertEquals("object", r.getEntry("/copy"));
		assertEquals("\"Ada\"", r.getEntry("/copy/name"));
		assertEquals("\"Ada\"", r.getEntry("/group/name"));
	}

	@Test
	public void renameEntryMovesNestedSubtree() {
		JsonDocument r = new JsonDocument(null);
		LinkedHashMap<String,String> entries = new LinkedHashMap<>();
		entries.put("", "object");
		entries.put("/group", "object");
		entries.put("/group/name", "\"Ada\"");
		r.setEntries(entries);

		r.renameEntry("/group", "/renamed");

		assertEquals("object", r.getEntry("/renamed"));
		assertEquals("\"Ada\"", r.getEntry("/renamed/name"));
		assertEquals(null, r.getEntry("/group"));
	}
}
