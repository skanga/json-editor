package com.skanga.jsoneditor.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.DuplicateJsonKey;

public class DuplicateJsonKeyDetectorTest {
	@Test
	public void noDuplicatesReturnsEmptyList() throws Exception {
		assertTrue(DuplicateJsonKeyDetector.findDuplicates("{\"role\":\"user\",\"enabled\":true}").isEmpty());
	}

	@Test
	public void duplicateAtRootReportsJsonPath() throws Exception {
		List<DuplicateJsonKey> duplicates = DuplicateJsonKeyDetector.findDuplicates(
				"{\"role\":\"user\",\"role\":\"admin\"}");

		assertEquals(List.of(new DuplicateJsonKey("role", "/role")), duplicates);
	}

	@Test
	public void duplicateNestedObjectReportsNestedJsonPath() throws Exception {
		List<DuplicateJsonKey> duplicates = DuplicateJsonKeyDetector.findDuplicates(
				"{\"user\":{\"role\":\"user\",\"role\":\"admin\"}}");

		assertEquals(List.of(new DuplicateJsonKey("role", "/user/role")), duplicates);
	}

	@Test
	public void sameKeyNameInDifferentObjectsIsAllowed() throws Exception {
		assertTrue(DuplicateJsonKeyDetector.findDuplicates(
				"{\"user\":{\"role\":\"user\"},\"admin\":{\"role\":\"admin\"}}").isEmpty());
	}

	@Test
	public void duplicateInsideArrayObjectReportsArrayJsonPath() throws Exception {
		List<DuplicateJsonKey> duplicates = DuplicateJsonKeyDetector.findDuplicates(
				"{\"items\":[{\"role\":\"user\",\"role\":\"admin\"}]}");

		assertEquals(List.of(new DuplicateJsonKey("role", "/items/0/role")), duplicates);
	}
}
