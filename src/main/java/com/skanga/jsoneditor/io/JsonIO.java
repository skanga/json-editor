package com.skanga.jsoneditor.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.skanga.jsoneditor.model.JsonDocument;
import com.skanga.jsoneditor.util.JsonLoadException;
import com.skanga.jsoneditor.util.JsonParseError;
import com.skanga.jsoneditor.util.ResourceKeys;

/**
 * Utility functions for loading and writing JSON {@link JsonDocument} files.
 */
public final class JsonIO {
	private final static Gson gson = new Gson();
	private final static Charset UTF8_ENCODING;

	static {
		UTF8_ENCODING = StandardCharsets.UTF_8;
	}

	/**
	 * Loads a {@link JsonDocument} from disk and stores a checksum on the resource.
	 *
	 * @throws IOException if an I/O error occurs reading the file.
	 */
	public static void load(JsonDocument jsonDocument) throws IOException {
		Path path = jsonDocument.getPath();
		String content = Files.readString(path, UTF8_ENCODING);
		jsonDocument.setEntries(fromJson(content));
		jsonDocument.setChecksum(createChecksum(jsonDocument));
	}

	/** Opens a JSON file from disk and returns a loaded {@link JsonDocument}. */
	public static JsonDocument open(Path file) throws IOException {
		JsonDocument jsonDocument = new JsonDocument(file);
		load(jsonDocument);
		return jsonDocument;
	}

	/**
	 * Writes the entries of the given {@link JsonDocument} to disk.
	 * Empty leaf values are written as JSON {@code null}.
	 *
	 * <p>Performs a checksum check to detect concurrent external modifications.</p>
	 *
	 * @throws IOException if an I/O error occurs writing the file.
	 */
	public static void write(JsonDocument jsonDocument, boolean prettyPrinting, boolean flattenKeys) throws IOException {
		if (jsonDocument.getChecksum() != null) {
			String checksum = createChecksum(jsonDocument);
			if (!checksum.equals(jsonDocument.getChecksum())) {
				throw new ChecksumException("File on disk has been changed.");
			}
		}
		String content = toJson(jsonDocument.getEntries(), prettyPrinting, flattenKeys);
		Path target = jsonDocument.getPath();
		Path parentDir = target.getParent() != null ? target.getParent() : Path.of(".");
		Files.createDirectories(parentDir);
		Path temp = Files.createTempFile(parentDir, ".tmp-", null);
		try {
			Files.writeString(temp, content, UTF8_ENCODING);
			try {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			Files.deleteIfExists(temp);
			throw e;
		}
		jsonDocument.setChecksum(createChecksum(jsonDocument));
	}

	private static Map<String,String> fromJson(String json) throws IOException {
		Map<String,String> result = new LinkedHashMap<>();
		try {
			JsonElement elem = gson.fromJson(json, JsonElement.class);
			if (elem == null) {
				throw new IOException("JSON content is empty.");
			}
			fromJson("", elem, result);
		} catch (JsonParseException e) {
			throw new JsonLoadException(JsonParseError.from(e, json));
		}
		return result;
	}

	private static void fromJson(String key, JsonElement elem, Map<String,String> content) {
		if (elem.isJsonObject()) {
			content.put(key, "object");
			elem.getAsJsonObject().entrySet().forEach(entry -> {
				String newKey = ResourceKeys.createJsonPath(key, entry.getKey());
				fromJson(newKey, entry.getValue(), content);
			});
		} else if (elem.isJsonArray()) {
			content.put(key, "array");
			JsonArray array = elem.getAsJsonArray();
			for (int i = 0; i < array.size(); i++) {
				fromJson(ResourceKeys.createJsonPath(key, String.valueOf(i)), array.get(i), content);
			}
		} else {
			content.put(key, elem.toString());
		}
	}

	private static String toJson(Map<String,String> entries, boolean prettify, boolean flattenKeys) {
		JsonElement elem;
		if (entries.isEmpty()) {
			elem = new JsonObject(); // an empty resource serializes as {} (no root key to recurse from)
		} else if (flattenKeys) {
			elem = toFlatJson(entries, new ArrayList<>(entries.keySet()));
		} else {
			elem = toJsonTree(entries, "", directJsonPathChildIndex(entries));
		}
		// JsonObject keeps explicit JsonNull members only when null serialization is enabled.
		GsonBuilder builder = new GsonBuilder().disableHtmlEscaping().serializeNulls();
		if (prettify) {
			builder.setPrettyPrinting();
		}
		return builder.create().toJson(elem);
	}

	private static JsonElement toFlatJson(Map<String,String> entries, List<String> keys) {
		JsonObject object = new JsonObject();
		if (!keys.isEmpty()) {
			entries.forEach((k, v) -> {
				String kv = entries.get(k);
				if (kv == null || kv.isEmpty()) {
					object.add(k, JsonNull.INSTANCE);
					return;
				}
				if ("object".equals(kv) || "array".equals(kv)) {
					if (k.isEmpty() || hasChildKey(keys, k)) {
						return;
					}
					object.add(k, "object".equals(kv) ? new JsonObject() : new JsonArray());
				} else {
					object.add(k, toJsonElement(kv));
				}
			});
		}
		return object;
	}

	private static boolean hasChildKey(List<String> keys, String key) {
		return keys.stream().anyMatch(candidate -> ResourceKeys.isChildKeyOf(candidate, key));
	}

	private static JsonElement toJsonTree(Map<String,String> entries, String key, Map<String,List<String>> childrenByParent) {
		String value = entries.get(key);
		if ("object".equals(value) || value == null && childrenByParent.containsKey(key)) {
			JsonObject object = new JsonObject();
			childrenByParent.getOrDefault(key, new ArrayList<>()).forEach(child -> object.add(ResourceKeys.lastPart(child), toJsonTree(entries, child, childrenByParent)));
			return object;
		}
		if ("array".equals(value)) {
			JsonArray array = new JsonArray();
			childrenByParent.getOrDefault(key, new ArrayList<>()).stream()
					.sorted(Comparator.comparingInt(JsonIO::jsonArrayIndex))
					.forEach(child -> array.add(toJsonTree(entries, child, childrenByParent)));
			return array;
		}
		if (value == null || value.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		return toJsonElement(value);
	}

	private static Map<String,List<String>> directJsonPathChildIndex(Map<String,String> entries) {
		Map<String,List<String>> result = new LinkedHashMap<>();
		entries.keySet().forEach(key -> {
			if (key.isEmpty()) {
				return;
			}
			String parent = ResourceKeys.withoutLastPart(key);
			result.computeIfAbsent(parent, k -> new ArrayList<>()).add(key);
		});
		return result;
	}

	private static int jsonArrayIndex(String key) {
		try {
			return Integer.parseInt(ResourceKeys.lastPart(key));
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	private static JsonElement toJsonElement(String value) {
		try {
			return gson.fromJson(value, JsonElement.class);
		} catch (RuntimeException e) {
			return new JsonPrimitive(value);
		}
	}

	private static String createChecksum(JsonDocument jsonDocument) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		byte[] buffer = new byte[1024];
		int bytesRead;
		try (InputStream is = Files.newInputStream(jsonDocument.getPath())) {
			while ((bytesRead = is.read(buffer)) != -1) {
				digest.update(buffer, 0, bytesRead);
			}
		}
		StringBuilder result = new StringBuilder(40);
		byte[] bytes = digest.digest();
        for (byte aByte : bytes) {
            result.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
		return result.toString();
	}
}
