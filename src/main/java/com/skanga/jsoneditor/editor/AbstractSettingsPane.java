package com.skanga.jsoneditor.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * This class represents an abstract base class for all setting panes.
 */
public abstract class AbstractSettingsPane extends JPanel {
	@Serial
    private final static long serialVersionUID = -8953194193840198893L;
	private final GridBagConstraints vGridBagConstraints;
	
	protected final List<ComboBoxLocale> localeComboBoxItems = Editor.SUPPORTED_LANGUAGES.stream()
			.map(ComboBoxLocale::new)
			.sorted()
			.collect(Collectors.toList());
	
	protected AbstractSettingsPane() {
		super();
		vGridBagConstraints = new GridBagConstraints();
		vGridBagConstraints.insets = new Insets(4,4,4,4);
		vGridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		vGridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		vGridBagConstraints.weightx = 1;
	}
	
	protected GridBagConstraints createVerticalGridBagConstraints() {
		vGridBagConstraints.gridy = (vGridBagConstraints.gridy + 1) % Integer.MAX_VALUE;
		return vGridBagConstraints;
	}
	
	protected JPanel createFieldset(String title) {
		JPanel fieldset = new JPanel(new GridBagLayout());
		fieldset.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(null, title), 
				BorderFactory.createEmptyBorder(5,5,5,5)));
		return fieldset;
	}

	protected record ComboBoxLocale(Locale locale) implements Comparable<ComboBoxLocale> {

		public String toString() {
				return locale.getDisplayName();
			}

			@Override
			public int compareTo(ComboBoxLocale o) {
				return toString().compareTo(o.toString());
			}
		}
}
