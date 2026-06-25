package com.skanga.jsoneditor.swing.text;

import java.awt.event.ActionEvent;
import java.io.Serial;

import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

/**
 * An action implementation useful for deleting text.
 */
public class DeleteAction extends TextAction {
	@Serial
    private final static long serialVersionUID = -7933405670677160997L;
	
	public DeleteAction(String name) {
		super(name);
	}
    
    @Override
	public void actionPerformed(ActionEvent e) {
        JTextComponent component = getFocusedComponent();
        component.replaceSelection("");
    }
}
