package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

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

	@Test
	public void editorVersionIsNotHardcodedInSource() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/skanga/jsoneditor/editor/Editor.java"));

		assertFalse(source.contains("VERSION = \""));
	}

	@Test
	public void readmeDoesNotHardcodeCurrentReleaseVersion() throws Exception {
		String readme = Files.readString(Path.of("README.md"));

		assertFalse(readme.contains("current release line"));
		assertFalse(Pattern.compile("json-editor-[0-9]+\\.[0-9]+\\.[0-9]+\\.jar").matcher(readme).find());
		assertFalse(readme.contains("json-editor-<version>.jar"));
		assertTrue(readme.contains("target/json-editor.jar"));
	}

	@Test
	public void releaseScriptDerivesTagAndArtifactFromMavenVersion() throws Exception {
		assertFalse(Files.exists(Path.of("scripts/release.ps1")));
		String script = Files.readString(Path.of("scripts/release.py"));

		assertFalse(script.contains("help:evaluate"));
		assertTrue(script.contains("project.version"));
		assertTrue(script.contains("ElementTree"));
		assertTrue(script.contains("f\"v{version}\""));
		assertFalse(script.contains("json-editor-{version}.jar"));
		assertTrue(script.contains("\"target\" / \"json-editor.jar\""));
		assertFalse(script.contains("shutil.copyfile"));
		assertFalse(script.contains("--notes-file"));
		assertFalse(script.contains("CHANGELOG.md"));
		assertTrue(script.contains("shutil.which"));
		assertTrue(script.contains("\"mvn.cmd\""));
		assertNotNull(Pattern.compile("version\\s*=").matcher(script).results().findFirst().orElse(null));
		assertFalse(Pattern.compile("0\\.1\\.1|v0\\.1\\.1").matcher(script).find());
	}
}
