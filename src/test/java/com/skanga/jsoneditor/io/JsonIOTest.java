package com.skanga.jsoneditor.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.skanga.jsoneditor.model.JsonDocument;
import com.skanga.jsoneditor.util.JsonLoadException;
import com.skanga.jsoneditor.util.JsonParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class JsonIOTest {
	private final static Gson gson = new Gson();
	
	@TempDir
	public Path tmp;
	
	@Test
	public void loadJsonKeepsGenericJsonLiteralValues() throws Exception {
		Path file = tmp.resolve("data.json");
		Files.writeString(file, "{\"name\":\"Ada\",\"enabled\":true,\"count\":3,\"tags\":[\"a\",\"b\"],\"empty\":null}",
				StandardCharsets.UTF_8);
		
		JsonDocument jsonDocument = new JsonDocument(file);
		JsonIO.load(jsonDocument);
		
		Map<String,String> values = jsonDocument.getEntries();
		assertEquals("\"Ada\"", values.get("/name"));
		assertEquals("true", values.get("/enabled"));
		assertEquals("3", values.get("/count"));
		assertEquals("array", values.get("/tags"));
		assertEquals("\"a\"", values.get("/tags/0"));
		assertEquals("\"b\"", values.get("/tags/1"));
		assertEquals("null", values.get("/empty"));
	}
	
	@Test
	public void loadJsonRepresentsEveryJsonNodeWithUnambiguousPaths() throws Exception {
		Path file = tmp.resolve("tree.json");
		Files.writeString(file, "{\"a.b\":{\"emptyObject\":{},\"emptyArray\":[],\"items\":[true,{\"x\":1}]}}",
				StandardCharsets.UTF_8);
		
		JsonDocument jsonDocument = new JsonDocument(file);
		JsonIO.load(jsonDocument);
		
		Map<String,String> values = jsonDocument.getEntries();
		assertEquals("object", values.get(""));
		assertEquals("object", values.get("/a.b"));
		assertEquals("object", values.get("/a.b/emptyObject"));
		assertEquals("array", values.get("/a.b/emptyArray"));
		assertEquals("array", values.get("/a.b/items"));
		assertEquals("true", values.get("/a.b/items/0"));
		assertEquals("object", values.get("/a.b/items/1"));
		assertEquals("1", values.get("/a.b/items/1/x"));
	}
	
	@Test
	public void rootPrimitiveJsonRoundTrips() throws Exception {
		Path file = tmp.resolve("root.json");
		Files.writeString(file, "true", StandardCharsets.UTF_8);
		
		JsonDocument jsonDocument = new JsonDocument(file);
		JsonIO.load(jsonDocument);
		assertEquals("true", jsonDocument.getEntries().get(""));
		
		JsonIO.write(jsonDocument, false, false);
		assertEquals("true", Files.readString(file, StandardCharsets.UTF_8).trim());
	}
	
	@Test
	public void complexJsonTreeRoundTripsWithoutLosingNodes() throws Exception {
		String json = "{\"a.b\":{\"emptyObject\":{},\"emptyArray\":[],\"items\":[true,{\"x/y\":1}]}}";
		Path file = tmp.resolve("roundtrip.json");
		Files.writeString(file, json, StandardCharsets.UTF_8);
		
		JsonDocument jsonDocument = new JsonDocument(file);
		JsonIO.load(jsonDocument);
		JsonIO.write(jsonDocument, false, false);
		
		assertEquals(parseJson(json), parseJson(Files.readString(file, StandardCharsets.UTF_8)));
	}
	
	@Test
	public void jsonObjectOrderIsPreservedOnLoadAndSave() throws Exception {
		String json = "{\"z\":1,\"a\":2,\"nested\":{\"b\":3,\"a\":4},\"arr\":[{\"y\":5,\"x\":6}]}";
		Path file = tmp.resolve("ordered.json");
		Files.writeString(file, json, StandardCharsets.UTF_8);
		
		JsonDocument jsonDocument = new JsonDocument(file);
		JsonIO.load(jsonDocument);
		
		assertEquals(List.of("", "/z", "/a", "/nested", "/nested/b", "/nested/a", "/arr", "/arr/0", "/arr/0/y", "/arr/0/x"),
				List.copyOf(jsonDocument.getEntries().keySet()));
		
		JsonIO.write(jsonDocument, false, false);
		assertEquals(json, Files.readString(file, StandardCharsets.UTF_8).trim());
	}
	
	@Test
	public void oneMegabyteJsonFileRoundTrips() throws Exception {
		StringBuilder json = new StringBuilder("{\"items\":[");
		for (int i = 0; i < 18000; i++) {
			if (i > 0) {
				json.append(',');
			}
			json.append("{\"id\":").append(i)
					.append(",\"name\":\"item-").append(i)
					.append("\",\"enabled\":").append(i % 2 == 0)
					.append('}');
		}
		json.append("]}");
		Path file = tmp.resolve("large.json");
		Files.writeString(file, json, StandardCharsets.UTF_8);
		
		JsonDocument jsonDocument = new JsonDocument(file);
		JsonIO.load(jsonDocument);
		JsonIO.write(jsonDocument, false, false);
		
		assertEquals(parseJson(json.toString()), parseJson(Files.readString(file, StandardCharsets.UTF_8)));
	}
	
	@Test
	public void writeJsonParsesGenericJsonLiteralValues() throws Exception {
		Path file = tmp.resolve("data.json");
		Files.writeString(file, "{}", StandardCharsets.UTF_8);
		
		JsonDocument jsonDocument = new JsonDocument(file);
		jsonDocument.storeEntry("/name", "\"Ada\"");
		jsonDocument.storeEntry("/enabled", "true");
		jsonDocument.storeEntry("/count", "3");
		jsonDocument.storeEntry("/tags", "[\"a\",\"b\"]");
		JsonIO.write(jsonDocument, false, false);
		
		assertEquals("{\"name\":\"Ada\",\"enabled\":true,\"count\":3,\"tags\":[\"a\",\"b\"]}",
				Files.readString(file, StandardCharsets.UTF_8).trim());
	}
	
	@Test
	public void writeJsonPathKeysProducesCorrectJson() throws Exception {
		Path file = tmp.resolve("flat.json");
		Files.writeString(file, "{}", StandardCharsets.UTF_8);

		java.util.LinkedHashMap<String,String> entries = new java.util.LinkedHashMap<>();
		entries.put("", "object");
		entries.put("/count", "3");
		entries.put("/enabled", "true");
		JsonDocument jsonDocument = new JsonDocument(file);
		jsonDocument.setEntries(entries);
		JsonIO.write(jsonDocument, false, false);

		assertEquals("{\"count\":3,\"enabled\":true}", Files.readString(file, StandardCharsets.UTF_8).trim());
	}

	@Test
	public void writesEmptyEntriesAsEmptyObject() throws Exception {
		Path file = tmp.resolve("empty.json");
		JsonDocument jsonDocument = new JsonDocument(file);
		jsonDocument.setEntries(Map.of());

		JsonIO.write(jsonDocument, false, false);

		assertEquals("{}", Files.readString(file, StandardCharsets.UTF_8).trim());
	}

	@Test
	public void writesEmptyEntriesAsEmptyObjectWhenFlattening() throws Exception {
		Path file = tmp.resolve("empty-flat.json");
		JsonDocument jsonDocument = new JsonDocument(file);
		jsonDocument.setEntries(Map.of());

		JsonIO.write(jsonDocument, false, true);

		assertEquals("{}", Files.readString(file, StandardCharsets.UTF_8).trim());
	}

	@Test
	public void flattenJsonPathKeysSkipsContainerSentinels() throws Exception {
		Path file = tmp.resolve("flatten.json");
		String json = "{\"a\":1,\"nested\":{\"b\":2},\"items\":[\"x\"]}";
		Files.writeString(file, json, StandardCharsets.UTF_8);
		JsonDocument jsonDocument = JsonIO.open(file);

		JsonIO.write(jsonDocument, false, true);

		assertEquals("{\"/a\":1,\"/nested/b\":2,\"/items/0\":\"x\"}",
				Files.readString(file, StandardCharsets.UTF_8).trim());
	}

	@Test
	public void flattenJsonPathKeysWritesEmptyLeafAsNull() throws Exception {
		Path file = tmp.resolve("flatten-empty.json");
		Files.writeString(file, "{}", StandardCharsets.UTF_8);
		JsonDocument jsonDocument = new JsonDocument(file);
		jsonDocument.storeEntry("", "object");
		jsonDocument.storeEntry("/added", "");

		assertEquals("", jsonDocument.getEntries().get("/added"));

		JsonIO.write(jsonDocument, false, true);

		assertEquals("{\"/added\":null}", Files.readString(file, StandardCharsets.UTF_8).trim());
	}

	@Test
	public void malformedJsonLoadThrowsIOException() throws Exception {
		Path file = tmp.resolve("malformed.json");
		Files.writeString(file, "{bad", StandardCharsets.UTF_8);
		JsonDocument jsonDocument = new JsonDocument(file);

		assertThrows(IOException.class, () -> JsonIO.load(jsonDocument));
	}

	@Test
	public void emptyJsonLoadThrowsIOException() throws Exception {
		Path file = tmp.resolve("empty.json");
		Files.writeString(file, "", StandardCharsets.UTF_8);
		JsonDocument jsonDocument = new JsonDocument(file);

		assertThrows(IOException.class, () -> JsonIO.load(jsonDocument));
	}
	
	@Test
	public void writeProducesExactBytesWithoutTrailingNewline() throws Exception {
		Path file = tmp.resolve("data.json");
		Files.writeString(file, "{}", StandardCharsets.UTF_8);
		JsonDocument jsonDocument = JsonIO.open(file);
		jsonDocument.storeEntry("/x", "1");
		JsonIO.write(jsonDocument, false, false);
		assertEquals("{\"x\":1}", Files.readString(file, StandardCharsets.UTF_8));
	}

	@Test
	public void writeLeavesNoTempFilesOnSuccess() throws Exception {
		Path file = tmp.resolve("data.json");
		Files.writeString(file, "{\"x\":1}", StandardCharsets.UTF_8);
		JsonDocument jsonDocument = JsonIO.open(file);
		jsonDocument.storeEntry("/y", "2");
		JsonIO.write(jsonDocument, false, false);
		long tempCount = Files.list(tmp)
				.filter(p -> p.getFileName().toString().startsWith(".tmp-"))
				.count();
		assertEquals(0, tempCount);
	}

	@Test
	public void writePreservesFileContentWhenChecksumFails() throws Exception {
		Path file = tmp.resolve("data.json");
		String original = "{\"name\":\"Ada\"}";
		Files.writeString(file, original, StandardCharsets.UTF_8);
		JsonDocument jsonDocument = JsonIO.open(file);
		Files.writeString(file, "{\"name\":\"Grace\"}", StandardCharsets.UTF_8);
		jsonDocument.storeEntry("/name", "\"Katherine\"");
		assertThrows(IOException.class, () -> JsonIO.write(jsonDocument, false, false));
		assertEquals("{\"name\":\"Grace\"}", Files.readString(file, StandardCharsets.UTF_8));
	}

	@Test
	public void checksumProtectsAgainstOverwritingExternalChanges() throws Exception {
		Path file = tmp.resolve("data.json");
		Files.writeString(file, "{\"name\":\"Ada\"}", StandardCharsets.UTF_8);
		JsonDocument jsonDocument = new JsonDocument(file);
		JsonIO.load(jsonDocument);
		Files.writeString(file, "{\"name\":\"Grace\"}", StandardCharsets.UTF_8);
		jsonDocument.storeEntry("/name", "\"Katherine\"");
		
		assertThrows(IOException.class, () -> JsonIO.write(jsonDocument, false, false));
	}
	
	@Test
	public void openSingleFileLoadsEntries() throws Exception {
		Path file = tmp.resolve("data.json");
		Files.writeString(file, "{\"name\":\"Ada\",\"enabled\":true}", StandardCharsets.UTF_8);

		JsonDocument jsonDocument = JsonIO.open(file);

		assertEquals("\"Ada\"", jsonDocument.getEntries().get("/name"));
		assertEquals("true", jsonDocument.getEntries().get("/enabled"));
		assertEquals(file, jsonDocument.getPath());
	}

	@Test
	public void writeSingleFileRoundTrip() throws Exception {
		Path file = tmp.resolve("roundtrip.json");
		Files.writeString(file, "{\"x\":1}", StandardCharsets.UTF_8);

		JsonDocument jsonDocument = JsonIO.open(file);
		jsonDocument.storeEntry("/y", "2");
		JsonIO.write(jsonDocument, false, false);

		JsonDocument reloaded = JsonIO.open(file);
		assertEquals("1", reloaded.getEntries().get("/x"));
		assertEquals("2", reloaded.getEntries().get("/y"));
	}

	@Test
	public void loadMalformedMissingCommaThrowsJsonLoadExceptionWithLocation() throws Exception {
		Path file = tmp.resolve("bad.json");
		Files.writeString(file, "{\"a\":1 \"b\":2}", StandardCharsets.UTF_8);

		JsonDocument jsonDocument = new JsonDocument(file);
		JsonLoadException ex = assertThrows(JsonLoadException.class, () -> JsonIO.load(jsonDocument));

		JsonParseError err = ex.getParseError();
		assertNotNull(err);
		assertEquals(1, err.line());
		assertEquals(9, err.column());
	}

	@Test
	public void loadMalformedMissingBraceThrowsJsonLoadException() throws Exception {
		Path file = tmp.resolve("bad2.json");
		Files.writeString(file, "{\"a\":1", StandardCharsets.UTF_8);

		JsonDocument jsonDocument = new JsonDocument(file);
		assertThrows(JsonLoadException.class, () -> JsonIO.load(jsonDocument));
	}

	@Test
	public void loadMalformedMissingBracketThrowsJsonLoadException() throws Exception {
		Path file = tmp.resolve("bad3.json");
		Files.writeString(file, "{\"a\":[1,2}", StandardCharsets.UTF_8);

		JsonDocument jsonDocument = new JsonDocument(file);
		assertThrows(JsonLoadException.class, () -> JsonIO.load(jsonDocument));
	}

	@Test
	public void loadEmptyStillThrowsPlainIOExceptionNotJsonLoadException() throws Exception {
		Path file = tmp.resolve("empty.json");
		Files.writeString(file, "", StandardCharsets.UTF_8);

		JsonDocument jsonDocument = new JsonDocument(file);
		IOException ex = assertThrows(IOException.class, () -> JsonIO.load(jsonDocument));
		assertTrue(!(ex instanceof JsonLoadException), "empty content is not a malformed-parse error");
	}

	private static JsonElement parseJson(String json) {
		return gson.fromJson(json, JsonElement.class);
	}
}
