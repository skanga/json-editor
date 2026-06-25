package com.skanga.jsoneditor.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.undo.UndoManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.editor.JsonKeyField;
import com.skanga.jsoneditor.util.MessageBundle;

public class SwingComponentTest {
	@BeforeEach
	public void loadMessages() {
		MessageBundle.loadResources();
	}
	
	@Test
	public void textFieldSupportsUndoAndRedoActions() {
		JTextField field = new JTextField();
		field.setText("Ada");
		
		field.getActionMap().get("undo").actionPerformed(new ActionEvent(field, ActionEvent.ACTION_PERFORMED, "undo"));
		assertEquals("", field.getText());
		
		field.getActionMap().get("redo").actionPerformed(new ActionEvent(field, ActionEvent.ACTION_PERFORMED, "redo"));
		assertEquals("Ada", field.getText());
	}
	
	@Test
	public void undoRedoActionsOperateOnUndoManager() throws BadLocationException {
		UndoManager undoManager = new UndoManager();
		PlainDocument document = new PlainDocument();
		document.addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
		document.insertString(0, "Ada", null);
		
		Action undo = UndoRedoActions.undo(undoManager);
		Action redo = UndoRedoActions.redo(undoManager);
		
		undo.actionPerformed(new ActionEvent(document, ActionEvent.ACTION_PERFORMED, "undo"));
		assertEquals("", document.getText(0, document.getLength()));
		
		redo.actionPerformed(new ActionEvent(document, ActionEvent.ACTION_PERFORMED, "redo"));
		assertEquals("Ada", document.getText(0, document.getLength()));
	}
	
	@Test
	public void textAreaConfiguresEditingSupportAndOpacityWithEnabledState() {
		JTextArea textArea = new JTextArea();
		
		assertTrue(textArea.getLineWrap());
		assertTrue(textArea.getWrapStyleWord());
		assertNotNull(textArea.getActionMap().get("undo"));
		assertNotNull(textArea.getActionMap().get("redo"));
		
		textArea.setEnabled(false);
		assertFalse(textArea.isOpaque());
		textArea.setEnabled(true);
		assertTrue(textArea.isOpaque());
	}
	
	@Test
	public void jsonKeyFieldTrimsAndClearsValues() {
		JsonKeyField field = new JsonKeyField();
		
		field.setValue("  /users/0/name  ");
		assertEquals("/users/0/name", field.getValue());
		
		field.clear();
		assertEquals("", field.getText());
	}
	
	@Test
	public void scrollablePanelTracksViewportHeightWhenContentIsShorter() {
		JScrollablePanel panel = new JScrollablePanel(true, false);
		panel.setPreferredSize(new Dimension(100, 100));
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setSize(200, 300);
		scrollPane.doLayout();
		
		assertTrue(panel.getScrollableTracksViewportHeight());
	}
	
	@Test
	public void scrollablePanelDoesNotTrackViewportHeightWhenContentIsTaller() {
		JScrollablePanel panel = new JScrollablePanel(true, false);
		panel.setPreferredSize(new Dimension(100, 500));
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setSize(200, 300);
		scrollPane.doLayout();
		
		assertFalse(panel.getScrollableTracksViewportHeight());
	}
	
}
