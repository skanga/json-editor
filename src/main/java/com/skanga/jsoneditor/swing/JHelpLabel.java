package com.skanga.jsoneditor.swing;

import java.awt.Dimension;
import java.awt.Font;
import java.io.Serial;

import javax.swing.JLabel;
import javax.swing.UIManager;

/**
 * This class extends a default {@link javax.swing.JLabel} with a custom look and feel
 * for help messages.
 */
public class JHelpLabel extends JLabel {
	@Serial
    private final static long serialVersionUID = -6879887592161450052L;

	/**
	 * Constructs a {@link JHelpLabel}.
	 */
	public JHelpLabel(String text) {
		super(text);
		
		Dimension size = getSize();
		size.height -= 10;
		setSize(size);
		setFont(getFont().deriveFont(Font.PLAIN, getFont().getSize()-1));
		setForeground(UIManager.getColor("Label.disabledForeground"));
	}
}
