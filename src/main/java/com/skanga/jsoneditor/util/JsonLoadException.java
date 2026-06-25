package com.skanga.jsoneditor.util;

import java.io.IOException;

/**
 * Thrown when a JSON file cannot be parsed. Carries a {@link JsonParseError}
 * with the failure location so the UI can show a precise, located message.
 * Extends {@link IOException} so existing {@code catch (IOException)} call
 * sites continue to handle it without change.
 */
public class JsonLoadException extends IOException {

	private final transient JsonParseError parseError;

	public JsonLoadException(JsonParseError parseError) {
		super(parseError.message());
		this.parseError = parseError;
	}

	public JsonParseError getParseError() {
		return parseError;
	}
}
