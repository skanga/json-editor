package com.skanga.jsoneditor.io;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.skanga.jsoneditor.util.DuplicateJsonKey;
import com.skanga.jsoneditor.util.ResourceKeys;

/** Streaming duplicate-key detector that does not materialize a JSON tree. */
public final class DuplicateJsonKeyDetector {
	private DuplicateJsonKeyDetector() {
	}

	public static List<DuplicateJsonKey> findDuplicates(String json) throws IOException {
		List<DuplicateJsonKey> duplicates = new ArrayList<>();
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setStrictness(Strictness.LENIENT);
		scanValue(reader, "", duplicates);
		return List.copyOf(duplicates);
	}

	private static void scanValue(JsonReader reader, String path, List<DuplicateJsonKey> duplicates) throws IOException {
		JsonToken token = reader.peek();
		if (token == JsonToken.BEGIN_OBJECT) {
			scanObject(reader, path, duplicates);
		} else if (token == JsonToken.BEGIN_ARRAY) {
			scanArray(reader, path, duplicates);
		} else {
			reader.skipValue();
		}
	}

	private static void scanObject(JsonReader reader, String path, List<DuplicateJsonKey> duplicates) throws IOException {
		Set<String> names = new HashSet<>();
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			String childPath = ResourceKeys.createJsonPath(path, name);
			if (!names.add(name)) {
				duplicates.add(new DuplicateJsonKey(name, childPath));
			}
			scanValue(reader, childPath, duplicates);
		}
		reader.endObject();
	}

	private static void scanArray(JsonReader reader, String path, List<DuplicateJsonKey> duplicates) throws IOException {
		reader.beginArray();
		int index = 0;
		while (reader.hasNext()) {
			scanValue(reader, ResourceKeys.createJsonPath(path, String.valueOf(index)), duplicates);
			index++;
		}
		reader.endArray();
	}
}
