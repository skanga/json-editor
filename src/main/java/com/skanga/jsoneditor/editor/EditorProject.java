package com.skanga.jsoneditor.editor;

import java.nio.file.Path;

import com.skanga.jsoneditor.model.JsonDocument;

/**
 * This class represents an editor project.
 */
public class EditorProject {
	private Path path;
	private JsonDocument jsonDocument;
	private boolean minifyResources;
	private boolean flattenJSON;

	public EditorProject(Path path) {
		this.path = path;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public JsonDocument getResource() {
		return jsonDocument;
	}

	public void setResource(JsonDocument jsonDocument) {
		this.jsonDocument = jsonDocument;
	}

	public boolean hasResource() {
		return jsonDocument != null;
	}
	
	public boolean isMinifyResources() {
		return minifyResources;
	}

	public void setMinifyResources(boolean minifyResources) {
		this.minifyResources = minifyResources;
	}

	public boolean isFlattenJSON() {
		return flattenJSON;
	}

	public void setFlattenJSON(boolean flattenJSON) {
		this.flattenJSON = flattenJSON;
	}
	
}

