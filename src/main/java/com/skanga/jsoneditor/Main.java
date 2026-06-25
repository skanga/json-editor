package com.skanga.jsoneditor;

import java.awt.Font;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.skanga.jsoneditor.editor.Editor;
import com.skanga.jsoneditor.util.ExtendedProperties;

/**
 * Main entry point
 */
public class Main {
	private final static Logger log = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {
		Optional<Path> startupJsonFile = getStartupJsonFile(args);
		SwingUtilities.invokeLater(() -> {
			setupLookAndFeel(readDarkThemePreference());
			new Editor().launch(startupJsonFile);
		});
	}

	/** Reads the persisted dark-theme preference so the correct theme is installed before the UI is built. */
	static boolean readDarkThemePreference() {
		ExtendedProperties props = new ExtendedProperties();
		props.load(Path.of(Editor.SETTINGS_DIR, Editor.SETTINGS_FILE));
		return props.getBooleanProperty("dark_theme", false);
	}
	
	public static Optional<Path> getStartupJsonFile(String[] args) {
		if (args == null || args.length == 0 || args[0] == null || args[0].isBlank()) {
			return Optional.empty();
		}
		return Optional.of(Path.of(args[0]).toAbsolutePath().normalize());
	}
	
	static void setupLookAndFeel() {
		setupLookAndFeel(false);
	}

	/**
	 * Installs the FlatLaf look and feel for the given theme. Safe to call again at
	 * runtime to switch themes (follow with {@code FlatLaf.updateUI()} to apply live).
	 *
	 * @param	dark whether to use the dark theme.
	 */
	public static void setupLookAndFeel(boolean dark) {
		// Enable global menu on macOS
		if (System.getProperty("os.name","").toLowerCase(Locale.ROOT).startsWith("mac")) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
		try {
			UIManager.setLookAndFeel(dark ? new FlatDarkLaf() : new FlatLightLaf());
			// For windows use menu font for entire UI
			if (System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")) {
				setUIFont(UIManager.getFont("Menu.font"));
			}
		} catch (Exception e) {
			log.warn("Unable to use FlatLeaf look and feel");
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception fallbackError) {
				log.warn("Unable to use native look and feel");
			}
		}
	}
	
	private static void setUIFont(Font font) {
		UIDefaults defaults = UIManager.getDefaults();
		List.of(
			"List.font",
			"TableHeader.font",
			"Panel.font",
			"TextArea.font",
			"ToggleButton.font",
			"ComboBox.font",
			"ScrollPane.font",
			"Spinner.font",
			"Slider.font",
			"EditorPane.font",
			"OptionPane.font",
			"ToolBar.font",
			"Tree.font",
			"TitledBorder.font",
			"Table.font",
			"Label.font",
			"TextField.font",
			"TextPane.font",
			"CheckBox.font",
			"ProgressBar.font",
			"FormattedTextField.font",
			"ColorChooser.font",
			"PasswordField.font",
			"Viewport.font",
			"TabbedPane.font",
			"RadioButton.font",
			"ToolTip.font",
			"Button.font"
		).forEach(key -> defaults.put(key, font));
	}
}
