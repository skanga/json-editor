package com.skanga.jsoneditor.editor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.KeyStroke;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import com.formdev.flatlaf.FlatClientProperties;
import com.skanga.jsoneditor.model.JsonDocument;
import com.skanga.jsoneditor.util.Colors;
import com.skanga.jsoneditor.util.MessageBundle;
import com.skanga.jsoneditor.util.ResourceKeys;

/**
 * Right-side JSON node inspector with typed and raw literal editing modes.
 */
public class JsonValueEditorPanel extends JPanel {
	@Serial
	private static final long serialVersionUID = -489150771693850403L;
	private static final Pattern JSONPATH_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
	private static final Color STATUS_FALLBACK_COLOR = new Color(210, 112, 0);
	private static final String APPLY_BUTTON_STYLE
			= "background: #178741;"
			+ "foreground: #ffffff;"
			+ "borderColor: #178741";
	private static final String EDITOR_SCROLL_PANE_STYLE
			= "focusedBorderColor: #5f86b8;"
			+ "focusWidth: 1;"
			+ "innerFocusWidth: 0";
	private static final String CARD_TEXT = "text";
	private static final String CARD_NUMBER = "number";
	private static final String CARD_BOOLEAN = "boolean";
	private static final String CARD_EMPTY = "empty";
	private static final String EDITOR_TYPED = "typed";
	private static final String EDITOR_RAW = "raw";
	
	public interface Actions {
		void setLiteral(String key, String literal);
		boolean changeType(String key, JsonNodeType type);
		void addChild(String key);
		void rename(String key);
		void duplicate(String key);
		void delete(String key);
		void moveUp(String key);
		void moveDown(String key);
	}
	
