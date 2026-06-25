package com.skanga.jsoneditor.util;

/** A duplicate JSON object key found in the original source text. */
public record DuplicateJsonKey(String key, String path) {
}
