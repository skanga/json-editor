package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class JsonLiteralCodecTest {
	@Test
	public void stringsConvertBetweenQuotedLiteralAndTypedText() {
		assertEquals("Ada\nLovelace", JsonLiteralCodec.toTypedString("\"Ada\\nLovelace\""));
		assertEquals("\"Ada\\nLovelace\"", JsonLiteralCodec.fromTypedString("Ada\nLovelace"));
	}
	
	@Test
	public void strictJsonNumberValidationRejectsLeadingZeroes() {
		assertTrue(JsonLiteralCodec.isValidNumber("0"));
		assertTrue(JsonLiteralCodec.isValidNumber("-12.5e+2"));
		assertFalse(JsonLiteralCodec.isValidNumber("01"));
		assertFalse(JsonLiteralCodec.isValidNumber("1."));
		assertFalse(JsonLiteralCodec.isValidNumber("+1"));
	}
	
	@Test
	public void booleansAndNullSerializeExactly() {
		assertEquals("true", JsonLiteralCodec.fromTypedBoolean(true));
		assertEquals("false", JsonLiteralCodec.fromTypedBoolean(false));
		assertEquals("null", JsonLiteralCodec.defaultLiteral(JsonNodeType.Null));
	}
	
	@Test
	public void rawObjectAndArrayLiteralsFlattenIntoPathEntries() {
		Map<String,String> object = JsonLiteralCodec.flattenLiteral("/settings", "{\"b\":2,\"a\":{\"enabled\":true}}");
		assertEquals("object", object.get("/settings"));
		assertEquals("2", object.get("/settings/b"));
		assertEquals("object", object.get("/settings/a"));
		assertEquals("true", object.get("/settings/a/enabled"));
		
		Map<String,String> array = JsonLiteralCodec.flattenLiteral("/items", "[\"x\",null]");
		assertEquals("array", array.get("/items"));
		assertEquals("\"x\"", array.get("/items/0"));
		assertEquals("null", array.get("/items/1"));
	}
	
	@Test
	public void invalidRawLiteralIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> JsonLiteralCodec.normalizeRawLiteral("{broken"));
	}
}