	private final Actions actions;
	private final JLabel titleLabel = new JLabel("No selection");
	private final JLabel typeBadge = new JLabel() {
		@Override protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(getBackground());
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
			g2.dispose();
			super.paintComponent(g);
		}
	};
	private final JTextField pathField = new JTextField();
	private final JButton copyPathButton = new JButton("Copy Path");
	private final JComboBox<JsonNodeType> typeCombo = new JComboBox<>(new JsonNodeType[] {
			JsonNodeType.String, JsonNodeType.Number, JsonNodeType.Boolean,
			JsonNodeType.Null, JsonNodeType.Object, JsonNodeType.Array
	});
	private final JLabel summaryLabel = new JLabel();
	private final JLabel actionsLabel = createSectionLabel("ACTIONS");
	private final JLabel valueLabel = createSectionLabel("VALUE");
	private final JToggleButton typedModeButton = new JToggleButton("Typed", true);
	private final JToggleButton rawModeButton = new JToggleButton("Raw");
	private final JCheckBox wrapTextBox = new JCheckBox(MessageBundle.get("jsonvalue.wrap.title"));
	private final JTextField findField = new JTextField(10);
	private final JButton findPreviousButton = new JButton(new ActionIcon(ActionIcon.Kind.UP));
	private final JButton findNextButton = new JButton(new ActionIcon(ActionIcon.Kind.DOWN));
	private final JLabel findMatchLabel = new JLabel("0 / 0");
	private final JPanel findControlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
	private final CardLayout cardLayout = new CardLayout();
	private final CardLayout editorCardLayout = new CardLayout();
	private final JPanel valueCards = new VisibleCardPanel(cardLayout);
	private final JPanel editorCards = new VisibleCardPanel(editorCardLayout);
	private final JTextArea typedText = new JTextArea(8, 24);
	private final JScrollPane typedScrollPane = new JScrollPane(typedText);
	private final JTextField typedNumber = new JTextField(12);
	private final JPanel typedBooleanPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
	private final JRadioButton typedBooleanTrue = new JRadioButton("true");
	private final JRadioButton typedBooleanFalse = new JRadioButton("false");
	private final JLabel emptyValue = new JLabel("");
	private final JTextArea rawText = new JTextArea(8, 24);
	private final JScrollPane rawScrollPane = new JScrollPane(rawText);
	private final JButton applyButton = new JButton("Apply Value");
	private final JButton revertButton = new JButton("Revert");
	private final JLabel statusLabel = new JLabel(" ");
	private final JLabel charCountLabel = new JLabel();
	private final List<JButton> actionButtons = new ArrayList<>();
	private JPanel contentPanel;
	private JPanel valueSection;
	private Component bottomSpacer;
	private final Consumer<Boolean> wrapPreferenceChanged;
	
	private JsonTreeNode node;
	private List<JsonDocument> jsonDocuments = List.of();
	private String originalLiteral;
	private boolean updating;
	private boolean applying;
	private boolean wrapLongTextValues;
	private Runnable dirtyChangeCallback;
	private String currentKey = "";
	private List<TextMatch> findMatches = List.of();
	private int findMatchIndex = -1;
	
	public JsonValueEditorPanel(Actions actions) {
		this(actions, false, ignored -> {});
	}

	public JsonValueEditorPanel(Actions actions, boolean wrapLongTextValues, Consumer<Boolean> wrapPreferenceChanged) {
		super(new BorderLayout());
		this.actions = actions;
		this.wrapLongTextValues = wrapLongTextValues;
		this.wrapPreferenceChanged = wrapPreferenceChanged == null ? ignored -> {} : wrapPreferenceChanged;
		setupUI();
		applyTextWrap();
		updateControls();
	}
	
	public void setNode(JsonTreeNode node, List<JsonDocument> jsonDocuments) {
		this.node = node;
		this.jsonDocuments = jsonDocuments == null ? List.of() : jsonDocuments;
		String key = node == null ? "" : node.getKey();
		JsonDocument jsonDocument = firstResource();
		originalLiteral = jsonDocument == null ? null : jsonDocument.getEntry(key);
		if (originalLiteral == null) {
			originalLiteral = "";
		}
		bindNode();
	}
	
	public JsonNodeType getSelectedJsonType() {
		return (JsonNodeType) typeCombo.getSelectedItem();
	}
	
	public String getTypedText() {
		if (getSelectedJsonType() == JsonNodeType.Number) {
			return typedNumber.getText();
		}
		return typedText.getText();
	}
	
	public void setTypedText(String value) {
		if (getSelectedJsonType() == JsonNodeType.Number) {
			typedNumber.setText(value);
			typedNumber.setCaretPosition(0);
			return;
		}
		typedText.setText(value);
		typedText.setCaretPosition(0);
	}
	
	public boolean getTypedBoolean() {
		return typedBooleanTrue.isSelected();
	}
	
	public String getBooleanLabelText() {
		return getTypedBoolean() ? typedBooleanTrue.getText() : typedBooleanFalse.getText();
	}
	
	public boolean isTypedEditorEditable() {
		if (getSelectedJsonType() == JsonNodeType.Boolean) {
			return typedBooleanTrue.isEnabled() && typedBooleanFalse.isEnabled();
		}
		if (getSelectedJsonType() == JsonNodeType.Number) {
			return typedNumber.isEditable() && typedNumber.isEnabled();
		}
		return typedText.isEditable() && typedText.isEnabled();
	}
	
	public String getSummaryText() {
		return summaryLabel.getText();
	}
	
	public String getContainerValueText() {
		return emptyValue.getText();
	}
	
	public String getDisplayedPath() {
		return pathField.getText();
	}
	
	public String getCopyPathButtonText() {
		return copyPathButton.getText();
	}
	
	public List<JButton> getActionButtons() {
		return List.copyOf(actionButtons);
	}
	
	public String[] getSectionLabels() {
		return new String[] { actionsLabel.getText(), valueLabel.getText() };
	}
	
	public void setRawMode(boolean rawMode) {
		if (rawMode) rawModeButton.setSelected(true);
		else typedModeButton.setSelected(true);
		updateMode();
	}
	
	public void setRawText(String value) {
		rawText.setText(value);
		rawText.setCaretPosition(0);
	}
	
	public boolean isApplyEnabled() {
		return applyButton.isEnabled();
	}
	
	public boolean isDirty() {
		return node != null && hasUnappliedChanges();
	}

	public boolean hasInvalidEdit() {
		if (node == null) return false;
		try {
			currentLiteral();
			return false;
		} catch (IllegalArgumentException e) {
			return true;
		}
	}

	private boolean hasUnappliedChanges() {
		if (node == null) {
			return false;
		}
		try {
			return !currentLiteral().equals(originalLiteral);
		} catch (IllegalArgumentException e) {
			return true; // invalid literal means something was typed
		}
	}
	
	public String getStatusText() {
		return statusLabel.getText().trim();
	}

	public String getCharCountText() {
		return charCountLabel.getText();
	}

	public String getTypeBadgeText() {
		return typeBadge.getText();
	}

	public void setDirtyChangeCallback(Runnable callback) {
		this.dirtyChangeCallback = callback;
	}

	public String getCurrentKey() {
		return node == null ? null : node.getKey();
	}
	
	public void applyValue() {
		if (applying || !applyButton.isEnabled() || node == null) {
			return;
		}
		String literal = currentLiteral();
		applying = true;
		try {
			actions.setLiteral(node.getKey(), literal);
			originalLiteral = literal;
		} finally {
			applying = false;
		}
		updateControls();
	}
	
	private void setupUI() {
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		contentPanel = new JPanel(new GridBagLayout());
		contentPanel.setOpaque(false);
		add(contentPanel, BorderLayout.CENTER);
		
		int row = 0;
		addWide(contentPanel, createTitleRow(), row++, 0, false);
		addPathRow(contentPanel, row++);
		addTypeRow(contentPanel, row++);
		addWide(contentPanel, sectionSeparator(), row++, 16, false);
		addWide(contentPanel, createActionsRow(), row++, 8, false);
		addWide(contentPanel, sectionSeparator(), row++, 8, false);
		addWide(contentPanel, createValueHeader(), row++, 4, false);
		valueSection = createValueSection();
		addWide(contentPanel, valueSection, row++, 8, false);
		addWide(contentPanel, createCharCountRow(), row++, 2, false);
		addWide(contentPanel, createFooter(), row++, 8, false);
		GridBagConstraints spacer = new GridBagConstraints();
		spacer.gridx = 0; spacer.gridy = row; spacer.gridwidth = 3;
		spacer.weighty = 1; spacer.fill = GridBagConstraints.VERTICAL;
		bottomSpacer = Box.createVerticalGlue();
		contentPanel.add(bottomSpacer, spacer);
		
		setupListeners();
	}
	
	private JPanel createTitleRow() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createCompoundBorder(
				sectionDividerBorder(),
				BorderFactory.createEmptyBorder(0, 0, 14, 0)));
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() * 1.4f));
		titleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
		panel.add(titleLabel);
		panel.add(Box.createHorizontalStrut(10));
		typeBadge.setOpaque(false);
		typeBadge.setForeground(Color.WHITE);
		typeBadge.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
		typeBadge.setFont(typeBadge.getFont().deriveFont(Font.BOLD, typeBadge.getFont().getSize2D() * 0.9f));
		typeBadge.setAlignmentY(Component.CENTER_ALIGNMENT);
		panel.add(typeBadge);
		panel.add(Box.createHorizontalStrut(8));
		summaryLabel.setForeground(secondaryTextColor());
		summaryLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
		panel.add(summaryLabel);
		panel.add(Box.createHorizontalGlue());
		return panel;
	}
	
	private void addPathRow(JPanel content, int row) {
		JLabel label = createFieldLabel("JSONPath");
		addCell(content, label, 0, row, 0, 0, GridBagConstraints.NONE, 16);
		pathField.setEditable(false);
		pathField.setToolTipText(MessageBundle.get("jsonvalue.path.tooltip"));
		pathField.setFont(pathField.getFont().deriveFont(pathField.getFont().getSize2D() * 1.15f));
		addCell(content, pathField, 1, row, 1, 0, GridBagConstraints.HORIZONTAL, 16);
		copyPathButton.setFont(copyPathButton.getFont().deriveFont(copyPathButton.getFont().getSize2D() * 1.15f));
		copyPathButton.setToolTipText(MessageBundle.get("jsonvalue.copyPath.tooltip"));
		copyPathButton.addActionListener(e -> copyDisplayedPath());
		addCell(content, copyPathButton, 2, row, 0, 0, GridBagConstraints.HORIZONTAL, 16);
	}
	
	private void addTypeRow(JPanel content, int row) {
		addCell(content, createFieldLabel("Change Type"), 0, row, 0, 0, GridBagConstraints.NONE);
		typeCombo.setFont(typeCombo.getFont().deriveFont(typeCombo.getFont().getSize2D() * 1.15f));
		GridBagConstraints c = baseConstraints(1, row);
		c.gridwidth = 2;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		content.add(typeCombo, c);
	}
	
	private JPanel createActionsRow() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setOpaque(false);
		actionButtons.clear();
		panel.add(actionsLabel);
		panel.add(Box.createHorizontalStrut(10));
		addActionButton(panel, new ActionIcon(ActionIcon.Kind.ADD),
				MessageBundle.get("jsonvalue.action.add.tooltip"), () -> actions.addChild(node.getKey()));
		panel.add(Box.createHorizontalStrut(6));
		addActionButton(panel, new ActionIcon(ActionIcon.Kind.EDIT),
				MessageBundle.get("jsonvalue.action.rename.tooltip"), () -> actions.rename(node.getKey()));
		panel.add(Box.createHorizontalStrut(6));
		addActionButton(panel, new ActionIcon(ActionIcon.Kind.DUPLICATE),
				MessageBundle.get("jsonvalue.action.duplicate.tooltip"), () -> actions.duplicate(node.getKey()));
		panel.add(Box.createHorizontalStrut(8));
		panel.add(verticalSeparator());
		panel.add(Box.createHorizontalStrut(8));
		addActionButton(panel, new ActionIcon(ActionIcon.Kind.DELETE),
				MessageBundle.get("jsonvalue.action.delete.tooltip"), () -> actions.delete(node.getKey()));
		panel.add(Box.createHorizontalStrut(8));
		panel.add(verticalSeparator());
		panel.add(Box.createHorizontalStrut(8));
		addActionButton(panel, new ActionIcon(ActionIcon.Kind.UP),
				MessageBundle.get("jsonvalue.action.moveUp.tooltip"), () -> actions.moveUp(node.getKey()));
		panel.add(Box.createHorizontalStrut(6));
		addActionButton(panel, new ActionIcon(ActionIcon.Kind.DOWN),
				MessageBundle.get("jsonvalue.action.moveDown.tooltip"), () -> actions.moveDown(node.getKey()));
		panel.add(Box.createHorizontalGlue());
		return panel;
	}
	
	private void addActionButton(JPanel panel, Icon icon, String tooltip, Runnable action) {
		JButton button = new JButton(icon) {
			@Override
			public Point getToolTipLocation(MouseEvent event) {
				JToolTip tip = createToolTip();
				tip.setTipText(getToolTipText());
				return new Point(0, -(tip.getPreferredSize().height + 2));
			}
		};
		button.setToolTipText(tooltip);
		button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
		button.setFocusPainted(false);
		button.setMargin(new Insets(2, 2, 2, 2));
		Dimension buttonSize = new Dimension(34, 34);
		button.setMinimumSize(buttonSize);
		button.setPreferredSize(buttonSize);
		button.setMaximumSize(buttonSize);
		button.addActionListener(e -> {
			if (node != null) {
				action.run();
			}
		});
		actionButtons.add(button);
		panel.add(button);
	}

	private JSeparator verticalSeparator() {
		JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
		Dimension d = new Dimension(1, 24);
		separator.setPreferredSize(d);
		separator.setMaximumSize(d);
		return separator;
	}

	private JSeparator sectionSeparator() {
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		Dimension d = new Dimension(1, 2);
		separator.setMinimumSize(d);
		separator.setPreferredSize(d);
		return separator;
	}

	static Color dividerColor() {
		return Colors.scale(UIManager.getColor("Panel.background"), .65f);
	}

	static javax.swing.border.Border sectionDividerBorder() {
		return BorderFactory.createMatteBorder(0, 0, 2, 0, dividerColor());
	}

	static javax.swing.border.Border sectionBoxBorder() {
		return BorderFactory.createMatteBorder(2, 2, 2, 2, dividerColor());
	}

	Color titleDividerColor() {
		return dividerColor();
	}
	
	private JPanel createValueHeader() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.add(valueLabel, BorderLayout.WEST);
		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(typedModeButton);
		modeGroup.add(rawModeButton);
		typedModeButton.setFont(typedModeButton.getFont().deriveFont(typedModeButton.getFont().getSize2D() * 0.95f));
		rawModeButton.setFont(rawModeButton.getFont().deriveFont(rawModeButton.getFont().getSize2D() * 0.95f));
		typedModeButton.setMargin(new Insets(2, 10, 2, 10));
		rawModeButton.setMargin(new Insets(2, 10, 2, 10));
		typedModeButton.setFocusPainted(false);
		rawModeButton.setFocusPainted(false);
		typedModeButton.setToolTipText(MessageBundle.get("jsonvalue.mode.typed.tooltip"));
		rawModeButton.setToolTipText(MessageBundle.get("jsonvalue.mode.raw.tooltip"));
		wrapTextBox.setOpaque(false);
		wrapTextBox.setFont(wrapTextBox.getFont().deriveFont(wrapTextBox.getFont().getSize2D() * 0.95f));
		wrapTextBox.setFocusPainted(false);
		wrapTextBox.setSelected(wrapLongTextValues);
		wrapTextBox.setToolTipText(MessageBundle.get("jsonvalue.wrap.tooltip"));
		JPanel togglePanel = new JPanel(new GridLayout(1, 2, 3, 0));
		togglePanel.setOpaque(false);
		togglePanel.add(typedModeButton);
		togglePanel.add(rawModeButton);
		JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		controlsPanel.setOpaque(false);
		setupFindControls();
		controlsPanel.add(findControlsPanel);
		controlsPanel.add(wrapTextBox);
		controlsPanel.add(togglePanel);
		panel.add(controlsPanel, BorderLayout.EAST);
		return panel;
	}

	private void setupFindControls() {
		findField.setToolTipText(MessageBundle.get("jsonvalue.find.tooltip"));
		findField.putClientProperty("JTextField.placeholderText", MessageBundle.get("jsonvalue.find.placeholder"));
		findPreviousButton.setToolTipText(MessageBundle.get("jsonvalue.find.previous.tooltip"));
		findNextButton.setToolTipText(MessageBundle.get("jsonvalue.find.next.tooltip"));
		setupFindButton(findPreviousButton);
		setupFindButton(findNextButton);
		findMatchLabel.setForeground(secondaryTextColor());
		findControlsPanel.setOpaque(false);
		findControlsPanel.add(findField);
		findControlsPanel.add(findPreviousButton);
		findControlsPanel.add(findNextButton);
		findControlsPanel.add(findMatchLabel);
	}

	private void setupFindButton(JButton button) {
		button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
		button.setFocusPainted(false);
		button.setMargin(new Insets(2, 2, 2, 2));
		Dimension size = new Dimension(26, 26);
		button.setMinimumSize(size);
		button.setPreferredSize(size);
		button.setMaximumSize(size);
	}
	
	private JPanel createValueSection() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		typedText.setFont(typedText.getFont().deriveFont(typedText.getFont().getSize2D() * 1.15f));
		typedText.setMargin(new Insets(8, 8, 8, 8));
		applyEditorScrollPaneStyle(typedScrollPane);
		applyEditorScrollPaneStyle(rawScrollPane);
		valueCards.add(typedScrollPane, CARD_TEXT);
		ButtonGroup booleanGroup = new ButtonGroup();
		booleanGroup.add(typedBooleanTrue);
		booleanGroup.add(typedBooleanFalse);
		typedBooleanPanel.setOpaque(false);
		typedBooleanPanel.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
		setupBooleanRadioButton(typedBooleanTrue);
		setupBooleanRadioButton(typedBooleanFalse);
		typedBooleanPanel.add(typedBooleanTrue);
		typedBooleanPanel.add(typedBooleanFalse);
		valueCards.add(typedBooleanPanel, CARD_BOOLEAN);
		emptyValue.setForeground(secondaryTextColor());
		emptyValue.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
		valueCards.add(emptyValue, CARD_EMPTY);
		valueCards.setOpaque(false);
		rawText.setFont(rawText.getFont().deriveFont(rawText.getFont().getSize2D() * 1.15f));
		rawText.setMargin(new Insets(8, 8, 8, 8));
		typedNumber.setFont(typedNumber.getFont().deriveFont(typedNumber.getFont().getSize2D() * 1.15f));
		valueCards.add(typedNumber, CARD_NUMBER);
		editorCards.setOpaque(false);
		editorCards.add(valueCards, EDITOR_TYPED);
		editorCards.add(rawScrollPane, EDITOR_RAW);
		panel.add(editorCards, BorderLayout.CENTER);
		return panel;
	}

	private void applyEditorScrollPaneStyle(JScrollPane scrollPane) {
		scrollPane.putClientProperty(FlatClientProperties.STYLE, EDITOR_SCROLL_PANE_STYLE);
	}

	private void setupBooleanRadioButton(JRadioButton radioButton) {
		radioButton.setOpaque(false);
		radioButton.setFont(radioButton.getFont().deriveFont(radioButton.getFont().getSize2D() * 1.15f));
	}
	
	private JPanel createCharCountRow() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		charCountLabel.setFont(charCountLabel.getFont().deriveFont(charCountLabel.getFont().getSize2D() * 0.92f));
		charCountLabel.setForeground(secondaryTextColor());
		panel.add(charCountLabel, BorderLayout.EAST);
		return panel;
	}

	private JPanel createFooter() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
		buttons.setOpaque(false);
		applyButton.setFont(applyButton.getFont().deriveFont(applyButton.getFont().getSize2D() * 1.15f));
		revertButton.setFont(revertButton.getFont().deriveFont(revertButton.getFont().getSize2D() * 1.15f));
		applyButton.putClientProperty(FlatClientProperties.STYLE, APPLY_BUTTON_STYLE);
		buttons.add(applyButton);
		applyButton.setToolTipText(MessageBundle.get("jsonvalue.apply.tooltip"));
		revertButton.setToolTipText(MessageBundle.get("jsonvalue.revert.tooltip"));
		buttons.add(revertButton);
		panel.add(buttons, BorderLayout.WEST);
		statusLabel.setForeground(statusColor());
		statusLabel.setIcon(new StatusDotIcon());
		statusLabel.setFont(statusLabel.getFont().deriveFont(statusLabel.getFont().getSize2D() * 1.15f));
		panel.add(statusLabel, BorderLayout.EAST);
		return panel;
	}
	
	private void setupListeners() {
		DocumentListener listener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) { updateControls(); }
			@Override
			public void removeUpdate(DocumentEvent e) { updateControls(); }
			@Override
			public void changedUpdate(DocumentEvent e) { updateControls(); }
		};
		typedText.getDocument().addDocumentListener(listener);
		typedNumber.getDocument().addDocumentListener(listener);
		rawText.getDocument().addDocumentListener(listener);
		findField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) { refreshFind(); }
			@Override
			public void removeUpdate(DocumentEvent e) { refreshFind(); }
			@Override
			public void changedUpdate(DocumentEvent e) { refreshFind(); }
		});
		typedBooleanTrue.addActionListener(e -> updateControls());
		typedBooleanFalse.addActionListener(e -> updateControls());
		typedModeButton.addActionListener(e -> updateMode());
		rawModeButton.addActionListener(e -> updateMode());
		wrapTextBox.addActionListener(e -> {
			wrapLongTextValues = wrapTextBox.isSelected();
			applyTextWrap();
			wrapPreferenceChanged.accept(wrapLongTextValues);
		});
		applyButton.addActionListener(e -> applyValue());
		KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
		Action applyAct = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) { applyValue(); }
		};
		typedText.getInputMap().put(ctrlEnter, "applyValue");
		typedText.getActionMap().put("applyValue", applyAct);
		typedNumber.getInputMap().put(ctrlEnter, "applyValue");
		typedNumber.getActionMap().put("applyValue", applyAct);
		rawText.getInputMap().put(ctrlEnter, "applyValue");
		rawText.getActionMap().put("applyValue", applyAct);
		bindFindKeys();
		findPreviousButton.addActionListener(e -> moveFindMatch(-1));
		findNextButton.addActionListener(e -> moveFindMatch(1));
		revertButton.addActionListener(e -> bindNode());
		typeCombo.addActionListener(e -> {
			if (!updating && node != null && !node.isRoot()) {
				JsonNodeType type = getSelectedJsonType();
				if (actions.changeType(node.getKey(), type)) {
					originalLiteral = JsonLiteralCodec.defaultLiteral(type);
					bindNode();
				}
			}
		});
	}
	
	private void bindNode() {
		updating = true;
		try {
			String key = node == null ? "" : node.getKey();
			currentKey = key;
			clearFindQuery();
			if (node == null) {
				titleLabel.setText("No selection");
				pathField.setText("");
				typeCombo.setSelectedItem(JsonNodeType.String);
				typeBadge.setText("");
				typeBadge.setVisible(false);
				summaryLabel.setText("");
				rawText.setText("");
				rawText.setCaretPosition(0);
				typedText.setText("");
				typedText.setCaretPosition(0);
				typedNumber.setText("");
				typedNumber.setCaretPosition(0);
				typedBooleanTrue.setSelected(false);
				typedBooleanFalse.setSelected(false);
				emptyValue.setText("");
				typedModeButton.setSelected(true);
				cardLayout.show(valueCards, CARD_EMPTY);
				editorCardLayout.show(editorCards, EDITOR_TYPED);
			} else {
				JsonNodeType type = JsonNodeType.fromJsonValue(originalLiteral);
				if (type == JsonNodeType.Unknown) {
					type = JsonNodeType.String;
				}
				titleLabel.setText(node.getName());
				pathField.setText(toDisplayJsonPath(key));
				typeCombo.setSelectedItem(type);
				typeBadge.setText(type.getLabel().toUpperCase());
				typeBadge.setVisible(true);
				typeBadge.setBackground(typeColor(type));
				summaryLabel.setText(summary(type));
				rawText.setText(originalLiteral);
				rawText.setCaretPosition(0);
				typedText.setText(typedValue(type));
				typedText.setCaretPosition(0);
				typedNumber.setText(type == JsonNodeType.Number ? originalLiteral : "");
				typedNumber.setCaretPosition(0);
				typedBooleanTrue.setSelected("true".equals(originalLiteral));
				typedBooleanFalse.setSelected(!typedBooleanTrue.isSelected());
				emptyValue.setText("");
				selectTypedCard(type);
			}
		} finally {
			updating = false;
		}
		updateMode();
		updateControls();
	}

	private void bindFindKeys() {
		KeyStroke ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
		getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ctrlF, "focusFind");
		getActionMap().put("focusFind", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (findControlsPanel.isVisible()) {
					findField.requestFocusInWindow();
					findField.selectAll();
				}
			}
		});
		bindFindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "findNext", 1);
		bindFindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "findNext", 1);
		bindFindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "findPrevious", -1);
		bindFindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK), "findPrevious", -1);
		findField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearFind");
		findField.getActionMap().put("clearFind", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!findField.getText().isEmpty()) {
					findField.setText("");
					return;
				}
				JTextArea editor = activeFindEditor();
				if (editor != null) {
					editor.requestFocusInWindow();
				}
			}
		});
	}

	private void bindFindKey(KeyStroke keyStroke, String name, int direction) {
		findField.getInputMap().put(keyStroke, name);
		findField.getActionMap().put(name, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				moveFindMatch(direction);
			}
		});
	}
	
	private void updateMode() {
		boolean rawMode = rawModeButton.isSelected();
		if (rawMode && hasUnappliedChanges()) {
			int choice = javax.swing.JOptionPane.showConfirmDialog(this,
				"You have unapplied changes. Apply them before switching to Raw mode?",
				"Unapplied Changes",
				javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
				javax.swing.JOptionPane.WARNING_MESSAGE);
			if (choice == javax.swing.JOptionPane.CANCEL_OPTION) {
				typedModeButton.setSelected(true);
				return;
			}
			if (choice == javax.swing.JOptionPane.YES_OPTION) {
				applyValue();
			}
		}
		editorCardLayout.show(editorCards, rawMode ? EDITOR_RAW : EDITOR_TYPED);
		updateControls();
		revalidate();
		repaint();
	}
	
	private void updateControls() {
		if (updating) {
			return;
		}
		if (node == null) {
			statusLabel.setText(" ");
			statusLabel.setIcon(null);
			applyButton.setEnabled(false);
			revertButton.setEnabled(false);
			copyPathButton.setEnabled(false);
			typeCombo.setEnabled(false);
			typedModeButton.setEnabled(false);
			rawModeButton.setEnabled(false);
			typedText.setEditable(false);
			typedText.setEnabled(false);
			typedNumber.setEditable(false);
			typedNumber.setEnabled(false);
			typedBooleanTrue.setEnabled(false);
			typedBooleanFalse.setEnabled(false);
			rawText.setEditable(false);
			rawText.setEnabled(false);
			wrapTextBox.setVisible(false);
			updateFindControls();
			charCountLabel.setText("");
			updateValueLayout(false);
			updateActionStates();
			if (dirtyChangeCallback != null) {
				dirtyChangeCallback.run();
			}
			return;
		}
		copyPathButton.setEnabled(true);
		typedModeButton.setEnabled(true);
		rawModeButton.setEnabled(true);
		rawText.setEditable(true);
		rawText.setEnabled(true);
		boolean valid = true;
		String status = " ";
		String literal = originalLiteral;
		try {
			literal = currentLiteral();
		} catch (IllegalArgumentException e) {
			valid = false;
			status = "Invalid JSON literal";
		}
		boolean dirty = valid && !literal.equals(originalLiteral);
		if (!valid) {
			statusLabel.setText(status);
		} else if (dirty) {
			statusLabel.setText("Node changes not applied");
		} else {
			statusLabel.setText(" ");
		}
		statusLabel.setIcon(dirty ? new StatusDotIcon() : null);
		applyButton.setEnabled(node != null && valid && dirty);
		revertButton.setEnabled(node != null && (!valid || dirty));
		updateValueLayout(rawModeButton.isSelected() || getSelectedJsonType() == JsonNodeType.String);
		updateWrapControlVisibility();
		updateFindControls();
		updateActionStates();
		updateCharCount();
		if (dirtyChangeCallback != null) {
			dirtyChangeCallback.run();
		}
	}
	
	private void updateValueLayout(boolean grow) {
		GridBagLayout layout = (GridBagLayout) contentPanel.getLayout();
		GridBagConstraints valueConstraints = layout.getConstraints(valueSection);
		valueConstraints.weighty = grow ? 1 : 0;
		valueConstraints.fill = grow ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
		layout.setConstraints(valueSection, valueConstraints);
		
		GridBagConstraints spacerConstraints = layout.getConstraints(bottomSpacer);
		spacerConstraints.weighty = grow ? 0 : 1;
		layout.setConstraints(bottomSpacer, spacerConstraints);
	}

	private void applyTextWrap() {
		typedText.setLineWrap(wrapLongTextValues);
		typedText.setWrapStyleWord(wrapLongTextValues);
		rawText.setLineWrap(wrapLongTextValues);
		rawText.setWrapStyleWord(wrapLongTextValues);
		typedText.revalidate();
		rawText.revalidate();
	}

	private void updateWrapControlVisibility() {
		wrapTextBox.setVisible(node != null
				&& (rawModeButton.isSelected() || getSelectedJsonType() == JsonNodeType.String));
	}

	private void updateFindControls() {
		boolean visible = activeFindEditor() != null;
		findControlsPanel.setVisible(visible);
		findField.setVisible(visible);
		findPreviousButton.setVisible(visible);
		findNextButton.setVisible(visible);
		findMatchLabel.setVisible(visible);
		findField.setEnabled(visible);
		if (visible) {
			refreshFind();
		} else {
			clearFindHighlights();
			findMatches = List.of();
			findMatchIndex = -1;
			findMatchLabel.setText("0 / 0");
		}
	}

	private JTextArea activeFindEditor() {
		if (node == null) {
			return null;
		}
		if (rawModeButton.isSelected()) {
			return rawText;
		}
		return getSelectedJsonType() == JsonNodeType.String ? typedText : null;
	}

	private void clearFindQuery() {
		if (findField.getText().isEmpty()) {
			clearFindHighlights();
			findMatches = List.of();
			findMatchIndex = -1;
			findMatchLabel.setText("0 / 0");
			return;
		}
		findField.setText("");
	}

	private void refreshFind() {
		JTextArea editor = activeFindEditor();
		clearFindHighlights();
		String query = findField.getText();
		if (editor == null || query.isEmpty()) {
			findMatches = List.of();
			findMatchIndex = -1;
			findMatchLabel.setText("0 / 0");
			findPreviousButton.setEnabled(false);
			findNextButton.setEnabled(false);
			return;
		}
		findMatches = findMatches(editor.getText(), query);
		findMatchIndex = findMatches.isEmpty() ? -1 : 0;
		highlightMatches(editor);
		selectFindMatch();
	}

	private List<TextMatch> findMatches(String text, String query) {
		List<TextMatch> matches = new ArrayList<>();
		String haystack = text.toLowerCase(Locale.ROOT);
		String needle = query.toLowerCase(Locale.ROOT);
		int index = haystack.indexOf(needle);
		while (index >= 0) {
			matches.add(new TextMatch(index, index + needle.length()));
			index = haystack.indexOf(needle, index + needle.length());
		}
		return matches;
	}

	private void highlightMatches(JTextArea editor) {
		Highlighter.HighlightPainter painter =
				new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 230, 130));
		for (TextMatch match : findMatches) {
			try {
				editor.getHighlighter().addHighlight(match.start(), match.end(), painter);
			} catch (BadLocationException ignored) {
				// Match offsets came from the current document text.
			}
		}
	}

	private void moveFindMatch(int direction) {
		if (findMatches.isEmpty()) {
			refreshFind();
			return;
		}
		findMatchIndex = (findMatchIndex + direction + findMatches.size()) % findMatches.size();
		selectFindMatch();
	}

	private void selectFindMatch() {
		JTextArea editor = activeFindEditor();
		boolean hasMatches = editor != null && !findMatches.isEmpty() && findMatchIndex >= 0;
		findPreviousButton.setEnabled(hasMatches);
		findNextButton.setEnabled(hasMatches);
		if (!hasMatches) {
			findMatchLabel.setText("0 / " + findMatches.size());
			return;
		}
		TextMatch match = findMatches.get(findMatchIndex);
		editor.select(match.start(), match.end());
		findMatchLabel.setText((findMatchIndex + 1) + " / " + findMatches.size());
	}

	private void clearFindHighlights() {
		typedText.getHighlighter().removeAllHighlights();
		rawText.getHighlighter().removeAllHighlights();
	}

	private void updateCharCount() {
		String text;
		if (rawModeButton.isSelected()) {
			text = rawText.getText();
		} else if (getSelectedJsonType() == JsonNodeType.Number) {
			text = typedNumber.getText();
		} else {
			text = typedText.getText();
		}
		int len = text.length();
		charCountLabel.setText(len > 0 ? len + " chars" : "");
	}
	
	private void updateActionStates() {
		typeCombo.setEnabled(node != null && !node.isRoot());
		JsonNodeType type = node == null ? JsonNodeType.Unknown : node.getJsonType();
		boolean hasNode = node != null && !node.isRoot();
		for (JButton button : actionButtons) {
			button.setEnabled(node != null);
		}
		if (actionButtons.size() == 6) {
			actionButtons.get(0).setEnabled(node != null && (type == JsonNodeType.Object || type == JsonNodeType.Array));
			actionButtons.get(1).setEnabled(hasNode);
			actionButtons.get(2).setEnabled(hasNode);
			actionButtons.get(3).setEnabled(hasNode);
			actionButtons.get(4).setEnabled(hasNode);
			actionButtons.get(5).setEnabled(hasNode);
		}
	}
	
	private String currentLiteral() {
		if (rawModeButton.isSelected()) {
			return JsonLiteralCodec.normalizeRawLiteral(rawText.getText());
		}
		JsonNodeType type = getSelectedJsonType();
		switch (type) {
			case Number:
				String number = typedNumber.getText().trim();
				if (!JsonLiteralCodec.isValidNumber(number)) {
					throw new IllegalArgumentException("Invalid JSON number");
				}
				return number;
			case Boolean:
				return JsonLiteralCodec.fromTypedBoolean(getTypedBoolean());
			case Null:
				return "null";
			case Object:
			case Array:
				return originalLiteral;
			case String:
			default:
				return JsonLiteralCodec.fromTypedString(typedText.getText());
		}
	}
	
	private String typedValue(JsonNodeType type) {
		if (type == JsonNodeType.String) {
			return JsonLiteralCodec.toTypedString(originalLiteral);
		}
		return "";
	}
	
	private void selectTypedCard(JsonNodeType type) {
		boolean textEditable = type == JsonNodeType.String;
		typedText.setEditable(textEditable);
		typedText.setEnabled(textEditable);
		typedNumber.setEditable(type == JsonNodeType.Number);
		typedNumber.setEnabled(type == JsonNodeType.Number);
		typedBooleanTrue.setEnabled(type == JsonNodeType.Boolean);
		typedBooleanFalse.setEnabled(type == JsonNodeType.Boolean);
		if (type == JsonNodeType.Boolean) {
			cardLayout.show(valueCards, CARD_BOOLEAN);
		} else if (type == JsonNodeType.Number) {
			cardLayout.show(valueCards, CARD_NUMBER);
		} else if (textEditable) {
			cardLayout.show(valueCards, CARD_TEXT);
		} else {
			emptyValue.setText(emptyHint(type));
			cardLayout.show(valueCards, CARD_EMPTY);
		}
	}
	
	private String emptyHint(JsonNodeType type) {
		if (type == JsonNodeType.Object) {
			return "Object - switch to Raw to edit contents, or use the tree.";
		}
		if (type == JsonNodeType.Array) {
			return "Array - switch to Raw to edit contents, or use the tree.";
		}
		return "";
	}

	private String summary(JsonNodeType type) {
		int childCount = node == null ? 0 : node.getJsonChildCount();
		if (type == JsonNodeType.Object) {
			return "{" + childCount + " " + (childCount == 1 ? "property" : "properties") + "}";
		}
		if (type == JsonNodeType.Array) {
			return "[" + childCount + " " + (childCount == 1 ? "item" : "items") + "]";
		}
		if (type == JsonNodeType.Null) {
			return "null";
		}
		return "";
	}
	
	private JsonDocument firstResource() {
		return jsonDocuments.isEmpty() ? null : jsonDocuments.getFirst();
	}
	
	private void copyDisplayedPath() {
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(new StringSelection(toDisplayJsonPath(currentKey)), null);
		} catch (HeadlessException | IllegalStateException e) {
			// Clipboard access can be unavailable in tests or remote sessions.
		}
	}
	
	private String toDisplayJsonPath(String key) {
		if (key == null || key.isEmpty()) {
			return "$";
		}
		StringBuilder result = new StringBuilder("$");
		for (String part : ResourceKeys.parts(key)) {
			if (part.matches("[0-9]+")) {
				result.append('[').append(part).append(']');
			} else if (JSONPATH_IDENTIFIER.matcher(part).matches()) {
				result.append('.').append(part);
			} else {
				result.append("['").append(part.replace("\\", "\\\\").replace("'", "\\'")).append("']");
			}
		}
		return result.toString();
	}
	
	private Color typeColor(JsonNodeType type) {
		return JsonTypeIcon.colorFor(type);
	}

	private static Color secondaryTextColor() {
		Color color = UIManager.getColor("Label.disabledForeground");
		return color == null ? new Color(120, 120, 120) : color;
	}

	private static Color statusColor() {
		Color color = UIManager.getColor("Component.warning.focusedBorderColor");
		if (color == null) {
			color = UIManager.getColor("Actions.Yellow");
		}
		return color == null ? STATUS_FALLBACK_COLOR : color;
	}
	
	private static JLabel createFieldLabel(String text) {
		JLabel label = new JLabel(text);
		label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() * 1.15f));
		return label;
	}

	private static JLabel createSectionLabel(String text) {
		JLabel label = new JLabel(text);
		label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D()));
		return label;
	}
	
	private static GridBagConstraints baseConstraints(int x, int y) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.insets = new Insets(5, x == 0 ? 0 : 10, 5, 0);
		c.anchor = GridBagConstraints.WEST;
		return c;
	}
	
	private static void addCell(JPanel panel, Component component, int x, int y,
			double weightx, double weighty, int fill) {
		GridBagConstraints c = baseConstraints(x, y);
		c.weightx = weightx;
		c.weighty = weighty;
		c.fill = fill;
		panel.add(component, c);
	}

	private static void addCell(JPanel panel, Component component, int x, int y,
			double weightx, double weighty, int fill, int topInset) {
		GridBagConstraints c = baseConstraints(x, y);
		c.insets = new Insets(topInset, x == 0 ? 0 : 10, 5, 0);
		c.weightx = weightx;
		c.weighty = weighty;
		c.fill = fill;
		panel.add(component, c);
	}
	
	private static void addWide(JPanel panel, Component component, int y, int topInset, boolean grow) {
		GridBagConstraints c = baseConstraints(0, y);
		c.gridwidth = 3;
		c.weightx = 1;
		c.weighty = grow ? 1 : 0;
		c.fill = grow ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(topInset, 0, 5, 0);
		panel.add(component, c);
	}
	
	private static class VisibleCardPanel extends JPanel {
		@Serial
        private static final long serialVersionUID = -823081334750231533L;
		
		VisibleCardPanel(LayoutManager layout) {
			super(layout);
		}
		
		@Override
		public Dimension getPreferredSize() {
			for (Component component : getComponents()) {
				if (component.isVisible()) {
					return component.getPreferredSize();
				}
			}
			return super.getPreferredSize();
		}
		
		@Override
		public Dimension getMinimumSize() {
			for (Component component : getComponents()) {
				if (component.isVisible()) {
					return component.getMinimumSize();
				}
			}
			return super.getMinimumSize();
		}
	}

	private record TextMatch(int start, int end) {}
	
	private static class ActionIcon implements Icon {
		private static final int SIZE = EditorIconStyle.ICON_SIZE;
		
		enum Kind {
			ADD, EDIT, DUPLICATE, DELETE, UP, DOWN
		}
		
		private final Kind kind;
		
		ActionIcon(Kind kind) {
			this.kind = kind;
		}
		
		@Override
		public int getIconWidth() {
			return SIZE;
		}
		
		@Override
		public int getIconHeight() {
			return SIZE;
		}
		
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setStroke(EditorIconStyle.stroke(EditorIconStyle.STROKE_WIDTH));
			g2.setColor(color(c));
			switch (kind) {
				case ADD:
					g2.drawLine(x + 12, y + 4, x + 12, y + 20);
					g2.drawLine(x + 4, y + 12, x + 20, y + 12);
					break;
				case EDIT:
					// Pencil pointing lower-left: tip at (4,20), eraser at upper-right
					int[] tipXs = {x+6, x+4, x+8};
					int[] tipYs = {y+16, y+20, y+18};
					g2.fillPolygon(tipXs, tipYs, 3);
					g2.drawPolygon(tipXs, tipYs, 3);
					g2.drawLine(x+6,  y+16, x+16, y+6);  // shaft top edge
					g2.drawLine(x+8,  y+18, x+18, y+8);  // shaft bottom edge
					g2.drawLine(x+16, y+6,  x+18, y+8);  // eraser cap
					g2.drawLine(x+14, y+8,  x+16, y+10); // eraser band
					break;
				case DUPLICATE:
					g2.drawRect(x + 7, y + 7, 10, 10);
					g2.drawRect(x + 11, y + 3, 10, 10);
					break;
				case DELETE:
					g2.drawLine(x + 7, y + 8, x + 17, y + 8);
					g2.drawLine(x + 10, y + 5, x + 14, y + 5);
					g2.drawRect(x + 8, y + 9, 8, 11);
					break;
				case UP:
					g2.fillPolygon(new int[] { x + 12, x + 4, x + 20 }, new int[] { y + 6, y + 18, y + 18 }, 3);
					break;
				case DOWN:
					g2.fillPolygon(new int[] { x + 4, x + 20, x + 12 }, new int[] { y + 6, y + 6, y + 18 }, 3);
					break;
			}
			g2.dispose();
		}
		
		private Color color(Component c) {
			if (c != null && !c.isEnabled()) {
				Color disabled = UIManager.getColor("Label.disabledForeground");
				return disabled == null ? new Color(180, 180, 180) : disabled;
			}
			// Brighten the accent colors on dark themes so they stay legible, the same
			// way JsonTypeIcon adapts its per-type palette.
			if (kind == Kind.ADD) {
				return Colors.forBackground(new Color(23, 135, 65), panelBackground());
			}
			if (kind == Kind.DELETE) {
				return Colors.forBackground(new Color(178, 28, 37), panelBackground());
			}
			return UIManager.getColor("Label.foreground");
		}

		private static Color panelBackground() {
			Color bg = UIManager.getColor("Panel.background");
			return bg == null ? Color.WHITE : bg;
		}
	}
	
	private static class StatusDotIcon implements Icon {
		@Override
		public int getIconWidth() {
			return 12;
		}
		
		@Override
		public int getIconHeight() {
			return 12;
		}
		
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(statusColor());
			g.fillOval(x + 2, y + 3, 7, 7);
		}
	}
}
