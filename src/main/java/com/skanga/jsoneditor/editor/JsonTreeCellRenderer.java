package com.skanga.jsoneditor.editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseListener;
import java.io.Serial;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.skanga.jsoneditor.util.Images;

/**
 * This class represents a default cell renderer for the JSON tree.
 */
public class JsonTreeCellRenderer extends DefaultTreeCellRenderer {
	@Serial
	private final static long serialVersionUID = 3511394180407171920L;
	private final static ImageIcon ROOT_ICON = Images.loadFromClasspath("images/icon-folder.png");
	private static final int ROOT_NAME_LEFT_GAP = 4;
	private static final int TYPE_ICON_LEFT_GAP = 1;
	private final JPanel nodePanel = new JPanel();
	private final JLabel nodeStatusLabel = new JLabel();
	private final JLabel nodeTextLabel = new JLabel();
	private final JLabel nodeTypeLabel = new JLabel();

	public JsonTreeCellRenderer() {
		super();
		setLeafIcon(null);
		setClosedIcon(null);
		setOpenIcon(null);
		for (MouseListener l : getMouseListeners()) {
			removeMouseListener(l);
		}
		nodePanel.setLayout(new BoxLayout(nodePanel, BoxLayout.X_AXIS));
		nodePanel.setOpaque(false);
		nodeStatusLabel.setOpaque(false);
		nodeTextLabel.setOpaque(false);
		nodeTypeLabel.setOpaque(false);
		nodeTypeLabel.setBorder(new EmptyBorder(0, TYPE_ICON_LEFT_GAP, 0, 0));
		nodePanel.add(nodeStatusLabel);
		nodePanel.add(nodeTextLabel);
		nodePanel.add(nodeTypeLabel);
	}

	public Color getSelectionBackground() {
		return selectionBackground();
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
		JsonTreeNode node = (JsonTreeNode) value;
        JLabel l = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        // Read theme colors live so the tree tracks light/dark L&F switches at runtime.
        Color background = selected ? selectionBackground() : treeBackground();
        Color foreground = selected ? selectionForeground() : treeForeground();

        if (!node.isRoot()) {
            nodePanel.setBackground(background);

            // nodeStatusLabel is an empty 24px gutter that aligns non-root rows with the
            // root row's folder-icon column.
            nodeStatusLabel.setPreferredSize(new Dimension(24, l.getPreferredSize().height));
            nodeStatusLabel.setForeground(foreground);
            nodeStatusLabel.setFont(tree.getFont());

            nodeTextLabel.setText(l.getText());
            boolean nodeDirty = (tree instanceof JsonTree) && ((JsonTree) tree).isDirtyNode(node);
            nodeTextLabel.setForeground(nodeDirty && !selected ? dirtyColor() : foreground);
            nodeTextLabel.setFont(tree.getFont());

            String typeLabel = getTypeTooltip(node);
            nodeTypeLabel.setIcon(createTypeIcon(node, selected ? foreground : null));
            nodeTypeLabel.setToolTipText(typeLabel);
            nodeTypeLabel.setForeground(foreground);
            nodeTypeLabel.setFont(tree.getFont());
            nodePanel.setToolTipText(typeLabel);

            return nodePanel;
        }

        // Root node: create a fresh panel each time (rendered rarely)
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.setBackground(background);

        JLabel status = createLabel(tree, background, foreground);
        status.setIcon(ROOT_ICON);
        status.setPreferredSize(new Dimension(24, l.getPreferredSize().height));
        panel.add(status);

        JLabel text = createLabel(tree, background, foreground);
        text.setText(l.getText());
        text.setBorder(new EmptyBorder(0, ROOT_NAME_LEFT_GAP, 0, 0));
        panel.add(text);

        JLabel type = createLabel(tree, background, foreground);
        type.setBorder(new EmptyBorder(0, TYPE_ICON_LEFT_GAP, 0, 0));
        String typeLabel = getTypeTooltip(node);
        type.setIcon(createTypeIcon(node, selected ? foreground : null));
        type.setToolTipText(typeLabel);
        panel.setToolTipText(typeLabel);
        panel.add(type);

        return panel;
    }

	private JLabel createLabel(JTree tree, Color background, Color foreground) {
		JLabel label = new JLabel();
		label.setOpaque(false);
		label.setForeground(foreground);
		label.setBackground(background);
		label.setFont(tree.getFont());
		return label;
	}

	private Icon createTypeIcon(JsonTreeNode node, Color foreground) {
		if (node.getJsonType() == JsonNodeType.Array || node.getJsonType() == JsonNodeType.Object) {
			return new JsonTypeIcon(node.getJsonType(), node.getJsonChildCount(), foreground);
		}
		return new JsonTypeIcon(node.getJsonType(), foreground);
	}

	private String getTypeTooltip(JsonTreeNode node) {
		if (node.getJsonType() == JsonNodeType.Array) {
			int childCount = node.getJsonChildCount();
			return String.format("Array (%d %s)", childCount, childCount == 1 ? "item" : "items");
		}
		if (node.getJsonType() == JsonNodeType.Object) {
			int childCount = node.getJsonChildCount();
			return String.format("Object (%d %s)", childCount,
					childCount == 1 ? "property" : "properties");
		}
		return node.getJsonType().getLabel();
	}
	
	private static Color uiColor(String key, Color fallback) {
		Color color = UIManager.getColor(key);
		return color == null ? fallback : color;
	}

	private static Color treeForeground() {
		return uiColor("Tree.foreground", uiColor("Label.foreground", Color.BLACK));
	}

	private static Color treeBackground() {
		return uiColor("Tree.background", uiColor("Panel.background", Color.WHITE));
	}

	private static Color selectionForeground() {
		return uiColor("Tree.selectionForeground", treeForeground());
	}

	private static Color selectionBackground() {
		return uiColor("Tree.selectionBackground", uiColor("Panel.background", Color.LIGHT_GRAY));
	}

	private static Color dirtyColor() {
		Color color = UIManager.getColor("Component.warning.focusedBorderColor");
		if (color == null) {
			color = UIManager.getColor("Actions.Yellow");
		}
		return color == null ? new Color(188, 107, 0) : color;
	}
}
