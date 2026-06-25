package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.model.JsonDocument;
import com.skanga.jsoneditor.io.ChecksumException;
import com.skanga.jsoneditor.util.DuplicateJsonKey;
import com.skanga.jsoneditor.util.MessageBundle;


public class EditorSaveJsonDocumentTest {
	@Test
	public void confirmedChecksumRetryReturnsRetryFailure() {
		JsonDocument jsonDocument = new JsonDocument(null);
		jsonDocument.setChecksum("stale");
		AtomicInteger writes = new AtomicInteger();
		AtomicInteger errors = new AtomicInteger();
		
		boolean result = Editor.saveResource(jsonDocument, false, false, (r, pretty, flatten) -> {
			if (writes.incrementAndGet() == 1) {
				throw new ChecksumException("changed");
			}
			throw new IOException("retry failed");
		}, () -> true, e -> errors.incrementAndGet());
		
		assertFalse(result);
		assertEquals(2, writes.get());
		assertEquals(1, errors.get());
		assertNull(jsonDocument.getChecksum());
	}
	
	@Test
	public void rejectedChecksumConflictReturnsFalseWithoutRetry() {
		JsonDocument jsonDocument = new JsonDocument(null);
		jsonDocument.setChecksum("stale");
		AtomicInteger writes = new AtomicInteger();
		
		boolean result = Editor.saveResource(jsonDocument, false, false, (r, pretty, flatten) -> {
			writes.incrementAndGet();
			throw new IOException("changed");
		}, () -> false, e -> {});
		
		assertFalse(result);
		assertEquals(1, writes.get());
		assertEquals("stale", jsonDocument.getChecksum());
	}

	@Test
	public void duplicateKeyDocumentDeclinesSaveWithoutWriting() {
		JsonDocument jsonDocument = new JsonDocument(null);
		jsonDocument.setDuplicateKeys(List.of(new DuplicateJsonKey("role", "/role")));
		AtomicInteger writes = new AtomicInteger();
		
		boolean result = Editor.saveResource(jsonDocument, false, false, (r, pretty, flatten) -> {
			writes.incrementAndGet();
		}, () -> true, () -> false, e -> {});
		
		assertFalse(result);
		assertEquals(0, writes.get());
		assertTrue(jsonDocument.hasDuplicateKeys());
	}

	@Test
	public void duplicateKeyDocumentConfirmedSaveWritesAndClearsMetadata() {
		JsonDocument jsonDocument = new JsonDocument(null);
		jsonDocument.setDuplicateKeys(List.of(new DuplicateJsonKey("role", "/role")));
		AtomicInteger writes = new AtomicInteger();
		
		boolean result = Editor.saveResource(jsonDocument, false, false, (r, pretty, flatten) -> {
			writes.incrementAndGet();
		}, () -> true, () -> true, e -> {});
		
		assertTrue(result);
		assertEquals(1, writes.get());
		assertFalse(jsonDocument.hasDuplicateKeys());
	}

	@Test
	public void newJsonResourceStartsAsEditableRootObject() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = Editor.createNewJsonResource(Path.of("created.json"));

		assertEquals("object", jsonDocument.getEntry(""));

		JsonResourceMutator mutator = new JsonResourceMutator(jsonDocument);
		mutator.addObjectProperty("", "title");

		assertEquals(List.of("", "/title"), List.copyOf(jsonDocument.getEntries().keySet()));
		assertEquals("\"\"", jsonDocument.getEntry("/title"));
		assertSame(JsonNodeType.Object, new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("").getJsonType());
		assertSame(JsonNodeType.String, new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/title").getJsonType());
	}
}
