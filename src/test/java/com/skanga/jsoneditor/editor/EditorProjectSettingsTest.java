package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import com.skanga.jsoneditor.model.JsonDocument;
import org.junit.jupiter.api.Test;

public class EditorProjectSettingsTest {
	@Test
	public void singleResourceProject() {
		EditorProject project = new EditorProject(Path.of("project"));
		JsonDocument jsonDocument = new JsonDocument(Path.of("config.json"));

		assertFalse(project.hasResource());

		project.setResource(jsonDocument);

		assertTrue(project.hasResource());
		assertEquals(jsonDocument, project.getResource());
	}

	@Test
	public void projectStoresConfigurationValues() {
		EditorProject project = new EditorProject(Path.of("project"));
		project.setPath(Path.of("other"));
		project.setMinifyResources(true);
		project.setFlattenJSON(true);

		assertEquals(Path.of("other"), project.getPath());
		assertTrue(project.isMinifyResources());
		assertTrue(project.isFlattenJSON());
	}

	@Test
	public void settingsStoreEditorStateValues() {
		EditorSettings settings = new EditorSettings();
		settings.setWindowPositionX(11);
		settings.setWindowPositionY(22);
		settings.setWindowWidth(800);
		settings.setWindowHeight(600);
		settings.setWindowDividerPosition(240);
		settings.setHistory(List.of("a", "b"));
		settings.setLastExpandedNodes(List.of("/a"));
		settings.setLastSelectedNode("/a/b");
		settings.setMinifyResources(true);
		settings.setFlattenJSON(true);
		settings.setCheckVersionOnStartup(true);
		settings.setDoubleClickTreeToggling(true);
		settings.setWrapLongTextValues(true);
		settings.setDarkTheme(true);
		settings.setEditorLanguage(Locale.ENGLISH);

		assertEquals(11, settings.getWindowPositionX());
		assertEquals(22, settings.getWindowPositionY());
		assertEquals(800, settings.getWindowWidth());
		assertEquals(600, settings.getWindowHeight());
		assertEquals(240, settings.getWindowDividerPosition());
		assertEquals(List.of("a", "b"), settings.getHistory());
		assertEquals(List.of("/a"), settings.getLastExpandedNodes());
		assertEquals("/a/b", settings.getLastSelectedNode());
		assertTrue(settings.isMinifyResources());
		assertTrue(settings.isFlattenJSON());
		assertTrue(settings.isCheckVersionOnStartup());
		assertTrue(settings.isDoubleClickTreeToggling());
		assertTrue(settings.isWrapLongTextValues());
		assertTrue(settings.isDarkTheme());
		assertEquals(Locale.ENGLISH, settings.getEditorLanguage());
	}
}
