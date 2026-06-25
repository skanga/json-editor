package com.skanga.jsoneditor.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.skanga.jsoneditor.model.JsonDocument;
import com.skanga.jsoneditor.util.ResourceKeys;

/**
 * Structural mutations for JSON resources backed by path-keyed literal maps.
 */
public class JsonResourceMutator {
	private final JsonDocument jsonDocument;
	
	public JsonResourceMutator(JsonDocument jsonDocument) {
		this.jsonDocument = jsonDocument;
	}
	
	public void setNodeLiteral(String key, String literal) {
		replaceSubtree(key, JsonLiteralCodec.flattenLiteral(key, literal));
	}
	
	public void changeType(String key, JsonNodeType type) {
		replaceSubtree(key, JsonLiteralCodec.flattenLiteral(key, JsonLiteralCodec.defaultLiteral(type)));
	}
	
	public String addObjectProperty(String parentKey, String name) {
		String childKey = ResourceKeys.createJsonPath(parentKey, name);
		Map<String,String> subtree = new LinkedHashMap<>();
		subtree.put(childKey, JsonLiteralCodec.defaultLiteral(JsonNodeType.String));
		List<String> children = directChildren(parentKey);
		children.add(childKey);
		rebuildParent(parentKey, children, Map.of(childKey, subtree));
		return childKey;
	}
	
	public String renameObjectProperty(String key, String newName) {
		String parentKey = ResourceKeys.withoutLastPart(key);
		String newKey = ResourceKeys.createJsonPath(parentKey, newName);
		Map<String,String> moved = rekeySubtree(key, newKey);
		List<String> children = directChildren(parentKey).stream()
				.map(child -> child.equals(key) ? newKey : child)
				.collect(Collectors.toList());
		rebuildParent(parentKey, children, Map.of(newKey, moved));
		return newKey;
	}
	
	public void moveObjectProperty(String key, int delta) {
		String parentKey = ResourceKeys.withoutLastPart(key);
		List<String> children = directChildren(parentKey);
		int index = children.indexOf(key);
		if (index < 0) {
			return;
		}
		int newIndex = Math.clamp(index + delta, 0, children.size() - 1);
		if (index == newIndex) {
			return;
		}
		children.remove(index);
		children.add(newIndex, key);
		rebuildParent(parentKey, children, Map.of());
	}
	
	public String appendArrayItem(String arrayKey) {
		return insertArrayItem(arrayKey, arrayChildren(arrayKey).size());
	}
	
	public String insertArrayItem(String arrayKey, int index) {
		List<Map<String,String>> subtrees = new ArrayList<>();
		List<String> children = arrayChildren(arrayKey);
		int target = Math.clamp(index, 0, children.size());
		for (int i = 0; i <= children.size(); i++) {
			if (i == target) {
				subtrees.add(defaultArrayItem(arrayKey, subtrees.size()));
			}
			if (i < children.size()) {
				subtrees.add(rekeySubtree(children.get(i), ResourceKeys.createJsonPath(arrayKey, String.valueOf(subtrees.size()))));
			}
		}
		rebuildParent(arrayKey, subtreeRoots(subtrees), subtreesByRoot(subtrees));
		return ResourceKeys.createJsonPath(arrayKey, String.valueOf(target));
	}
	
	public void deleteArrayItem(String itemKey) {
		String arrayKey = ResourceKeys.withoutLastPart(itemKey);
		List<Map<String,String>> subtrees = new ArrayList<>();
		for (String child : arrayChildren(arrayKey)) {
			if (!child.equals(itemKey)) {
				subtrees.add(rekeySubtree(child, ResourceKeys.createJsonPath(arrayKey, String.valueOf(subtrees.size()))));
			}
		}
		rebuildParent(arrayKey, subtreeRoots(subtrees), subtreesByRoot(subtrees));
	}
	
	public String moveArrayItem(String itemKey, int delta) {
		String arrayKey = ResourceKeys.withoutLastPart(itemKey);
		List<String> children = arrayChildren(arrayKey);
		int index = children.indexOf(itemKey);
		if (index < 0) {
			return itemKey;
		}
		int newIndex = Math.clamp(index + delta, 0, children.size() - 1);
		if (index == newIndex) {
			return itemKey;
		}
		children.remove(index);
		children.add(newIndex, itemKey);
		rebuildArrayWithChildren(arrayKey, children);
		return ResourceKeys.createJsonPath(arrayKey, String.valueOf(newIndex));
	}
	
	public String duplicateArrayItem(String itemKey) {
		String arrayKey = ResourceKeys.withoutLastPart(itemKey);
		List<String> children = arrayChildren(arrayKey);
		int index = children.indexOf(itemKey);
		if (index < 0) {
			return itemKey;
		}
		children.add(index + 1, itemKey);
		rebuildArrayWithChildren(arrayKey, children);
		return ResourceKeys.createJsonPath(arrayKey, String.valueOf(index + 1));
	}
	
