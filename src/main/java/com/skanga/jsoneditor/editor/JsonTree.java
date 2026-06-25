package com.skanga.jsoneditor.editor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.skanga.jsoneditor.util.ResourceKeys;

/**
 * This class represents a tree view for JSON keys.
 */
public class JsonTree extends JTree {
	@Serial
    private final static long serialVersionUID = -2888673305196385241L;
	private static final int MIN_ROW_HEIGHT = 28;
	private static final int SELECTION_ARC = 8;
	private static final int SELECTION_HORIZONTAL_MARGIN = 4;
	private static final float STRIPE_FOREGROUND_BLEND = 0.02f;
	private JsonTreeModel sourceModel;
	private String filterText;
	private String dirtyNodeKey = null;

	public JsonTree() {
		super(new JsonTreeModel());
		setupUI();
	}

	@Override
	public void updateUI() {
		// Keep the custom UI (toggle icons, indents, borderless rows) instead of reverting to the
		// default tree UI, and re-pull the theme colors so the tree tracks light/dark L&F switches
		// at runtime. BasicTreeUI does not install a tree foreground, so set it explicitly here;
		// running on every updateUI() (including theme changes) keeps it current rather than frozen.
		setUI(new JsonTreeUI());
		setForeground(uiColor("Tree.foreground", uiColor("Label.foreground", Color.BLACK)));
		setBackground(uiColor("Tree.background", uiColor("Panel.background", Color.WHITE)));
	}

	public void collapseAll() {
		for (int i = getRowCount() - 1; i >= 0; i--) {
		    collapseRow(i);
		}
	}

	public void expandAll() {
		int row = 0;
		while (row < getRowCount()) {
			expandRow(row++);
		}
	}

	public void expand(List<JsonTreeNode> nodes) {
		nodes.forEach(n -> expandPath(new TreePath(n.getPath())));
	}

	public void collapse(List<JsonTreeNode> nodes) {
		nodes.forEach(n -> collapsePath(new TreePath(n.getPath())));
	}

	public JsonTreeNode addNodeByKey(String key) {
		if (key == null) {
			return null;
		}
		JsonTreeModel model = getSourceModel();
		JsonTreeNode node = model.getNodeByKey(key);
		if (node == null) {
			JsonTreeNode parent = model.getClosestParentNodeByKey(key);
			if (parent == null) {
				return null;
			}
			boolean jsonPath = ResourceKeys.isJsonPath(key);
			String newKey = ResourceKeys.childKey(key, parent.getKey());
			String firstPart = ResourceKeys.firstPart(newKey);
			String lastPart = ResourceKeys.withoutFirstPart(newKey);
			JsonTreeNode newNode = jsonPath
					? new JsonTreeNode(firstPart,
							lastPart.isEmpty() ? new ArrayList<>() : new ArrayList<>(List.of(lastPart)),
							ResourceKeys.createJsonPath(parent.getKey(), firstPart))
					: new JsonTreeNode(firstPart,
							lastPart.isEmpty() ? new ArrayList<>() : new ArrayList<>(List.of(lastPart)));
			newNode.setJsonType(jsonPath ? JsonNodeType.String : JsonNodeType.Unknown);
			model.insertNodeInto(newNode, parent);
			node = model.getNodeByKey(key);
		}
		refreshFilteredModel(key);
		JsonTreeNode visibleNode = getVisibleNodeByKey(key);
		if (visibleNode != null) {
			setSelectionNode(visibleNode);
			return visibleNode;
		}
		return node;
	}

	public void updateNodeJsonType(String key, JsonNodeType type) {
		JsonTreeModel model = getSourceModel();
		JsonTreeNode node = model.getNodeByKey(key);
		if (node != null) {
			node.setJsonType(type);
			model.nodeChanged(node);
			refreshFilteredModel(key);
		}
	}

	public void removeNodeByKey(String key) {
		JsonTreeModel model = getSourceModel();
		JsonTreeNode node = model.getNodeByKey(key);
		if (node != null) {
			model.removeNodeFromParent(node);
			refreshFilteredModel();
		}
	}

	public JsonTreeNode getNodeByKey(String key) {
		return getSourceModel().getNodeByKey(key);
	}

	public List<JsonTreeNode> getExpandedNodes() {
		JsonTreeNode node = (JsonTreeNode) getModel().getRoot();
		return getExpandedNodes(node);
	}

