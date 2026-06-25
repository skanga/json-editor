package com.skanga.jsoneditor.editor;

/**
 * The JSON value type represented by a JSON tree node.
 */
public enum JsonNodeType {
	Object("Object"),
	Array("Array"),
	String("String"),
	Number("Number"),
	Boolean("Boolean"),
	Null("Null"),
	Unknown("JSON");
	
	private final String label;
	
	JsonNodeType(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	public static JsonNodeType fromJsonValue(String value) {
		if (value == null) {
			return Unknown;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return String;
		}
		if ("object".equals(trimmed) || trimmed.startsWith("{")) {
			return Object;
		}
		if ("array".equals(trimmed) || trimmed.startsWith("[")) {
			return Array;
		}
		if ("true".equals(trimmed) || "false".equals(trimmed)) {
			return Boolean;
		}
		if ("null".equals(trimmed)) {
			return Null;
		}
		if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
			return String;
		}
		if (isNumber(trimmed)) {
			return Number;
		}
		return Unknown;
	}
	
	private static boolean isNumber(String value) {
		return JsonLiteralCodec.isValidNumber(value);
	}
}
