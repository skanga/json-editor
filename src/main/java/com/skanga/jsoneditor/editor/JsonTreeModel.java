package com.skanga.jsoneditor.editor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import javax.swing.tree.DefaultTreeModel;

import com.skanga.jsoneditor.util.MessageBundle;
import com.skanga.jsoneditor.util.ResourceKeys;

/**
 * This class represents a model for the JSON tree.
 */
public class JsonTreeModel extends DefaultTreeModel {
	@Serial
    private final static long serialVersionUID = 3261808274177599488L;

	public JsonTreeModel() {
		super(new JsonTreeNode(MessageBundle.get("tree.root.name"), List.of()));
	}

	public JsonTreeModel(List<String> keys) {
		super(new JsonTreeNode(MessageBundle.get("tree.root.name"), keys));
	}

	public JsonTreeModel(Map<String,String> values) {
		super(new JsonTreeNode(MessageBundle.get("tree.root.name"), new ArrayList<>(values.keySet()), values));
	}

	JsonTreeModel(JsonTreeNode root) {
		super(root);
	}

	public Enumeration<JsonTreeNode> getEnumeration() {
		return getEnumeration((JsonTreeNode) getRoot());
	}

	@SuppressWarnings("unchecked")
	public Enumeration<JsonTreeNode> getEnumeration(JsonTreeNode node) {
		return (Enumeration<JsonTreeNode>) (Enumeration<?>) node.depthFirstEnumeration();
	}

	public JsonTreeNode getNodeByKey(String key) {
		Enumeration<JsonTreeNode> e = getEnumeration();
	    while (e.hasMoreElements()) {
	    	JsonTreeNode n = e.nextElement();
	        if (n.getKey().equals(key)) {
	            return n;
	        }
	    }
	    return null;
	}

	public JsonTreeNode findNode(String query, Function<String,String> valueProvider) {
		if (query == null || query.trim().isEmpty()) {
			return null;
		}
		String trimmedQuery = query.trim();
		JsonTreeNode exact = getNodeByKey(trimmedQuery);
		if (exact != null) {
			return exact;
		}
		String normalizedQuery = trimmedQuery.toLowerCase(Locale.ROOT);
		Enumeration<JsonTreeNode> e = getEnumeration();
	    while (e.hasMoreElements()) {
	    	JsonTreeNode node = e.nextElement();
	    	String key = node.getKey();
	    	String value = valueProvider.apply(key);
	        if (key.toLowerCase(Locale.ROOT).contains(normalizedQuery) ||
	        		node.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery) ||
	        		value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
	            return node;
	        }
	    }
	    return null;
	}

	public int getNodeCount() {
		int count = 0;
		Enumeration<JsonTreeNode> e = getEnumeration();
	    while (e.hasMoreElements()) {
	    	e.nextElement();
	    	count++;
	    }
	    return count;
	}

	public JsonTreeNode getClosestParentNodeByKey(String key) {
		JsonTreeNode node = null;
		int count = ResourceKeys.size(key);
		while (node == null && count > 0) {
			key = ResourceKeys.withoutLastPart(key);
			node = getNodeByKey(key);
			count--;
		}
		if (node != null) {
			return node;
		} else {
			return (JsonTreeNode) getRoot();
		}
	}

	public void insertNodeInto(JsonTreeNode newChild, JsonTreeNode parent) {
		newChild.updateJsonPath(parent.getKey());
		insertNodeInto(newChild, parent, parent.getChildCount());
	}

	public void insertDescendantsInto(JsonTreeNode source, JsonTreeNode target) {
		source.getChildren().forEach(child -> {
			JsonTreeNode existing = target.getChild(child.getName());
			if (existing != null) {
				if (existing.isLeaf()) {
					removeNodeFromParent(existing);
					insertNodeInto(child, target);
				} else {
					insertDescendantsInto(child, existing);
				}
			} else {
				child.updateJsonPath(target.getKey());
				insertNodeInto(child, target);
			}
		});
	}

	public void nodeWithParentsChanged(JsonTreeNode node) {
		while (node != null) {
			nodeChanged(node);
			node = (JsonTreeNode) node.getParent();
		}
	}

	public JsonTreeModel filterByNodeName(String query) {
		if (query == null || query.trim().isEmpty()) {
			return this;
		}
		FilterQuery filter = FilterQuery.parse(query);
		JsonTreeNode filteredRoot = filterNode((JsonTreeNode) getRoot(), filter);
		return new JsonTreeModel(filteredRoot);
	}

	private JsonTreeNode filterNode(JsonTreeNode source, FilterQuery query) {
		boolean matches = !source.isRoot() && query.matches(source);
		if (matches && query.includeDescendantsOnMatch()) {
			return source.cloneWithChildren();
		}
		JsonTreeNode filtered = source.copyWithoutChildren();
		for (JsonTreeNode child : source.getChildren()) {
			JsonTreeNode filteredChild = filterNode(child, query);
			if (filteredChild != null) {
				filtered.add(filteredChild);
			}
		}
		return source.isRoot() || matches || filtered.getChildCount() > 0 ? filtered : null;
	}

	private record FilterQuery(String operator, String value) {

		static FilterQuery parse(String query) {
				String normalized = query.trim().toLowerCase(Locale.ROOT);
				int separator = normalized.indexOf(':');
				if (separator <= 0 || normalized.indexOf(':', separator + 1) >= 0) {
					return new FilterQuery("text", normalized);
				}
				String operator = normalized.substring(0, separator);
				String value = normalized.substring(separator + 1);
				if ("type".equals(operator) && isSupportedType(value)) {
					return new FilterQuery(operator, value);
				}
				if ("path".equals(operator)) {
					return new FilterQuery(operator, value);
				}
				return new FilterQuery("text", normalized);
			}

			boolean matches(JsonTreeNode node) {
				if ("type".equals(operator)) {
					return node.getJsonType().name().toLowerCase(Locale.ROOT).equals(value);
				}
				if ("path".equals(operator)) {
					return node.getKey().toLowerCase(Locale.ROOT).contains(value);
				}
				return node.getName().toLowerCase(Locale.ROOT).contains(value)
						|| node.getKey().toLowerCase(Locale.ROOT).contains(value);
			}

			boolean includeDescendantsOnMatch() {
				return !"type".equals(operator);
			}

			private static boolean isSupportedType(String value) {
				return "string".equals(value) || "number".equals(value) || "boolean".equals(value)
						|| "null".equals(value) || "object".equals(value) || "array".equals(value);
			}
		}
}
