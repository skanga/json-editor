package com.skanga.jsoneditor.swing.text;

import javax.swing.UIManager;
import javax.swing.text.DefaultCaret;
import java.io.Serial;

/**
 * This class extends the {@link DefaultCaret} with a default blink rate set.
 */
public class BlinkCaret extends DefaultCaret {
	@Serial
    private final static long serialVersionUID = -3365578081904749196L;
	
	public BlinkCaret() {
		int blinkRate = 0;
		Object o = UIManager.get("TextArea.caretBlinkRate");
		if (o instanceof Integer) {
			blinkRate = (Integer) o;
		}
		setBlinkRate(blinkRate);		
	}
}
