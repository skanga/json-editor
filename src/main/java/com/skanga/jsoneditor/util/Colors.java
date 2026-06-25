package com.skanga.jsoneditor.util;

import java.awt.Color;

/**
 * This class provides utility functions for colors.
 */
public final class Colors {

	/**
	 * Multiplies the RGB values of a color with the given factor.
	 * 
	 * @param 	c the original color
	 * @param 	factor the factor to scale with
	 * @return 	the scaled color
	 */
	public static Color scale(Color c, float factor) {
		return new Color(
				roundColorValue(c.getRed()*factor), 
				roundColorValue(c.getGreen()*factor), 
				roundColorValue(c.getBlue()*factor),
				c.getAlpha());
	}

	/**
	 * Determines whether the given background color is dark, using perceived
	 * luminance. Returns {@code false} for {@code null}.
	 *
	 * @param 	bg the background color to inspect
	 * @return	{@code true} if the color is dark
	 */
	public static boolean isDark(Color bg) {
		if (bg == null) {
			return false;
		}
		double luminance = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
		return luminance < 0.5;
	}

	/**
	 * Returns a variant of the given color that stays legible against the given
	 * background: on dark backgrounds the color is brightened, on light
	 * backgrounds it is returned unchanged.
	 *
	 * @param 	c the base color
	 * @param 	bg the background the color is drawn against
	 * @return	a theme-appropriate variant of the color
	 */
	public static Color forBackground(Color c, Color bg) {
		return isDark(bg) ? scale(c, 1.5f) : c;
	}

	/**
	 * Returns a muted (dimmed) variant of a text color for low-emphasis text: the
	 * midpoint between the text color and the background it is drawn against.
	 * Unlike darkening the background, this stays legible on both light and dark
	 * themes. Returns {@code text} unchanged if either argument is {@code null}.
	 *
	 * @param 	text the text color to mute
	 * @param 	background the background the text is drawn against
	 * @return	a muted color that still contrasts with the background
	 */
	public static Color muted(Color text, Color background) {
		if (text == null || background == null) {
			return text;
		}
		return new Color(
				(text.getRed() + background.getRed()) / 2,
				(text.getGreen() + background.getGreen()) / 2,
				(text.getBlue() + background.getBlue()) / 2);
	}

	private static int roundColorValue(float value) {
		return Math.round(Math.clamp(value, 0, 255));
	}
}