	public List<JsonTreeNode> getExpandedNodes(JsonTreeNode node) {
		List<JsonTreeNode> expandedNodes = new LinkedList<>();
		Enumeration<TreePath> expandedChildren = getExpandedDescendants(new TreePath(node.getPath()));
		if (expandedChildren != null) {
			while (expandedChildren.hasMoreElements()) {
				TreePath path = expandedChildren.nextElement();
				JsonTreeNode expandedNode = (JsonTreeNode) path.getLastPathComponent();
				if (!expandedNode.isRoot()) { // do not return the root node
					expandedNodes.add(expandedNode);
				}
			}
		}
		return expandedNodes;
	}

	public void renameNodeByKey(String key, String newKey) {
		duplicateNodeByKey(key, newKey, false);
	}

	public void duplicateNodeByKey(String key, String newKey) {
		duplicateNodeByKey(key, newKey, true);
	}

	public JsonTreeNode getSelectionNode() {
		return (JsonTreeNode) getLastSelectedPathComponent();
	}

	@Override
	public void setSelectionPath(TreePath path) {
		super.setSelectionPath(path);
		if (path != null) {
			scrollPathToVisible(path);
		}
	}

	@Override
	public void setSelectionRow(int row) {
		TreePath path = getPathForRow(row);
		setSelectionPath(path);
	}

	public void setSelectionNode(JsonTreeNode node) {
		if (node == null) {
			setSelectionPath(null);
			return;
		}
		JsonTreeNode visibleNode = getVisibleNodeByKey(node.getKey());
		if (visibleNode != null) {
			node = visibleNode;
		} else if (!getFilterText().isEmpty()) {
			return;
		}
		TreePath path = new TreePath(node.getPath());
		setSelectionPath(path);
	}

	public void clear() {
		setModel(new JsonTreeModel());
	}

	public void setFilterText(String filterText) {
		String newFilterText = filterText == null ? "" : filterText;
		boolean filterChanged = !newFilterText.equals(getFilterText());
		this.filterText = newFilterText;
		String selectedKey = getSelectionNode() == null ? null : getSelectionNode().getKey();
		refreshFilteredModel(selectedKey, !filterChanged);
	}

	public String getFilterText() {
		return filterText == null ? "" : filterText;
	}

	public int getVisibleLeafCount() {
		int count = 0;
		for (int i = 0; i < getRowCount(); i++) {
			javax.swing.tree.TreePath path = getPathForRow(i);
			if (path != null && ((JsonTreeNode) path.getLastPathComponent()).isLeaf()) {
				count++;
			}
		}
		return count;
	}

	public void setDirtyNode(String key) {
		this.dirtyNodeKey = key;
		repaint();
	}

	public boolean isDirtyNode(JsonTreeNode node) {
		return node != null && node.getKey() != null && node.getKey().equals(dirtyNodeKey);
	}

	public JsonTreeModel getSourceModel() {
		if (sourceModel == null) {
			sourceModel = (JsonTreeModel) getModel();
		}
		return sourceModel;
	}

	@Override
	public void setModel(TreeModel newModel) {
		if (newModel instanceof JsonTreeModel) {
			sourceModel = (JsonTreeModel) newModel;
			super.setModel(getFilteredModel());
			if (!getFilterText().isEmpty()) {
				expandAll();
			}
			return;
		}
		super.setModel(newModel);
	}

	@Override
	protected void paintComponent(Graphics g) {
		JsonTreeCellRenderer renderer = (JsonTreeCellRenderer) getCellRenderer();
		Color c1 = renderer.getSelectionBackground();

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        paintRowStripes(g2);

        int[] selectionRows = getSelectionRows();
        if (selectionRows != null) {
        	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	        for (int i : selectionRows) {
	        	Rectangle r = getRowBounds(i);
	        	if (r != null) {
		        	g2.setColor(c1);
		        	g2.fillRoundRect(SELECTION_HORIZONTAL_MARGIN, r.y + 2,
		        			Math.max(0, getWidth() - (SELECTION_HORIZONTAL_MARGIN * 2)),
		        			Math.max(0, r.height - 4), SELECTION_ARC, SELECTION_ARC);
	        	}
	        }
        }
        g2.dispose();

        boolean opaque = isOpaque();
        setOpaque(false);
        super.paintComponent(g);
        setOpaque(opaque);
    }

	private void paintRowStripes(Graphics2D g2) {
		Color stripeColor = subtleStripeColor();
		for (int row = 1; row < getRowCount(); row += 2) {
			Rectangle bounds = getRowBounds(row);
			if (bounds != null) {
				g2.setColor(stripeColor);
				g2.fillRect(0, bounds.y, getWidth(), bounds.height);
			}
		}
	}

