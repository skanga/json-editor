package com.skanga.jsoneditor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.DuplicateJsonKey;

public class JsonDocumentDuplicateKeysTest {
	@Test
	public void duplicateKeyMetadataDefaultsToEmpty() {
		JsonDocument jsonDocument = new JsonDocument(null);

		assertTrue(jsonDocument.getDuplicateKeys().isEmpty());
		assertTrue(!jsonDocument.hasDuplicateKeys());
		assertTrue(!jsonDocument.isDuplicateKeyScanPending());
	}

	@Test
	public void duplicateKeyMetadataIsStoredAsImmutableCopy() {
		JsonDocument jsonDocument = new JsonDocument(null);
		List<DuplicateJsonKey> duplicates = new java.util.ArrayList<>(
				List.of(new DuplicateJsonKey("role", "/role")));

		jsonDocument.setDuplicateKeys(duplicates);
		duplicates.clear();

		assertEquals(List.of(new DuplicateJsonKey("role", "/role")), jsonDocument.getDuplicateKeys());
		assertTrue(jsonDocument.hasDuplicateKeys());
		assertThrows(UnsupportedOperationException.class,
				() -> jsonDocument.getDuplicateKeys().add(new DuplicateJsonKey("id", "/id")));
	}

	@Test
	public void clearingDuplicateKeyMetadataMarksDocumentSafe() {
		JsonDocument jsonDocument = new JsonDocument(null);
		jsonDocument.setDuplicateKeys(List.of(new DuplicateJsonKey("role", "/role")));
		jsonDocument.setDuplicateKeyScanPending(true);

		jsonDocument.clearDuplicateKeys();

		assertTrue(jsonDocument.getDuplicateKeys().isEmpty());
		assertTrue(!jsonDocument.hasDuplicateKeys());
		assertTrue(!jsonDocument.isDuplicateKeyScanPending());
	}

	@Test
	public void duplicateKeyScanPendingStateIsStored() {
		JsonDocument jsonDocument = new JsonDocument(null);

		jsonDocument.setDuplicateKeyScanPending(true);

		assertTrue(jsonDocument.isDuplicateKeyScanPending());
	}
}
