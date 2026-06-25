package com.skanga.jsoneditor.editor;

import com.skanga.jsoneditor.swing.JTextField;

import java.io.Serial;

/**
 * This class represents a text field for displaying, finding and adding a JSON key.
 */
public class JsonKeyField extends JTextField {
	@Serial
    private final static long serialVersionUID = -3951187528785224704L;

	public JsonKeyField() {
		super();
		setupUI();
	}

	public void clear() {
		setValue(null);
	}

	public String getValue() {
		return getText().trim();
	}

	public void setValue(String value) {
		setText(value);
		undoManager.discardAllEdits();
	}

	private void setupUI() {
		setEditable(false);
		setCaret(new JsonKeyCaret());
		getCaret().setBlinkRate(0);
	}
}
