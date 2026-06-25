package com.skanga.jsoneditor.editor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * This class represents a toggle icon for a JSON tree cell.
 */
public class JsonTreeToggleIcon implements Icon {
    private final static int SIZE = 16;
    private final ToggleIconType type;

    public enum ToggleIconType {
    	Collapsed, Expanded
    }

    public JsonTreeToggleIcon(ToggleIconType type) {
        this.type = type;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
    	Graphics2D g2 = (Graphics2D) g.create();
    	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    	g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    	g2.setColor(iconColor());
        if (type == ToggleIconType.Collapsed) {
        	g2.drawLine(x + 6, y + 4, x + 10, y + 8);
        	g2.drawLine(x + 10, y + 8, x + 6, y + 12);
        } else {
        	g2.drawLine(x + 4, y + 6, x + 8, y + 10);
        	g2.drawLine(x + 8, y + 10, x + 12, y + 6);
    	}
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
    
    private Color iconColor() {
    	String key = type == ToggleIconType.Collapsed ? "Tree.icon.collapsedColor" : "Tree.icon.expandedColor";
    	Color color = UIManager.getColor(key);
    	if (color == null) {
    		color = UIManager.getColor("Tree.foreground");
    	}
    	return color == null ? UIManager.getColor("Label.foreground") : color;
    }
}
