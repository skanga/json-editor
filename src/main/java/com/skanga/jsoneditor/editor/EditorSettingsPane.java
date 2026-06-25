package com.skanga.jsoneditor.editor;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.skanga.jsoneditor.util.MessageBundle;

/**
 * This class represents the editor settings pane.
 */
public class EditorSettingsPane extends AbstractSettingsPane {
	@Serial
    private final static long serialVersionUID = 4488173853564278813L;
	private final EditorSettings draft;
	private final Locale currentLocale;
	private ComboBoxLocale initialLocaleItem;
	private JCheckBox versionBox;
	private JComboBox<ComboBoxLocale> languageListField;
	private JLabel restartLabel;
	private JCheckBox darkThemeBox;
	private JCheckBox minifyBox;
	private JCheckBox flattenJSONBox;
	private JCheckBox keyNodeClickBox;
	
	public EditorSettingsPane(Editor editor) {
		this(editor.getSettings(), editor.getCurrentLocale());
	}

	EditorSettingsPane(EditorSettings settings, Locale currentLocale) {
		super();
		this.draft = copyPreferences(settings);
		this.currentLocale = currentLocale;
		this.setupUI();
	}
	
	private void setupUI() {
		EditorSettings settings = draft;
		
		// General settings
		JPanel fieldset1 = createFieldset(MessageBundle.get("settings.fieldset.general"));
		
		versionBox = new JCheckBox(MessageBundle.get("settings.checkversion.title"));
		versionBox.setToolTipText(MessageBundle.get("settings.checkversion.tooltip"));
		versionBox.setSelected(settings.isCheckVersionOnStartup());
		versionBox.addChangeListener(e -> settings.setCheckVersionOnStartup(versionBox.isSelected()));
		fieldset1.add(versionBox, createVerticalGridBagConstraints());
		
		ComboBoxLocale currentLocaleItem = findLocaleItem(settings.getEditorLanguage() != null
				? settings.getEditorLanguage()
				: currentLocale);
		initialLocaleItem = currentLocaleItem;
		JPanel languageListPanel = new JPanel(new GridLayout(0, 1));
		JLabel languageListLabel = new JLabel(MessageBundle.get("settings.language.title"));
		languageListField = new JComboBox<>(
				localeComboBoxItems.toArray(new ComboBoxLocale[0]));
		languageListField.setToolTipText(MessageBundle.get("settings.language.tooltip"));
		languageListField.setSelectedItem(currentLocaleItem);
		restartLabel = new JLabel(MessageBundle.get("settings.language.restart"));
		restartLabel.setForeground(Color.GRAY);
		restartLabel.setVisible(false);
		languageListField.addActionListener(e -> {
			settings.setEditorLanguage(languageListField.getItemAt(languageListField.getSelectedIndex()).locale());
			ComboBoxLocale selected = languageListField.getItemAt(languageListField.getSelectedIndex());
			restartLabel.setVisible(initialLocaleItem == null || !selected.locale().equals(initialLocaleItem.locale()));
		});
		languageListPanel.add(languageListLabel);
		languageListPanel.add(languageListField);
		languageListPanel.add(restartLabel);
		fieldset1.add(languageListPanel, createVerticalGridBagConstraints());
		
		// Appearance settings
		JPanel fieldsetAppearance = createFieldset(MessageBundle.get("settings.fieldset.appearance"));

		darkThemeBox = new JCheckBox(MessageBundle.get("settings.darktheme.title"));
		darkThemeBox.setToolTipText(MessageBundle.get("settings.darktheme.tooltip"));
		darkThemeBox.setSelected(settings.isDarkTheme());
		darkThemeBox.addActionListener(e -> settings.setDarkTheme(darkThemeBox.isSelected()));
		fieldsetAppearance.add(darkThemeBox, createVerticalGridBagConstraints());

		// Save settings
		JPanel fieldset2 = createFieldset(MessageBundle.get("settings.fieldset.newprojects"));

		minifyBox = new JCheckBox(MessageBundle.get("settings.minify.title"));
		minifyBox.setToolTipText(MessageBundle.get("settings.minify.tooltip.default"));
		minifyBox.setSelected(settings.isMinifyResources());
		minifyBox.addChangeListener(e -> settings.setMinifyResources(minifyBox.isSelected()));
		fieldset2.add(minifyBox, createVerticalGridBagConstraints());

		flattenJSONBox = new JCheckBox(MessageBundle.get("settings.flattenjson.title"));
		flattenJSONBox.setToolTipText(MessageBundle.get("settings.flattenjson.tooltip.default"));
		flattenJSONBox.setSelected(settings.isFlattenJSON());
		flattenJSONBox.addChangeListener(e -> settings.setFlattenJSON(flattenJSONBox.isSelected()));
		fieldset2.add(flattenJSONBox, createVerticalGridBagConstraints());
		
		// Editing settings
		JPanel fieldset3 = createFieldset(MessageBundle.get("settings.fieldset.editing"));
		
		keyNodeClickBox = new JCheckBox(MessageBundle.get("settings.treetogglemode.title"));
		keyNodeClickBox.setToolTipText(MessageBundle.get("settings.treetogglemode.tooltip"));
		keyNodeClickBox.setSelected(settings.isDoubleClickTreeToggling());
		keyNodeClickBox.addChangeListener(e -> settings.setDoubleClickTreeToggling(keyNodeClickBox.isSelected()));
		fieldset3.add(keyNodeClickBox, createVerticalGridBagConstraints());

		JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 4, 0));
		JButton restoreDefaultsButton = new JButton(MessageBundle.get("settings.restoredefaults.title"));
		restoreDefaultsButton.setToolTipText(MessageBundle.get("settings.restoredefaults.tooltip"));
		restoreDefaultsButton.addActionListener(e -> {
			copyPreferences(defaultPreferences(), draft);
			syncControls();
		});
		actionsPanel.add(restoreDefaultsButton);
		
		setLayout(new GridBagLayout());
		add(fieldset1, createVerticalGridBagConstraints());
		add(fieldsetAppearance, createVerticalGridBagConstraints());
		add(fieldset2, createVerticalGridBagConstraints());
		add(fieldset3, createVerticalGridBagConstraints());
		add(actionsPanel, createVerticalGridBagConstraints());
	}

	void applyTo(Editor editor) {
		boolean darkThemeChanged = editor.getSettings().isDarkTheme() != draft.isDarkTheme();
		applyTo(editor.getSettings());
		if (darkThemeChanged) {
			editor.applyTheme(draft.isDarkTheme());
		}
		editor.updateUI();
	}

	void applyTo(EditorSettings settings) {
		copyPreferences(draft, settings);
	}

	private static EditorSettings copyPreferences(EditorSettings settings) {
		EditorSettings copy = new EditorSettings();
		copyPreferences(settings, copy);
		return copy;
	}

	private static EditorSettings defaultPreferences() {
		EditorSettings defaults = new EditorSettings();
		defaults.setCheckVersionOnStartup(true);
		defaults.setEditorLanguage(Editor.DEFAULT_LANGUAGE);
		defaults.setDarkTheme(false);
		defaults.setMinifyResources(false);
		defaults.setFlattenJSON(false);
		defaults.setDoubleClickTreeToggling(false);
		return defaults;
	}

	private static void copyPreferences(EditorSettings source, EditorSettings target) {
		target.setCheckVersionOnStartup(source.isCheckVersionOnStartup());
		target.setEditorLanguage(source.getEditorLanguage());
		target.setDarkTheme(source.isDarkTheme());
		target.setMinifyResources(source.isMinifyResources());
		target.setFlattenJSON(source.isFlattenJSON());
		target.setDoubleClickTreeToggling(source.isDoubleClickTreeToggling());
	}

	private void syncControls() {
		versionBox.setSelected(draft.isCheckVersionOnStartup());
		languageListField.setSelectedItem(findLocaleItem(draft.getEditorLanguage()));
		ComboBoxLocale selected = languageListField.getItemAt(languageListField.getSelectedIndex());
		restartLabel.setVisible(initialLocaleItem == null || !selected.locale().equals(initialLocaleItem.locale()));
		darkThemeBox.setSelected(draft.isDarkTheme());
		minifyBox.setSelected(draft.isMinifyResources());
		flattenJSONBox.setSelected(draft.isFlattenJSON());
		keyNodeClickBox.setSelected(draft.isDoubleClickTreeToggling());
	}

	private ComboBoxLocale findLocaleItem(Locale locale) {
		for (Locale lookupLocale : localeLookupList(locale, Locale.ENGLISH)) {
			for (ComboBoxLocale item : localeComboBoxItems) {
				if (item.locale().equals(lookupLocale)) {
					return item;
				}
			}
		}
		return null;
	}

	private static List<Locale> localeLookupList(Locale locale, Locale fallback) {
		List<Locale> result = new ArrayList<>();
		result.add(locale);
		String country = locale.getCountry();
		if (!country.isEmpty()) result.add(Locale.of(locale.getLanguage()));
		if (!result.contains(fallback)) result.add(fallback);
		return result;
	}
}