	private Color subtleStripeColor() {
		Color background = getBackground();
		Color foreground = getForeground();
		return new Color(
				blend(background.getRed(), foreground.getRed()),
				blend(background.getGreen(), foreground.getGreen()),
				blend(background.getBlue(), foreground.getBlue()),
				background.getAlpha());
	}

	private int blend(int background, int foreground) {
		return Math.round(background + ((foreground - background) * STRIPE_FOREGROUND_BLEND));
	}

	private void setupUI() {
		UIManager.put("Tree.repaintWholeRow", Boolean.TRUE);

		// Remove all keystrokes
		InputMap inputMap = getInputMap().getParent();
		for (KeyStroke k : getRegisteredKeyStrokes()) {
			inputMap.remove(k);
		}
		setupKeyboardNavigation();

        setUI(new JsonTreeUI());
		setCellRenderer(new JsonTreeCellRenderer());
		ToolTipManager.sharedInstance().registerComponent(this);
		addTreeWillExpandListener(new JsonTreeExpandListener());
		addMouseListener(new JsonTreeMouseListener());
		// Colors are left to the look and feel (installed via JsonTreeUI) so they track
		// light/dark theme switches; pinning them here would freeze the original theme.
		setRowHeight(resolveRowHeight());
		setEditable(false);
		setOpaque(true);
		setRootVisible(true);
		setShowsRootHandles(false);
		setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
		putClientProperty("JTree.lineStyle", "None");
	}

	private void setupKeyboardNavigation() {
		bindNavigationKey(KeyEvent.VK_UP, "selectPreviousRow", -1);
		bindNavigationKey(KeyEvent.VK_DOWN, "selectNextRow", 1);
		bindNavigationKey(KeyEvent.VK_HOME, "selectFirstRow", Integer.MIN_VALUE);
		bindNavigationKey(KeyEvent.VK_END, "selectLastRow", Integer.MAX_VALUE);
		bindTreeActionKey(KeyEvent.VK_LEFT, "collapseSelectedRow", this::collapseSelectedRow);
		bindTreeActionKey(KeyEvent.VK_RIGHT, "expandSelectedRow", this::expandSelectedRow);
		bindTreeActionKey(KeyEvent.VK_ENTER, "toggleSelectedRow", this::toggleSelectedRow);
	}

	private void bindNavigationKey(int keyCode, String actionName, int direction) {
		bindTreeActionKey(keyCode, actionName, () -> selectVisibleRow(direction));
	}

