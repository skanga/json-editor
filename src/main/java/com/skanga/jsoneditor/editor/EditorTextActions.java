package com.skanga.jsoneditor.editor;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.text.JTextComponent;

final class EditorTextActions {
	private EditorTextActions() {
	}
	
	static void performUndo(ActionEvent event) {
		performFocusedTextAction(event, "undo");
	}
	
	static void performRedo(ActionEvent event) {
		performFocusedTextAction(event, "redo");
	}
	
	static void performFocusedTextAction(ActionEvent event, String actionName) {
		performTextAction(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), event, actionName);
	}
	
	static void performTextAction(Component focused, ActionEvent event, String actionName) {
		if (focused instanceof JTextComponent textComponent) {
			Action action = textComponent.getActionMap().get(actionName);
			if (action != null) {
				action.actionPerformed(event);
			}
		}
	}
}
