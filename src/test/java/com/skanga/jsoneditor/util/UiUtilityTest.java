package com.skanga.jsoneditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

public class UiUtilityTest {
	@Test
	public void imagesLoadClasspathIcons() {
		assertNotNull(Images.getClasspathURL("images/icon-16.png"));
		assertEquals(16, Images.loadFromClasspath("images/icon-16.png").getIconWidth());
	}

	@Test
	public void iconPngResourcesLoadWithExpectedDimensions() throws IOException {
		assertPngSize("images/icon-16.png", 16, 16);
		assertPngSize("images/icon-20.png", 20, 20);
		assertPngSize("images/icon-24.png", 24, 24);
		assertPngSize("images/icon-32.png", 32, 32);
		assertPngSize("images/icon-48.png", 48, 48);
		assertPngSize("images/icon-64.png", 64, 64);
		assertPngSize("images/icon-128.png", 128, 128);
		assertPngSize("images/icon-256.png", 256, 256);
		assertPngSize("images/icon-512.png", 512, 512);
		assertPngSize("images/icon-intro.png", 128, 128);
		assertPngSize("images/icon-folder.png", 14, 12);
	}

	@Test
	public void tinyIconPngResourcesContainVisiblePixels() throws IOException {
		assertVisiblePixelCountAtLeast("images/icon-16.png", 20);
		assertVisiblePixelCountAtLeast("images/icon-folder.png", 20);
	}
	
	@Test
	public void messageBundleLoadsFormatsAndReturnsMnemonics() {
		Locale previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.ENGLISH);
			MessageBundle.loadResources();
			
			assertEquals("About JSON Editor", MessageBundle.get("dialogs.about.title", "JSON Editor"));
			assertEquals('F', MessageBundle.getMnemonic("menu.file.vk"));
		} finally {
			Locale.setDefault(previous);
			MessageBundle.loadResources();
		}
	}
	
	@Test
	public void messageBundleCanSwitchLocale() {
		Locale previous = Locale.getDefault();
		try {
			MessageBundle.setLocale(Locale.of("nl"));
			assertEquals("Over JSON Editor", MessageBundle.get("dialogs.about.title", "JSON Editor"));
		} finally {
			Locale.setDefault(previous);
			MessageBundle.loadResources();
		}
	}

	private static void assertPngSize(String resourceName, int width, int height) throws IOException {
		BufferedImage image = loadPng(resourceName);
		assertEquals(width, image.getWidth(), resourceName);
		assertEquals(height, image.getHeight(), resourceName);
	}

	private static void assertVisiblePixelCountAtLeast(String resourceName, int minimum) throws IOException {
		BufferedImage image = loadPng(resourceName);
		int visible = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (((image.getRGB(x, y) >>> 24) & 0xff) > 0) {
					visible++;
				}
			}
		}
		assertTrue(visible >= minimum, resourceName + " visible pixels");
	}

	private static BufferedImage loadPng(String resourceName) throws IOException {
		try (InputStream input = UiUtilityTest.class.getClassLoader().getResourceAsStream(resourceName)) {
			assertNotNull(input, resourceName);
			BufferedImage image = ImageIO.read(input);
			assertNotNull(image, resourceName);
			return image;
		}
	}
}
