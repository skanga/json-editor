package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;

import javax.swing.JComponent;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.MessageBundle;

public class EditorProjectSettingsPaneTest {
	@Test
	public void saveSettingsSectionIdentifiesCurrentJsonFileScope() {
		MessageBundle.loadResources();
		EditorProjectSettingsPane pane = new EditorProjectSettingsPane(new EditorProject(Path.of("file.json")));

		assertTrue(hasTitledSection(pane, "Save settings for this JSON file"));
	}

	private boolean hasTitledSection(Container container, String title) {
		if (container instanceof JComponent component
				&& component.getBorder() instanceof CompoundBorder compoundBorder
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
}
