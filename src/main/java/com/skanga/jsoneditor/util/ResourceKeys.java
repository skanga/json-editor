package com.skanga.jsoneditor.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.skanga.jsoneditor.model.JsonDocument;

/**
 * This class provides key/JSON-path utility functions for a {@link JsonDocument}.
 *
 * <p>A key is a JSON-path {@code String} whose parts are separated by a slash
 * (e.g. {@code /parent/child}); the empty string denotes the root.</p>
 */
public final class ResourceKeys {
	
	public static String createJsonPath(String parent, String child) {
		String segment = escapeJsonPathSegment(child);
		if (parent == null || parent.isEmpty()) {
			return "/" + segment;
		}
		return parent + "/" + segment;
	}
	
	/**
	 * Returns the size of a key.
	 * The size is the number of parts the key consists of.
	 * 
	 * @param 	key the key.
	 * @return	the number of parts the key consists of.
	 */
	public static int size(String key) {
		return parts(key).length;
	}
	
	/**
	 * Returns the parts of a key.
	 * 
	 * @param 	key the key.
	 * @return	the parts of the key.
	 */
	public static String[] parts(String key) {
		if (!isJsonPath(key)) {
			return new String[]{key == null ? "" : key};
		}
		return Arrays.stream(key.substring(1).split("/", -1))
				.map(ResourceKeys::unescapeJsonPathSegment)
				.toArray(String[]::new);
	}
	
	/**
	 * Returns the first part of a key.
	 * 
	 * @param 	key the key.
	 * @return 	the first part.
	 */
	public static String firstPart(String key) {
		return parts(key)[0];
	}
	
	/**
	 * Returns the last part of a key.
	 * 
	 * @param 	key the key.
	 * @return 	the last part.
	 */
	public static String lastPart(String key) {
		String[] parts = parts(key);
		return parts[parts.length-1];
	}
	
	/**
	 * Creates a new key consisting of all but the first part.
	 * If the key has only one part, an empty key will be returned.
	 * 
	 * @param 	key the key.
	 * @return	the key without the first part.
	 */
	public static String withoutFirstPart(String key) {
		if (!isJsonPath(key)) return "";
		String[] parts = parts(key);
		if (parts.length <= 1) return "";
		return toJsonPath(Arrays.copyOfRange(parts, 1, parts.length));
	}
	
	/**
	 * Creates a new key consisting of all but the last part.
	 * If the key has only one part, an empty key will be returned.
	 * 
	 * @param 	key the key.
	 * @return	the key without the last part.
	 */
	public static String withoutLastPart(String key) {
		if (!isJsonPath(key)) return "";
		String[] parts = parts(key);
		if (parts.length <= 1) return "";
		return toJsonPath(Arrays.copyOfRange(parts, 0, parts.length-1));
	}
	
	/**
	 * Retrieve the part of the given key which is a child part of the given parent key.
	 * A key is a child of another key if it has the same parts at the beginning as the other key.
	 * This function will only return the child parts, so without the beginning parent parts.
	 * 
	 * <p>If the resulting key is the same as the given key, the key is considered not to be a child 
	 * of the given parent key, so an empty key will be returned.</p>
	 * 
	 * @param 	key the original key.
	 * @param 	parentKey a possible parent key of the original key.
	 * @return 	the part of the given key which is a child of the given parent key.
	 */
	public static String childKey(String key, String parentKey) {
		if (key == null || key.isEmpty()) return "";
		if (parentKey == null || parentKey.isEmpty()) return key;
		String prefix = parentKey + "/";
		if (!key.startsWith(prefix)) return "";
		return key.substring(parentKey.length());
	}
	
	/**
	 * Checks whether the given key is a child key of the given parent key.
	 * A key is a child of another key if it has the same parts at the beginning as the other key.
	 * 
	 * @param 	key the original key.
	 * @param 	parentKey the possible parent key of the original key.
	 * @return	whether the given key is a child of the given parent key.
	 */
	public static boolean isChildKeyOf(String key, String parentKey) {
		if (key == null || key.isEmpty()) return false;
		if (parentKey == null || parentKey.isEmpty()) return true;
		return key.startsWith(parentKey + "/");
	}
	
	/**
	 * Gets the unique root keys of a list of keys. A root key is the first part of a key.
	 * 
	 * @param 	keys the keys to retrieve the unique root keys from.
	 * @return 	the unique root keys.
	 */
	public static List<String> uniqueRootKeys(List<String> keys) {
		LinkedHashSet<String> result = new LinkedHashSet<>();
		keys.forEach(key -> {
			if (key.isEmpty()) {
				return;
			}
			String rootKey = firstPart(key);
			result.add(rootKey);
		});
		return new ArrayList<>(result);
	}
	
	/**
	 * Gets all keys from the given key list which are a child of the given parent key.
	 * A key is a child of another key if it has the same parts at the beginning as the other key.
	 *
	 * @param 	keys the keys to retrieve the child keys from.
	 * @param 	parentKey the parent key.
	 * @return	the keys of the given key list which are a child of the given parent key.
	 */
	public static List<String> extractChildKeys(List<String> keys, String parentKey) {
		List<String> result = new LinkedList<>();
		keys.forEach(key -> {
			if (!isJsonPath(key)) {
				return; // the root key "" has no child contribution
			}
			String[] parts = parts(key);
			if (parts.length > 1 && parts[0].equals(parentKey)) {
				result.add(toJsonPath(Arrays.copyOfRange(parts, 1, parts.length)));
			}
		});
		return result;
	}
	
	public static boolean isJsonPath(String key) {
		return key != null && key.startsWith("/");
	}
	
	public static String escapeJsonPathSegment(String value) {
		return value.replace("~", "~0").replace("/", "~1");
	}
	
	public static String unescapeJsonPathSegment(String value) {
		return value.replace("~1", "/").replace("~0", "~");
	}
	
	public static String toJsonPath(String[] parts) {
		if (parts.length == 0) {
			return "";
		}
		return "/" + Arrays.stream(parts)
				.map(ResourceKeys::escapeJsonPathSegment)
				.collect(Collectors.joining("/"));
	}
}
