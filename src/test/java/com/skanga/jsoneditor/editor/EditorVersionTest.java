package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EditorVersionTest {
	@Test
	public void newerVersionUsesNumericComponentComparison() {
		assertTrue(Editor.isNewerVersion("2.0.0", "10.0.0"));
		assertTrue(Editor.isNewerVersion("2.9.0", "2.10.0"));
		assertTrue(Editor.isNewerVersion("2.0.9", "2.0.10"));
	}
	
	@Test
	public void newerVersionHandlesPrefixAndPreRelease() {
		assertTrue(Editor.isNewerVersion("2.0.0-beta.1", "v2.0.0"));
		assertFalse(Editor.isNewerVersion("2.0.0", "2.0.0-beta.1"));
		assertFalse(Editor.isNewerVersion("2.0.0", "v2.0.0"));
	}
	
	@Test
	public void newerVersionFallsBackForNonSemanticVersions() {
		assertTrue(Editor.isNewerVersion("release-a", "release-b"));
		assertFalse(Editor.isNewerVersion("release-b", "release-a"));
	}
}
