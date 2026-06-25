package com.skanga.jsoneditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;

public class ColorsTest {

	@Test
	public void isDarkUsesPerceivedLuminance() {
		assertTrue(Colors.isDark(new Color(40, 40, 40)));
		assertFalse(Colors.isDark(Color.WHITE));
		assertFalse(Colors.isDark(null));
	}

	@Test
	public void forBackgroundBrightensOnDarkAndKeepsOnLight() {
		Color base = new Color(27, 132, 90);
		Color onLight = Colors.forBackground(base, Color.WHITE);
		Color onDark = Colors.forBackground(base, new Color(40, 40, 40));

		assertEquals(base, onLight);
		int baseSum = base.getRed() + base.getGreen() + base.getBlue();
		int darkSum = onDark.getRed() + onDark.getGreen() + onDark.getBlue();
		assertTrue(darkSum > baseSum, "color should brighten against a dark background");
	}

	@Test
	public void scaleClampsToValidRange() {
		Color scaled = Colors.scale(new Color(200, 200, 200), 2f);
		assertEquals(255, scaled.getRed());
		assertEquals(255, scaled.getGreen());
		assertEquals(255, scaled.getBlue());
	}

	@Test
	public void mutedStaysLegibleOnBothThemes() {
		// Light theme: dark text on light background -> muted color must stay darker than the background.
		Color onLight = Colors.muted(Color.BLACK, Color.WHITE);
		assertTrue(luminance(onLight) < luminance(Color.WHITE), "muted text must contrast with a light background");

		// Dark theme regression: the old code darkened the background, which vanished on dark themes.
		// muted() must produce a colour LIGHTER than a dark background so it remains readable.
		Color darkBg = new Color(40, 40, 40);
		Color onDark = Colors.muted(Color.WHITE, darkBg);
		assertTrue(luminance(onDark) > luminance(darkBg), "muted text must be lighter than a dark background");
	}

	@Test
	public void mutedReturnsTextWhenEitherArgumentNull() {
		assertEquals(Color.RED, Colors.muted(Color.RED, null));
		assertEquals(null, Colors.muted(null, Color.WHITE));
	}

	private static double luminance(Color c) {
		return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
	}
}
