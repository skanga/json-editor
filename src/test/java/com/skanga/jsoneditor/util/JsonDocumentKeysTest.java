package com.skanga.jsoneditor.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class JsonDocumentKeysTest {

	@Test
	public void sizeTest() {
		assertEquals(1, ResourceKeys.size(""));
		assertEquals(1, ResourceKeys.size("/a"));
		assertEquals(2, ResourceKeys.size("/a/b"));
		assertEquals(3, ResourceKeys.size("/a/b/c"));
	}

	@Test
	public void partsTest() {
		assertArrayEquals(new String[]{"a"}, ResourceKeys.parts("/a"));
		assertArrayEquals(new String[]{"a","b"}, ResourceKeys.parts("/a/b"));
		assertArrayEquals(new String[]{"a.b"}, ResourceKeys.parts("/a.b"));
	}

	@Test
	public void rootKeyBehavior() {
		assertArrayEquals(new String[]{""}, ResourceKeys.parts(""));
		assertEquals("", ResourceKeys.firstPart(""));
		assertEquals("", ResourceKeys.lastPart(""));
		assertEquals("", ResourceKeys.withoutFirstPart(""));
		assertEquals("", ResourceKeys.withoutLastPart(""));
		assertEquals("", ResourceKeys.childKey("", ""));
		assertFalse(ResourceKeys.isChildKeyOf("", ""));
	}

	@Test
	public void jsonPathPartsPreserveLiteralDotsAndUnescapeSegments() {
		assertArrayEquals(new String[]{"a.b", "slash/key", "tilde~key"},
				ResourceKeys.parts("/a.b/slash~1key/tilde~0key"));
		assertEquals("a.b", ResourceKeys.firstPart("/a.b/slash~1key"));
		assertEquals("slash/key", ResourceKeys.lastPart("/a.b/slash~1key"));
	}

	@Test
	public void jsonPathParentChildOperationsPreserveEscaping() {
		assertEquals("/slash~1key", ResourceKeys.createJsonPath("", "slash/key"));
		assertEquals("/a.b/slash~1key", ResourceKeys.createJsonPath("/a.b", "slash/key"));
		assertEquals("/slash~1key/tilde~0key", ResourceKeys.withoutFirstPart("/a.b/slash~1key/tilde~0key"));
		assertEquals("/a.b/slash~1key", ResourceKeys.withoutLastPart("/a.b/slash~1key/tilde~0key"));
		assertEquals("/slash~1key/tilde~0key", ResourceKeys.childKey("/a.b/slash~1key/tilde~0key", "/a.b"));
		assertTrue(ResourceKeys.isChildKeyOf("/a.b/slash~1key", "/a.b"));
		assertFalse(ResourceKeys.isChildKeyOf("/a.b", "/a.b"));
	}

	@Test
	public void jsonPathRootExtractionPreservesSourceOrder() {
		List<String> keys = new ArrayList<>(List.of(
				"",
				"/z",
				"/a.b",
				"/a.b/slash~1key",
				"/z/0"));

		assertEquals(new ArrayList<>(List.of("z", "a.b")), ResourceKeys.uniqueRootKeys(keys));
		assertEquals(new ArrayList<>(List.of("/slash~1key")),
				ResourceKeys.extractChildKeys(keys, "a.b"));
	}
}
