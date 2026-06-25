package com.skanga.jsoneditor.swing.text;

import java.awt.event.ActionEvent;
import java.io.Serial;

import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

/**
 * An action implementation useful for selecting all text.
 */
public class SelectAllAction extends TextAction {
	@Serial
    private final static long serialVersionUID = -4913270947629733919L;
	
	public SelectAllAction(String name) {
		super(name);
	}
    
    @Override
	public void actionPerformed(ActionEvent e) {
        JTextComponent component = getFocusedComponent();
        component.selectAll();
    }
}