	private void rebuildArrayWithChildren(String arrayKey, List<String> children) {
		List<Map<String,String>> subtrees = new ArrayList<>();
		for (String child : children) {
			subtrees.add(rekeySubtree(child, ResourceKeys.createJsonPath(arrayKey, String.valueOf(subtrees.size()))));
		}
		rebuildParent(arrayKey, subtreeRoots(subtrees), subtreesByRoot(subtrees));
	}
	
	private Map<String,String> defaultArrayItem(String arrayKey, int index) {
		Map<String,String> result = new LinkedHashMap<>();
		result.put(ResourceKeys.createJsonPath(arrayKey, String.valueOf(index)),
				JsonLiteralCodec.defaultLiteral(JsonNodeType.String));
		return result;
	}
	
	private void replaceSubtree(String key, Map<String,String> replacement) {
		Map<String,String> result = new LinkedHashMap<>();
		boolean inserted = false;
		for (Map.Entry<String,String> entry : jsonDocument.getEntries().entrySet()) {
			String currentKey = entry.getKey();
			if (currentKey.equals(key)) {
				result.putAll(replacement);
				inserted = true;
				continue;
			}
			if (isSelfOrDescendant(currentKey, key)) {
				continue;
			}
			result.put(currentKey, entry.getValue());
		}
		if (!inserted) {
			result.putAll(replacement);
		}
		jsonDocument.replaceEntries(result);
	}
	
	private void rebuildParent(String parentKey, List<String> childOrder, Map<String,Map<String,String>> replacements) {
		Map<String,String> current = jsonDocument.getEntries();
		Map<String,String> result = new LinkedHashMap<>();
		boolean emitted = false;
		for (Map.Entry<String,String> entry : current.entrySet()) {
			String key = entry.getKey();
			if (key.equals(parentKey)) {
				result.put(key, entry.getValue());
				emitChildren(result, childOrder, replacements);
				emitted = true;
				continue;
			}
			if (ResourceKeys.isChildKeyOf(key, parentKey)) {
				continue;
			}
			result.put(key, entry.getValue());
		}
		if (!emitted) {
			result.put(parentKey, "object");
			emitChildren(result, childOrder, replacements);
		}
		jsonDocument.replaceEntries(result);
	}
	
	private void emitChildren(Map<String,String> result, List<String> childOrder,
			Map<String,Map<String,String>> replacements) {
		for (String child : childOrder) {
			Map<String,String> subtree = replacements.getOrDefault(child, collectSubtree(child));
			result.putAll(subtree);
		}
	}
	
	private List<String> directChildren(String parentKey) {
		return jsonDocument.getEntries().keySet().stream()
				.filter(key -> isDirectChild(key, parentKey))
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private List<String> arrayChildren(String arrayKey) {
		return directChildren(arrayKey).stream()
				.sorted(Comparator.comparingInt(this::arrayIndex))
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private int arrayIndex(String key) {
		try {
			return Integer.parseInt(ResourceKeys.lastPart(key));
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}
	
	private boolean isDirectChild(String key, String parentKey) {
		return !key.equals(parentKey) && ResourceKeys.isChildKeyOf(key, parentKey)
				&& ResourceKeys.withoutLastPart(key).equals(parentKey);
	}
	
	private boolean isSelfOrDescendant(String key, String parentKey) {
		return key.equals(parentKey) || ResourceKeys.isChildKeyOf(key, parentKey);
	}
	
	private Map<String,String> collectSubtree(String rootKey) {
		Map<String,String> result = new LinkedHashMap<>();
		jsonDocument.getEntries().forEach((key, value) -> {
			if (isSelfOrDescendant(key, rootKey)) {
				result.put(key, value);
			}
		});
		return result;
	}
	
	private Map<String,String> rekeySubtree(String oldRoot, String newRoot) {
		Map<String,String> result = new LinkedHashMap<>();
		collectSubtree(oldRoot).forEach((key, value) -> {
			String suffix = key.equals(oldRoot) ? "" : ResourceKeys.childKey(key, oldRoot);
			String newKey = suffix.isEmpty() ? newRoot : newRoot + suffix;
			result.put(newKey, value);
		});
		return result;
	}
	
	private List<String> subtreeRoots(List<Map<String,String>> subtrees) {
		return subtrees.stream()
				.map(subtree -> subtree.keySet().iterator().next())
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private Map<String,Map<String,String>> subtreesByRoot(List<Map<String,String>> subtrees) {
		Map<String,Map<String,String>> result = new LinkedHashMap<>();
		for (Map<String,String> subtree : subtrees) {
			result.put(subtree.keySet().iterator().next(), subtree);
		}
		return result;
	}
}
