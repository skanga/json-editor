package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.border.TitledBorder;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.MessageBundle;

public class EditorSettingsPaneTest {
	@Test
	public void controlsDoNotMutateSourceSettingsBeforeApply() {
		MessageBundle.loadResources();
		EditorSettings settings = initialSettings();
		EditorSettingsPane pane = new EditorSettingsPane(settings, Locale.ENGLISH);

		toggleEveryCheckBox(pane);
		selectLocale(pane, Locale.of("pt", "BR"));

		assertFalse(settings.isCheckVersionOnStartup());
		assertEquals(Locale.ENGLISH, settings.getEditorLanguage());
		assertFalse(settings.isDarkTheme());
		assertFalse(settings.isMinifyResources());
		assertFalse(settings.isFlattenJSON());
		assertFalse(settings.isDoubleClickTreeToggling());
	}

	@Test
	public void applyCopiesDraftPreferencesToTargetSettings() {
		MessageBundle.loadResources();
		EditorSettings settings = initialSettings();
		EditorSettingsPane pane = new EditorSettingsPane(settings, Locale.ENGLISH);

		toggleEveryCheckBox(pane);
		selectLocale(pane, Locale.of("pt", "BR"));
		pane.applyTo(settings);

		assertTrue(settings.isCheckVersionOnStartup());
		assertEquals(Locale.of("pt", "BR"), settings.getEditorLanguage());
		assertTrue(settings.isDarkTheme());
		assertTrue(settings.isMinifyResources());
		assertTrue(settings.isFlattenJSON());
		assertTrue(settings.isDoubleClickTreeToggling());
	}

	@Test
	public void applyOnlyCopiesPreferencesOwnedByDialog() {
		MessageBundle.loadResources();
		EditorSettings source = initialSettings();
		source.setWindowPositionX(42);
		source.setLastSelectedNode("/before");
		EditorSettingsPane pane = new EditorSettingsPane(source, Locale.ENGLISH);
		EditorSettings target = initialSettings();
		target.setWindowPositionX(7);
		target.setLastSelectedNode("/target");

		toggleEveryCheckBox(pane);
		pane.applyTo(target);

		assertEquals(7, target.getWindowPositionX());
		assertEquals("/target", target.getLastSelectedNode());
	}

	@Test
	public void restoreDefaultsUpdatesDraftAndControlsWithoutMutatingSourceSettings() {
		MessageBundle.loadResources();
		EditorSettings settings = new EditorSettings();
		settings.setCheckVersionOnStartup(false);
		settings.setEditorLanguage(Locale.of("pt", "BR"));
		settings.setDarkTheme(true);
		settings.setMinifyResources(true);
		settings.setFlattenJSON(true);
		settings.setDoubleClickTreeToggling(true);
		EditorSettingsPane pane = new EditorSettingsPane(settings, Locale.of("pt", "BR"));

		findButton(pane, "Restore Defaults").doClick();

		assertFalse(settings.isCheckVersionOnStartup());
		assertEquals(Locale.of("pt", "BR"), settings.getEditorLanguage());
		assertTrue(settings.isDarkTheme());
		assertTrue(settings.isMinifyResources());
		assertTrue(settings.isFlattenJSON());
		assertTrue(settings.isDoubleClickTreeToggling());
		assertControlSelected(pane, "Check for new version on startup", true);
		assertControlSelected(pane, "Dark theme", false);
		assertControlSelected(pane, "Minify JSON files on save", false);
		assertControlSelected(pane, "Flatten JSON keys on save", false);
		assertControlSelected(pane, "Expand and collapse keys using double click", false);

		pane.applyTo(settings);

		assertTrue(settings.isCheckVersionOnStartup());
		assertEquals(Locale.ENGLISH, settings.getEditorLanguage());
		assertFalse(settings.isDarkTheme());
		assertFalse(settings.isMinifyResources());
		assertFalse(settings.isFlattenJSON());
		assertFalse(settings.isDoubleClickTreeToggling());
	}

	@Test
	public void saveSettingsSectionIdentifiesDefaultsForNewJsonFiles() {
		MessageBundle.loadResources();
		EditorSettingsPane pane = new EditorSettingsPane(initialSettings(), Locale.ENGLISH);

		assertTrue(hasTitledSection(pane, "Save defaults for new JSON files"));
	}

	private EditorSettings initialSettings() {
		EditorSettings settings = new EditorSettings();
		settings.setCheckVersionOnStartup(false);
		settings.setEditorLanguage(Locale.ENGLISH);
		settings.setDarkTheme(false);
		settings.setMinifyResources(false);
		settings.setFlattenJSON(false);
		settings.setDoubleClickTreeToggling(false);
		return settings;
	}

	private void toggleEveryCheckBox(Container container) {
		for (Component component : container.getComponents()) {
			if (component instanceof JCheckBox checkBox) {
				checkBox.doClick();
			}
			if (component instanceof Container child) {
				toggleEveryCheckBox(child);
			}
		}
	}

	private void assertControlSelected(Container container, String text, boolean selected) {
		assertEquals(selected, findCheckBox(container, text).isSelected());
	}

	private JCheckBox findCheckBox(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JCheckBox checkBox && text.equals(checkBox.getText())) {
				return checkBox;
			}
			if (component instanceof Container child) {
				JCheckBox checkBox = findCheckBox(child, text);
				if (checkBox != null) {
					return checkBox;
				}
			}
		}
		return null;
	}

	private JButton findButton(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JButton button && text.equals(button.getText())) {
				return button;
			}
			if (component instanceof Container child) {
				JButton button = findButton(child, text);
				if (button != null) {
					return button;
				}
			}
		}
		return null;
	}

	private boolean hasTitledSection(Container container, String title) {
		if (container instanceof javax.swing.JComponent component
				&& component.getBorder() instanceof javax.swing.border.CompoundBorder compoundBorder
				&& compoundBorder.getOutsideBorder() instanceof TitledBorder titledBorder
				&& title.equals(titledBorder.getTitle())) {
			return true;
		}
		for (Component component : container.getComponents()) {
			if (component instanceof Container child && hasTitledSection(child, title)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private void selectLocale(Container container, Locale locale) {
		for (Component component : container.getComponents()) {
			if (component instanceof JComboBox<?> comboBox) {
				for (int i = 0; i < comboBox.getItemCount(); i++) {
					AbstractSettingsPane.ComboBoxLocale item =
							(AbstractSettingsPane.ComboBoxLocale) comboBox.getItemAt(i);
					if (item.locale().equals(locale)) {
						((JComboBox<AbstractSettingsPane.ComboBoxLocale>) comboBox).setSelectedIndex(i);
						return;
					}
				}
			}
			if (component instanceof Container child) {
				selectLocale(child, locale);
			}
		}
	}
}
