package com.skanga.jsoneditor.editor;

import javax.swing.JComponent;

import com.skanga.jsoneditor.util.MessageBundle;

enum EditorCommand {
	NEW("menu.file.project.new.title"),
	OPEN("menu.file.project.import.title"),
	SAVE("menu.file.save.title"),
	CLOSE("menu.file.close.title"),
	UNDO("menu.edit.undo.title"),
	REDO("menu.edit.redo.title"),
	ADD_KEY("menu.edit.add.title"),
	FIND_KEY("menu.edit.find.title"),
	RENAME_KEY("menu.edit.rename.title"),
	DUPLICATE_KEY("menu.edit.duplicate.title"),
	DELETE_KEY("menu.edit.delete.title"),
	COPY_KEY("menu.edit.copy.key.title"),
	EXPAND("menu.view.expand.title"),
	COLLAPSE("menu.view.collapse.title");
	
	static final String CLIENT_PROPERTY = "editorCommand";
	
	private final String messageKey;
	
	EditorCommand(String messageKey) {
		this.messageKey = messageKey;
	}
	
	String text() {
		return MessageBundle.get(messageKey);
	}
	
	void applyTo(JComponent component) {
		component.putClientProperty(CLIENT_PROPERTY, name());
	}
}
