package com.skanga.jsoneditor.editor;

import java.awt.BasicStroke;

final class EditorIconStyle {
	static final int ICON_SIZE = 24;
	static final float STROKE_WIDTH = 2.4f;
	
	private EditorIconStyle() {
	}
	
	static BasicStroke stroke(float width) {
		return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	}
}
