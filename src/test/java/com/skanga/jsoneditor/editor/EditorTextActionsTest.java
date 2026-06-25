package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

public class EditorTextActionsTest {
	@Test
	public void performsNamedActionFromFocusedTextComponent() {
		JTextField field = new JTextField();
		AtomicInteger calls = new AtomicInteger();
		field.getActionMap().put("undo", new AbstractAction() {
			private static final long serialVersionUID = -8661077809852851059L;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				calls.incrementAndGet();
			}
		});
		
		EditorTextActions.performTextAction(field, new ActionEvent(field, ActionEvent.ACTION_PERFORMED, "undo"), "undo");
		
		assertEquals(1, calls.get());
	}
	
	@Test
	public void ignoresNonTextComponentsAndMissingActions() {
		EditorTextActions.performTextAction(new JLabel(), new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "undo"),
				"undo");
		EditorTextActions.performTextAction(new JTextField(), new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "redo"),
				"redo");
	}
}
