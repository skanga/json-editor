package com.skanga.jsoneditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ExtendedPropertiesTest {
	@TempDir
	public Path tmp;

	@Test
	public void typedValuesRoundTripThroughProperties() {
		ExtendedProperties props = new ExtendedProperties();

		props.setProperty("list", List.of("a", "b", "c"));
		props.setProperty("integer", 42);
		props.setProperty("enabled", true);
		props.setProperty("locale", Locale.of("pt", "BR"));

		assertEquals(List.of("a", "b", "c"), props.getListProperty("list"));
		assertEquals(42, props.getIntegerProperty("integer"));
		assertEquals(true, props.getBooleanProperty("enabled"));
		assertEquals(Locale.of("pt", "BR"), props.getLocaleProperty("locale"));
	}

	@Test
	public void emptyListPropertyRoundTripsAsEmptyList() {
		ExtendedProperties props = new ExtendedProperties();

		props.setProperty("list", List.of());

		assertTrue(props.getListProperty("list").isEmpty());
	}

	@Test
	public void invalidTypedValuesReturnDefaults() {
		ExtendedProperties props = new ExtendedProperties();
		props.setProperty("integer", "not-number");

		assertEquals(7, props.getIntegerProperty("integer", 7));
		assertEquals(false, props.getBooleanProperty("missing-boolean", false));
		assertEquals(Locale.ENGLISH, props.getLocaleProperty("missing-locale", Locale.ENGLISH));
	}

	@Test
	public void storeWritesSortedKeysWithoutTimestampHeader() throws Exception {
		ExtendedProperties props = new ExtendedProperties();
		props.setProperty("b", "2");
		props.setProperty("a", "1");
		Path file = tmp.resolve("settings.properties");
		
		props.store(file);
		
		List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		assertEquals("a=1", lines.get(0));
		assertEquals("b=2", lines.get(1));
	}
	
}
