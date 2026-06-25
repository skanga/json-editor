package com.skanga.jsoneditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.util.Optional;

import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

public class MainTest {
	@Test
	public void startupJsonFileUsesFirstCliArgument() {
		Optional<Path> path = Main.getStartupJsonFile(new String[] {"example.json"});
		
		assertEquals(Path.of("example.json").toAbsolutePath().normalize(), path.get());
	}
	
	@Test
	public void startupJsonFileIsEmptyWithoutArguments() {
		assertFalse(Main.getStartupJsonFile(new String[0]).isPresent());
	}
	
	@Test
	public void startupJsonFileIsEmptyForNullOrBlankArguments() {
		assertFalse(Main.getStartupJsonFile(null).isPresent());
		assertFalse(Main.getStartupJsonFile(new String[] {null}).isPresent());
		assertFalse(Main.getStartupJsonFile(new String[] {"  "}).isPresent());
	}
	
	@Test
	public void setupLookAndFeelUsesFlatLaf() {
		Main.setupLookAndFeel();

		assertEquals("com.formdev.flatlaf.FlatLightLaf", UIManager.getLookAndFeel().getClass().getName());
	}

	@Test
	public void setupLookAndFeelInstallsDarkOrLightFlatLaf() {
		Main.setupLookAndFeel(true);
		assertEquals("com.formdev.flatlaf.FlatDarkLaf", UIManager.getLookAndFeel().getClass().getName());

		Main.setupLookAndFeel(false);
		assertEquals("com.formdev.flatlaf.FlatLightLaf", UIManager.getLookAndFeel().getClass().getName());
	}
}
