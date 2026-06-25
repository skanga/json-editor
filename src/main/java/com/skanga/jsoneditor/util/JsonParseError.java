package com.skanga.jsoneditor.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonParseException;

/**
 * Structured, user-facing representation of a JSON parse failure.
 *
 * <p>Gson reports the failure location only inside its exception message
 * (e.g. {@code "... at line 1 column 9 path $.a"}); it exposes no line/column
 * accessors. This carrier parses that message and builds a caret snippet so
 * the editor can show the user exactly what and where the problem is.</p>
 */
public record JsonParseError(String message, int line, int column, String path, String snippet) {

	// "... at line 1 column 9 path $.a"  (path group is optional)
	private static final Pattern LOCATION =
			Pattern.compile("at line (\\d+) column (\\d+)(?: path (\\S+))?");

	// Gson appends a separate line linking its troubleshooting guide; drop it.
	private static final Pattern TROUBLESHOOTING_URL =
			Pattern.compile("\\s*See https?://\\S+", Pattern.DOTALL);

	public static JsonParseError from(JsonParseException ex, String source) {
		String raw = rootMessage(ex);
		String message = TROUBLESHOOTING_URL.matcher(raw).replaceAll("").trim();

		Matcher m = LOCATION.matcher(message);
		if (!m.find()) {
			return new JsonParseError(message, -1, -1, "", "");
		}
		int line = Integer.parseInt(m.group(1));
		int column = Integer.parseInt(m.group(2));
		String path = m.group(3) != null ? m.group(3) : "";
		return new JsonParseError(message, line, column, path, buildSnippet(source, line, column));
	}

	/** Unwraps to the deepest cause message so we show Gson's specific wording, not the wrapper. */
	private static String rootMessage(Throwable ex) {
		Throwable cause = ex;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		String msg = cause.getMessage();
		return msg != null ? msg : ex.toString();
	}

	/**
	 * Returns the offending source line plus a caret line marking the column,
	 * or {@code ""} when line/column are out of range. Tabs are expanded to
	 * single spaces before measuring so the caret aligns under a monospaced font.
	 */
	static String buildSnippet(String source, int line, int column) {
		if (line < 1 || column < 1 || source == null) {
			return "";
		}
		String[] lines = source.split("\n", -1);
		if (line > lines.length) {
			return "";
		}
		String src = lines[line - 1].replace('\t', ' ');
		String gutter = line + " | ";
		int caretCol = gutter.length() + Math.min(column - 1, src.length());
		String caretLine = " ".repeat(caretCol) + "^";
		return gutter + src + "\n" + caretLine;
	}
}
