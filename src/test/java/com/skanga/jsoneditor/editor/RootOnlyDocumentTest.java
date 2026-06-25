package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.model.JsonDocument;
import com.skanga.jsoneditor.util.MessageBundle;
import com.skanga.jsoneditor.io.JsonIO;

public class RootOnlyDocumentTest {

	@TempDir
	public Path tmp;

	private String roundTrip(String json) throws Exception {
		Path file = tmp.resolve("doc.json");
		Files.writeString(file, json, StandardCharsets.UTF_8);
		JsonDocument jsonDocument = JsonIO.open(file);
		JsonIO.write(jsonDocument, false, false);
		return Files.readString(file, StandardCharsets.UTF_8).trim();
	}

	@Test
	public void emptyObjectRoundTrips() throws Exception {
		assertEquals("{}", roundTrip("{}"));
	}

	@Test
	public void emptyArrayRoundTrips() throws Exception {
		assertEquals("[]", roundTrip("[]"));
	}

	@Test
	public void rootPrimitiveRoundTrips() throws Exception {
		assertEquals("42", roundTrip("42"));
	}

	@Test
	public void rootOnlyObjectTreeHasOnlyRootNodeKeyedAsEmpty() {
		MessageBundle.loadResources();
		JsonTreeModel model = new JsonTreeModel(java.util.Map.of("", "object"));
		assertEquals("", model.getNodeByKey("").getKey());
		assertEquals(1, model.getNodeCount());
	}

	@Test
	public void nestedTreeKeysAreJsonPaths() {
		MessageBundle.loadResources();
		java.util.LinkedHashMap<String,String> entries = new java.util.LinkedHashMap<>();
		entries.put("", "object");
		entries.put("/group", "object");
		entries.put("/group/name", "\"Ada\"");
		JsonTreeModel model = new JsonTreeModel(entries);
		assertEquals("/group", model.getNodeByKey("/group").getKey());
		assertEquals("/group/name", model.getNodeByKey("/group/name").getKey());
	}
}
