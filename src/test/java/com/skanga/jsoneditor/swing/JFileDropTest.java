package com.skanga.jsoneditor.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

public class JFileDropTest {

	@Test
	public void parsesPlainPath() {
		assertEquals(new File("/tmp/data.json"), JFileDrop.toFile("/tmp/data.json"));
	}

	@Test
	public void parsesEncodedFileUri() {
		assertEquals(new File("/tmp/my data.json"),
				JFileDrop.toFile("file:///tmp/my%20data.json"));
	}

	@Test
	public void parsesFileUriWithUnencodedSpaces() {
		// GTK/KDE drops sometimes send unencoded characters, which new URI(String) rejects.
		assertEquals(new File("/tmp/my data.json"),
				JFileDrop.toFile("file:///tmp/my data.json"));
	}
}
