package com.skanga.jsoneditor.editor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.skanga.jsoneditor.util.ResourceKeys;

/**
 * Converts between typed editor values and the raw JSON literal strings stored in resources.
 */
public final class JsonLiteralCodec {
	// Must match JsonIO.fromJson which stores literals via elem.toString() (no HTML escaping).
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
	private static final Pattern JSON_NUMBER = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");
	
	private JsonLiteralCodec() {}
	
	public static String toTypedString(String literal) {
		if (literal == null || literal.isEmpty()) {
			return "";
		}
		JsonElement element = parse(literal);
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
			return element.getAsString();
		}
		return literal;
	}
	
	public static String fromTypedString(String value) {
		return GSON.toJson(value == null ? "" : value);
	}
	
	public static String fromTypedBoolean(boolean value) {
		return Boolean.toString(value);
	}
	
	public static boolean isValidNumber(String value) {
		return value != null && JSON_NUMBER.matcher(value.trim()).matches();
	}
	
	public static String defaultLiteral(JsonNodeType type) {
        return switch (type) {
            case Object -> "object";
            case Array -> "array";
            case Number -> "0";
            case Boolean -> "false";
            case Null -> "null";
            default -> "\"\"";
        };
	}
	
	public static String normalizeRawLiteral(String raw) {
		String value = raw == null ? "" : raw.trim();
		if ("object".equals(value) || "array".equals(value)) {
			return value;
		}
		JsonElement element = parse(value);
		if (element.isJsonPrimitive()) {
			JsonPrimitive primitive = element.getAsJsonPrimitive();
			if (primitive.isNumber() && !isValidNumber(value)) {
				throw new IllegalArgumentException("Invalid JSON number");
			}
		}
		return element.toString();
	}
	
	public static Map<String,String> flattenLiteral(String key, String raw) {
		String normalized = normalizeRawLiteral(raw);
		Map<String,String> result = new LinkedHashMap<>();
		if ("object".equals(normalized) || "array".equals(normalized)) {
			result.put(key, normalized);
			return result;
		}
		flatten(key, parse(normalized), result);
		return result;
	}
	
	private static void flatten(String key, JsonElement element, Map<String,String> result) {
		if (element.isJsonObject()) {
			result.put(key, "object");
			JsonObject object = element.getAsJsonObject();
			object.entrySet().forEach(entry -> flatten(ResourceKeys.createJsonPath(key, entry.getKey()), entry.getValue(), result));
			return;
		}
		if (element.isJsonArray()) {
			result.put(key, "array");
			JsonArray array = element.getAsJsonArray();
			for (int i = 0; i < array.size(); i++) {
				flatten(ResourceKeys.createJsonPath(key, String.valueOf(i)), array.get(i), result);
			}
			return;
		}
		result.put(key, element.toString());
	}
	
	private static JsonElement parse(String literal) {
		try {
			return JsonParser.parseString(literal);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Invalid JSON literal", e);
		}
	}
}
