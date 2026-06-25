package com.skanga.jsoneditor.swing;

import java.awt.event.ActionEvent;
import java.io.Serial;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.undo.UndoManager;

public final class UndoRedoActions {
	private UndoRedoActions() {
	}
	
	public static Action undo(UndoManager undoManager) {
		return new UndoRedoAction(undoManager, true);
	}
	
	public static Action redo(UndoManager undoManager) {
		return new UndoRedoAction(undoManager, false);
	}
	
	private static class UndoRedoAction extends AbstractAction {
		@Serial
        private static final long serialVersionUID = -3051499148079684354L;
		private final UndoManager undoManager;
		private final boolean undo;
		
		UndoRedoAction(UndoManager undoManager, boolean undo) {
			this.undoManager = undoManager;
			this.undo = undo;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (undo && undoManager.canUndo()) {
				undoManager.undo();
			} else if (!undo && undoManager.canRedo()) {
				undoManager.redo();
			}
		}
	}
}
