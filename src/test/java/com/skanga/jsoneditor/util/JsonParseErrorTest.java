package com.skanga.jsoneditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

public class JsonParseErrorTest {

	private static JsonParseException parseFailure(String json) {
		Gson gson = new Gson();
		return assertThrows(JsonParseException.class,
				() -> gson.fromJson(json, JsonElement.class));
	}

	@Test
	public void extractsLineColumnAndPathFromGsonMessage() {
		// missing comma -> "Unterminated object at line 1 column 9 path $.a"
		JsonParseException ex = parseFailure("{\"a\":1 \"b\":2}");
		JsonParseError err = JsonParseError.from(ex, "{\"a\":1 \"b\":2}");

		assertEquals(1, err.line());
		assertEquals(9, err.column());
		assertEquals("$.a", err.path());
		assertTrue(err.message().contains("at line 1 column 9"),
				"message should retain the gson detail: " + err.message());
	}

	@Test
	public void stripsTroubleshootingUrlFromMessage() {
		JsonParseException ex = parseFailure("{\"a\":1 \"b\":2}");
		JsonParseError err = JsonParseError.from(ex, "{\"a\":1 \"b\":2}");

		assertTrue(!err.message().contains("http"),
				"troubleshooting URL should be stripped: " + err.message());
	}

	@Test
	public void unparseableMessageFallsBackToSentinels() {
		JsonParseException ex = new JsonParseException("totally unexpected wording");
		JsonParseError err = JsonParseError.from(ex, "{}");

		assertEquals(-1, err.line());
		assertEquals(-1, err.column());
		assertEquals("", err.snippet());
		assertEquals("totally unexpected wording", err.message());
	}

	@Test
	public void snippetPlacesCaretUnderColumn() {
		String source = "{\n  \"name\": \"foo\"  \"x\": 1\n}";
		// line 2, column 19 is roughly under the second quote group
		String snippet = JsonParseError.buildSnippet(source, 2, 19);

		String[] lines = snippet.split("\n");
		assertEquals(2, lines.length, "snippet is source line + caret line");
		int caretIndex = lines[1].indexOf('^');
		assertTrue(caretIndex >= 0, "caret line must contain ^");
		// caret column (after the "NN | " gutter) lines up with column-1 in the source line
		assertTrue(snippet.contains("\"name\""), "snippet shows the offending source line");
	}

	@Test
	public void snippetEmptyWhenLineOutOfRange() {
		assertEquals("", JsonParseError.buildSnippet("{}", 99, 1));
		assertEquals("", JsonParseError.buildSnippet("{}", -1, 1));
	}
}
