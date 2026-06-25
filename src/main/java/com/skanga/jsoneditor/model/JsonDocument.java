package com.skanga.jsoneditor.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.skanga.jsoneditor.util.DuplicateJsonKey;
import com.skanga.jsoneditor.util.ResourceKeys;

/**
 * A resource is a container for storing JSON data and is defined by the following properties:
 *
 * <ul>
 * <li>{@code type} the type of the resource ({@code JSON}).</li>
 * <li>{@code path} the path to the resource file on disk.</li>
 * <li>{@code entries} a map containing the key-value entries of the resource.</li>
 * </ul>
 *
 * <p>Objects can listen to a resource by adding a listener which
 * will be called when any change is made to the {@code entries}.</p>
 */
public class JsonDocument {
	private final Path path;
	private final List<Consumer<JsonDocument>> listeners = new LinkedList<>();
	private Map<String,String> entries = new LinkedHashMap<>();
	private List<DuplicateJsonKey> duplicateKeys = List.of();
	private boolean duplicateKeyScanPending;
	private String checksum;

	public JsonDocument(Path path) {
		this.path = path;
	}

	public Path getPath() {
		return path;
	}

	/**
	 * Gets an immutable copy of the entries of the resource.
	 *
	 * <p>Modifications should be done via
	 * {@link #storeEntry(String, String)}, {@link #removeEntry(String)} or
	 * {@link #renameEntry(String, String)}.</p>
	 */
	public Map<String,String> getEntries() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(entries));
	}

	public void setEntries(Map<String,String> entries) {
		this.entries = new LinkedHashMap<>(entries);
	}

	public List<DuplicateJsonKey> getDuplicateKeys() {
		return List.copyOf(duplicateKeys);
	}

	public void setDuplicateKeys(List<DuplicateJsonKey> duplicateKeys) {
		this.duplicateKeys = duplicateKeys == null ? List.of() : List.copyOf(duplicateKeys);
	}

	public boolean hasDuplicateKeys() {
		return !duplicateKeys.isEmpty();
	}

	public void clearDuplicateKeys() {
		setDuplicateKeys(List.of());
		setDuplicateKeyScanPending(false);
	}

	public boolean isDuplicateKeyScanPending() {
		return duplicateKeyScanPending;
	}

	public void setDuplicateKeyScanPending(boolean duplicateKeyScanPending) {
		this.duplicateKeyScanPending = duplicateKeyScanPending;
	}

	/**
	 * Replaces all entries and notifies listeners.
	 *
	 * <p>Intended for structural JSON edits that must preserve map order.</p>
	 */
	public void replaceEntries(Map<String,String> entries) {
		setEntries(entries);
		notifyListeners();
	}

	/** Returns true if an entry with the given key exists and has a non-empty value. */
	public boolean hasEntry(String key) {
		String v = entries.get(key);
		return !(v == null || v.isEmpty());
	}

	/** Returns the value for the given key, or {@code null} if absent. */
	public String getEntry(String key) {
		return entries.get(key);
	}

	/**
	 * Stores an entry. If the value is a primitive (not object/array), any existing
	 * children of {@code key} are removed first.
	 */
	public void storeEntry(String key, String value) {
		checkKey(key);
		String existing = entries.get(key);
		if (value == null || existing != null && existing.equals(value)) {
			return;
		}
		if (!isJsonContainerValue(value)) {
			removeChildren(key);
		}
		entries.put(key, value);
		notifyListeners();
	}

	/** Removes the entry and all its descendants. */
	public void removeEntry(String key) {
		removeChildren(key);
		entries.remove(key);
		notifyListeners();
	}

	/** Renames an entry key and all its descendants. */
	public void renameEntry(String key, String newKey) {
		checkKey(newKey);
		duplicateEntry(key, newKey, false);
		notifyListeners();
	}

	/** Duplicates an entry key and all its descendants under {@code newKey}. */
	public void duplicateEntry(String key, String newKey) {
		checkKey(newKey);
		duplicateEntry(key, newKey, true);
		notifyListeners();
	}

	public void addListener(Consumer<JsonDocument> listener) {
		listeners.add(listener);
	}

	public void removeListener(Consumer<JsonDocument> listener) {
		listeners.remove(listener);
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	private void duplicateEntry(String key, String newKey, boolean keepOld) {
		Map<String,String> newEntries = new LinkedHashMap<>();
		entries.keySet().forEach(k -> {
			if (ResourceKeys.isChildKeyOf(k, key)) {
				String nk = createDescendantKey(newKey, ResourceKeys.childKey(k, key));
				newEntries.put(nk, entries.get(k));
			}
		});
		if (entries.containsKey(key)) {
			newEntries.put(newKey, entries.get(key));
		}
		if (!keepOld) {
			removeChildren(key);
			entries.remove(key);
		}
		newEntries.forEach(this::putEntry);
	}

	private void putEntry(String key, String value) {
		if (value == null) {
			return;
		}
		if (!isJsonContainerValue(value)) {
			removeChildren(key);
		}
		entries.put(key, value);
	}

	private void removeChildren(String key) {
		new LinkedList<>(entries.keySet()).forEach(k -> {
			if (ResourceKeys.isChildKeyOf(k, key)) {
				entries.remove(k);
			}
		});
	}

	private void notifyListeners() {
		listeners.forEach(l -> l.accept(this));
	}

	private void checkKey(String key) {
		if (!key.isEmpty() && !ResourceKeys.isJsonPath(key)) {
			throw new IllegalArgumentException("Key is not valid.");
		}
	}

	private boolean isJsonContainerValue(String value) {
		return "object".equals(value) || "array".equals(value);
	}

	private String createDescendantKey(String parentKey, String childKey) {
		if (childKey == null || childKey.isEmpty()) {
			return parentKey;
		}
		if (parentKey == null || parentKey.isEmpty()) {
			return childKey;
		}
		return parentKey + childKey;
	}
}
