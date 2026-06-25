package com.skanga.jsoneditor.editor;

import java.awt.GridBagLayout;
import java.io.Serial;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.skanga.jsoneditor.util.MessageBundle;

/**
 * This class represents the project settings pane.
 */
public class EditorProjectSettingsPane extends AbstractSettingsPane {
	@Serial
    private final static long serialVersionUID = 5665963334924596315L;
	private final EditorProject project;
	
	public EditorProjectSettingsPane(Editor editor) {
		this(editor.getProject());
	}

	EditorProjectSettingsPane(EditorProject project) {
		super();
		this.project = project;
		this.setupUI();
	}
	
	private void setupUI() {
		JPanel fieldset1 = createFieldset(MessageBundle.get("settings.fieldset.currentfile"));

		JCheckBox minifyBox = new JCheckBox(MessageBundle.get("settings.minify.title"));
		minifyBox.setToolTipText(MessageBundle.get("settings.minify.tooltip.project"));
		minifyBox.setSelected(project.isMinifyResources());
		minifyBox.addChangeListener(e -> project.setMinifyResources(minifyBox.isSelected()));
		fieldset1.add(minifyBox, createVerticalGridBagConstraints());

		JCheckBox flattenJSONBox = new JCheckBox(MessageBundle.get("settings.flattenjson.title"));
		flattenJSONBox.setToolTipText(MessageBundle.get("settings.flattenjson.tooltip.project"));
		flattenJSONBox.setSelected(project.isFlattenJSON());
		flattenJSONBox.addChangeListener(e -> project.setFlattenJSON(flattenJSONBox.isSelected()));
		fieldset1.add(flattenJSONBox, createVerticalGridBagConstraints());
		
		setLayout(new GridBagLayout());
		add(fieldset1, createVerticalGridBagConstraints());
	}
}