	private void bindTreeActionKey(int keyCode, String actionName, Runnable action) {
		getInputMap().put(KeyStroke.getKeyStroke(keyCode, 0), actionName);
		getActionMap().put(actionName, new AbstractAction() {
			@Serial
            private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	private void selectVisibleRow(int direction) {
		int rowCount = getRowCount();
		if (rowCount == 0) {
			return;
		}
		int selectedRow = getSelectionRows() == null ? 0 : getSelectionRows()[0];
		if (direction == Integer.MIN_VALUE) {
			setSelectionRow(0);
		} else if (direction == Integer.MAX_VALUE) {
			setSelectionRow(rowCount - 1);
		} else {
			setSelectionRow(Math.clamp(selectedRow + direction, 0, rowCount - 1));
		}
	}

	private void collapseSelectedRow() {
		TreePath selected = getSelectionPath();
		if (selected == null || selected.getPathCount() == 1) {
			return;
		}
		int row = getRowForPath(selected);
		if (row >= 0 && !isCollapsed(row)) {
			collapseRow(row);
		}
	}

	private void expandSelectedRow() {
		TreePath selected = getSelectionPath();
		if (selected == null) {
			return;
		}
		int row = getRowForPath(selected);
		if (row >= 0 && isCollapsed(row)) {
			expandRow(row);
		}
	}

	private void toggleSelectedRow() {
		TreePath selected = getSelectionPath();
		if (selected == null || selected.getPathCount() == 1) {
			return;
		}
		int row = getRowForPath(selected);
		if (row < 0) {
			return;
		}
		if (isCollapsed(row)) {
			expandRow(row);
		} else if (!((JsonTreeNode) selected.getLastPathComponent()).isLeaf()) {
			collapseRow(row);
		}
	}
	
	private int resolveRowHeight() {
		int uiRowHeight = UIManager.getInt("Tree.rowHeight");
		if (uiRowHeight <= 0) {
			uiRowHeight = getFontMetrics(getFont()).getHeight() + 8;
		}
		return Math.max(MIN_ROW_HEIGHT, uiRowHeight);
	}
	
	private static Color uiColor(String key, Color fallback) {
		Color color = UIManager.getColor(key);
		return color == null ? fallback : color;
	}

	private void duplicateNodeByKey(String key, String newKey, boolean keepOld) {
		JsonTreeModel model = getSourceModel();
		JsonTreeNode node = model.getNodeByKey(key);
		if (node == null) {
			return;
		}
		JsonTreeNode newNode = model.getNodeByKey(newKey);
		List<JsonTreeNode> expandedNodes = null;

		if (keepOld) {
			node = node.cloneWithChildren();
		} else {
			expandedNodes = getExpandedNodes(node);
			model.removeNodeFromParent(node);
		}

		if (node.isLeaf() && newNode != null) {
			model.removeNodeFromParent(newNode);
			newNode = null;
		}
		if (newNode != null) {
			model.insertDescendantsInto(node, newNode);
			node = newNode;
		} else {
			JsonTreeNode parent = addNodeByKey(ResourceKeys.withoutLastPart(newKey));
			if (parent == null) {
				return;
			}
			node.setName(ResourceKeys.lastPart(newKey));
			if (ResourceKeys.isJsonPath(newKey)) {
				node.updateJsonPath(parent.getKey());
			}
			model.insertNodeInto(node, parent);
		}

		if (expandedNodes != null) {
			expand(expandedNodes);
		}

		refreshFilteredModel(node.getKey());
		JsonTreeNode visibleNode = getVisibleNodeByKey(node.getKey());
		if (visibleNode != null) {
			setSelectionNode(visibleNode);
		}
	}

	private JsonTreeModel getFilteredModel() {
		return sourceModel.filterByNodeName(getFilterText());
	}

	private void refreshFilteredModel() {
		refreshFilteredModel(null);
	}

	private void refreshFilteredModel(String selectedKey) {
		refreshFilteredModel(selectedKey, true);
	}

	private void refreshFilteredModel(String selectedKey, boolean preserveFilteredCollapseState) {
		Set<String> collapsedNodeKeys = preserveFilteredCollapseState && !getFilterText().isEmpty()
				? getCollapsedVisibleNodeKeys()
				: Set.of();
		super.setModel(getFilteredModel());
		if (!getFilterText().isEmpty()) {
			expandAll();
			collapseVisibleNodes(collapsedNodeKeys);
		}
		if (selectedKey != null) {
			JsonTreeNode visibleNode = getVisibleNodeByKey(selectedKey);
			if (visibleNode != null) {
				setSelectionNode(visibleNode);
			}
		}
	}

	private Set<String> getCollapsedVisibleNodeKeys() {
		Set<String> keys = new HashSet<>();
		for (int i = 0; i < getRowCount(); i++) {
			TreePath path = getPathForRow(i);
			JsonTreeNode node = (JsonTreeNode) path.getLastPathComponent();
			if (node.getChildCount() > 0 && isCollapsed(path)) {
				keys.add(node.getKey());
			}
		}
		return keys;
	}

	private void collapseVisibleNodes(Set<String> keys) {
		for (String key : keys) {
			JsonTreeNode node = getVisibleNodeByKey(key);
			if (node != null) {
				collapsePath(new TreePath(node.getPath()));
			}
		}
	}

	private JsonTreeNode getVisibleNodeByKey(String key) {
		JsonTreeModel visibleModel = (JsonTreeModel) getModel();
		return visibleModel.getNodeByKey(key);
	}

	private class JsonTreeMouseListener extends MouseAdapter {
		private boolean isPopupTrigger;

		@Override
		public void mousePressed(MouseEvent e) {
			isPopupTrigger = e.isPopupTrigger();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (!isPopupTrigger && !e.isPopupTrigger() && e.getClickCount() == getToggleClickCount()) {
				int row = getRowForLocation(e.getX(), e.getY());
				if (row < 0) {
					return;
				}
				if (isCollapsed(row)) {
					expandRow(row);
				} else {
					collapseRow(row);
				}
			}
		}
	}

	private static class JsonTreeExpandListener implements TreeWillExpandListener {
		@Override
		public void treeWillExpand(TreeExpansionEvent e) {}

		@Override
    	public void treeWillCollapse(TreeExpansionEvent e) throws ExpandVetoException {
			// Prevent root key from being collapsed
    		if (e.getPath().getPathCount() == 1) {
    			throw new ExpandVetoException(e);
    		}
    	}
	}
}
