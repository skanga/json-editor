package com.skanga.jsoneditor.editor;

import java.io.Serial;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.tree.DefaultMutableTreeNode;

import com.skanga.jsoneditor.util.ResourceKeys;

/**
 * This class represents a single node of the JSON tree.
 */
public class JsonTreeNode extends DefaultMutableTreeNode {
	@Serial
    private final static long serialVersionUID = -7372403592538358822L;
	private String name;
	private String key;
	private JsonNodeType jsonType;
	private int jsonChildCount = -1;

	private JsonTreeNode(String name, String key, JsonNodeType jsonType) {
		super();
		this.name = name;
		this.key = key;
		this.jsonType = jsonType;
	}

	public JsonTreeNode(String name, List<String> keys) {
		this(name, keys, Map.of(), true, null);
	}

	public JsonTreeNode(String name, List<String> keys, String key) {
		this(name, keys, Map.of(), true, key);
	}

	public JsonTreeNode(String name, List<String> keys, Map<String,String> values) {
		this(name, keys, values, true, null);
	}

	private JsonTreeNode(String name, List<String> keys, Map<String,String> values, boolean jsonPathMode, String key) {
		super();
		this.name = name;
		this.key = key == null ? "" : key;
		this.jsonType = JsonNodeType.fromJsonValue(values.get(getKey()));
		ResourceKeys.uniqueRootKeys(keys).forEach(rootKey -> {
			List<String> subKeys = ResourceKeys.extractChildKeys(keys, rootKey);
			String childKey = ResourceKeys.createJsonPath(this.key, rootKey);
			add(new JsonTreeNode(rootKey, subKeys, values, true, childKey));
		});
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public JsonNodeType getJsonType() {
		return jsonType;
	}

	public void setJsonType(JsonNodeType jsonType) {
		this.jsonType = jsonType == null ? JsonNodeType.Unknown : jsonType;
	}

	public int getJsonChildCount() {
		return jsonChildCount >= 0 ? jsonChildCount : getChildCount();
	}

	public void updateJsonPath(String parentKey) {
		if (key == null) {
			return;
		}
		key = ResourceKeys.createJsonPath(parentKey, name);
		getChildren().forEach(child -> child.updateJsonPath(key));
	}

	public String getKey() {
		return key;
	}

	@SuppressWarnings("unchecked")
	public List<JsonTreeNode> getChildren() {
		return Collections.list((Enumeration<JsonTreeNode>) (Enumeration<?>) children());
	}

	public JsonTreeNode getChild(String name) {
		Optional<JsonTreeNode> child = getChildren().stream()
				.filter(i -> i.getName().equals(name))
				.findFirst();
		return child.orElse(null);
	}

	public JsonTreeNode cloneWithChildren() {
		return cloneWithChildren(this);
	}

	JsonTreeNode copyWithoutChildren() {
		JsonTreeNode copy = new JsonTreeNode(name, key, jsonType);
		copy.jsonChildCount = getJsonChildCount();
		return copy;
	}

	@Override
	public String toString() {
		return name;
	}

	private JsonTreeNode cloneWithChildren(JsonTreeNode parent) {
		JsonTreeNode newParent = (JsonTreeNode) parent.clone();
		for (JsonTreeNode n : parent.getChildren()) {
			newParent.add(cloneWithChildren(n));
		}
		return newParent;
	}
}
