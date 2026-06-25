package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.skanga.jsoneditor.model.JsonDocument;
import com.skanga.jsoneditor.swing.JScrollablePanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.MessageBundle;

public class JsonValueEditorPanelTest {
	private static final List<String> RIGHT_PANEL_TOOLTIP_KEYS = List.of(
			"jsonvalue.path.tooltip",
			"jsonvalue.copyPath.tooltip",
			"jsonvalue.action.add.tooltip",
			"jsonvalue.action.rename.tooltip",
			"jsonvalue.action.duplicate.tooltip",
			"jsonvalue.action.delete.tooltip",
			"jsonvalue.action.moveUp.tooltip",
			"jsonvalue.action.moveDown.tooltip",
			"jsonvalue.mode.typed.tooltip",
			"jsonvalue.mode.raw.tooltip",
			"jsonvalue.wrap.tooltip",
			"jsonvalue.apply.tooltip",
			"jsonvalue.revert.tooltip");

	@BeforeEach
	public void loadMessages() {
		MessageBundle.loadResources();
	}

	@Test
	public void selectedStringShowsUnquotedTypedValueAndSavesQuotedLiteral() {
		JsonDocument jsonDocument = resource(Map.of("/users", "array", "/users/0", "object", "/users/0/name", "\"Ada\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/users/0/name"), List.of(jsonDocument));
		
		assertSame(JsonNodeType.String, panel.getSelectedJsonType());
		assertEquals("Ada", panel.getTypedText());
		assertEquals("$.users[0].name", panel.getDisplayedPath());
		
		panel.setTypedText("Grace");
		assertTrue(panel.isDirty());
		assertTrue(panel.isApplyEnabled());
		panel.applyValue();
		
		assertEquals("\"Grace\"", jsonDocument.getEntry("/users/0/name"));
	}
	
	@Test
	public void applyValueIgnoresReentrantApplyDuringTreeRefresh() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/name", "\"Ada\""));
		AtomicInteger setLiteralCalls = new AtomicInteger();
		AtomicReference<JsonValueEditorPanel> panelRef = new AtomicReference<>();
		JsonValueEditorPanel panel = new JsonValueEditorPanel(new JsonValueEditorPanel.Actions() {
			@Override
			public void setLiteral(String key, String literal) {
				setLiteralCalls.incrementAndGet();
				new JsonResourceMutator(jsonDocument).setNodeLiteral(key, literal);
				panelRef.get().applyValue();
			}
			
			@Override
			public boolean changeType(String key, JsonNodeType type) { return true; }
			@Override
			public void addChild(String key) {}
			@Override
			public void rename(String key) {}
			@Override
			public void duplicate(String key) {}
			@Override
			public void delete(String key) {}
			@Override
			public void moveUp(String key) {}
			@Override
			public void moveDown(String key) {}
		});
		panelRef.set(panel);
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/name"), List.of(jsonDocument));
		
		panel.setTypedText("Grace");
		panel.applyValue();
		
		assertEquals(1, setLiteralCalls.get());
		assertEquals("\"Grace\"", jsonDocument.getEntry("/name"));
	}
	
	@Test
	public void booleanNullAndContainerTypedEditorsUseTypeSpecificState() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of(
				"/enabled", "true",
				"/empty", "null",
				"/settings", "object",
				"/settings/name", "\"Ada\"",
				"/items", "array",
				"/items/0", "\"x\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/enabled"), List.of(jsonDocument));
		assertTrue(panel.getTypedBoolean());
		assertEquals("true", panel.getBooleanLabelText());
		assertTrue(findRadioButton(panel, "true").isSelected());
		assertFalse(findRadioButton(panel, "false").isSelected());
		
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/empty"), List.of(jsonDocument));
		assertFalse(panel.isTypedEditorEditable());
		
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/settings"), List.of(jsonDocument));
		assertEquals("{1 property}", panel.getSummaryText());
		assertFalse(panel.isTypedEditorEditable());
		// Typed mode now shows a hint guiding the user to Raw mode for object/array contents.
		assertTrue(panel.getContainerValueText().contains("Raw"));
		
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/items"), List.of(jsonDocument));
		assertEquals("[1 item]", panel.getSummaryText());
	}
	
	@Test
	public void panelExposesScreenshotControls() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/settings", "object", "/settings/name", "\"Ada\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/settings"), List.of(jsonDocument));
		
		assertEquals("Copy Path", panel.getCopyPathButtonText());
		assertEquals(6, panel.getActionButtons().size());
		assertTrue(panel.getActionButtons().stream().allMatch(button -> button.getText().isEmpty()));
		assertTrue(panel.getActionButtons().stream().allMatch(button -> button.getIcon() != null));
		assertTrue(panel.getActionButtons().stream().allMatch(button ->
				FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON.equals(
						button.getClientProperty(FlatClientProperties.BUTTON_TYPE))));
		assertTrue(Arrays.asList(panel.getSectionLabels()).contains("ACTIONS"));
		assertTrue(Arrays.asList(panel.getSectionLabels()).contains("VALUE"));
	}

	@Test
	public void rightPanelControlsUseLocalizedTooltips() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/settings", "object", "/settings/name", "\"Ada\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/settings"), List.of(jsonDocument));

		assertEquals(MessageBundle.get("jsonvalue.path.tooltip"),
				findTextField(panel, "$.settings").getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.copyPath.tooltip"),
				findButton(panel, "Copy Path").getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.mode.typed.tooltip"),
				findToggleButton(panel, "Typed").getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.mode.raw.tooltip"),
				findToggleButton(panel, "Raw").getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.wrap.tooltip"),
				findCheckBox(panel, MessageBundle.get("jsonvalue.wrap.title")).getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.apply.tooltip"),
				findButton(panel, "Apply Value").getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.revert.tooltip"),
				findButton(panel, "Revert").getToolTipText());

		List<JButton> buttons = panel.getActionButtons();
		assertEquals(MessageBundle.get("jsonvalue.action.add.tooltip"), buttons.get(0).getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.action.rename.tooltip"), buttons.get(1).getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.action.duplicate.tooltip"), buttons.get(2).getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.action.delete.tooltip"), buttons.get(3).getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.action.moveUp.tooltip"), buttons.get(4).getToolTipText());
		assertEquals(MessageBundle.get("jsonvalue.action.moveDown.tooltip"), buttons.get(5).getToolTipText());
	}

	@Test
	public void tooltipKeysExistInSupportedBundleFiles() throws IOException {
		Properties english = loadBundleProperties("bundles/messages.properties");
		List<Properties> localizedBundles = List.of(
				loadBundleProperties("bundles/messages_es_ES.properties"),
				loadBundleProperties("bundles/messages_nl.properties"),
				loadBundleProperties("bundles/messages_pt_BR.properties"));

		for (String key : english.stringPropertyNames()) {
			if (!key.contains(".tooltip")) {
				continue;
			}
			assertNotNull(english.getProperty(key));
			for (Properties bundle : localizedBundles) {
				assertTrue(bundle.containsKey(key), "Missing " + key);
				assertNotNull(bundle.getProperty(key));
			}
		}
	}

	@Test
	public void addActionIsEnabledForNewJsonRootObject() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = Editor.createNewJsonResource(null);
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));

		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey(""), List.of(jsonDocument));

		assertTrue(panel.getActionButtons().getFirst().isEnabled());
	}
	
	@Test
	public void actionButtonsUseUniformSquareSize() {
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(resource(Map.of())));
		Dimension expected = new Dimension(34, 34);
		
		for (JButton button : panel.getActionButtons()) {
			assertEquals(expected, button.getPreferredSize());
			assertEquals(expected, button.getMinimumSize());
			assertEquals(expected, button.getMaximumSize());
		}
	}
	
	@Test
	public void actionButtonsHaveVisibleGaps() {
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(resource(Map.of())));
		panel.setSize(1000, 800);
		
		layoutRecursively(panel);
		
		List<JButton> buttons = panel.getActionButtons();
		assertTrue(horizontalGap(buttons.get(0), buttons.get(1)) >= 6);
		assertTrue(horizontalGap(buttons.get(1), buttons.get(2)) >= 6);
	}
	
	@Test
	public void booleanEditorStaysNearValueHeaderAndFooter() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/enabled", "true"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/enabled"), List.of(jsonDocument));
		panel.setSize(1000, 800);
		
		layoutRecursively(panel);
		
		JLabel valueLabel = findLabel(panel, "VALUE");
		JRadioButton booleanEditor = findRadioButton(panel, "true");
		JButton applyButton = findButton(panel, "Apply Value");
		
		assertTrue(componentY(panel, booleanEditor) - componentY(panel, valueLabel) <= 64);
		assertTrue(componentY(panel, applyButton) - componentY(panel, booleanEditor) <= 72);
	}

	@Test
	public void booleanEditorUsesExplicitRadioChoicesAndAppliesSelectedLiteral() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/enabled", "true"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/enabled"), List.of(jsonDocument));

		JRadioButton trueChoice = findRadioButton(panel, "true");
		JRadioButton falseChoice = findRadioButton(panel, "false");

		assertTrue(trueChoice.isSelected());
		assertFalse(falseChoice.isSelected());

		falseChoice.doClick();
		assertFalse(panel.getTypedBoolean());
		assertTrue(panel.isApplyEnabled());
		panel.applyValue();

		assertEquals("false", jsonDocument.getEntry("/enabled"));
		assertFalse(trueChoice.isSelected());
		assertTrue(falseChoice.isSelected());
	}

	@Test
	public void numberTypedEditorUsesCompactSingleLineFieldAndAppliesLiteral() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/count", "42"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/count"), List.of(jsonDocument));
		panel.setSize(1000, 800);

		layoutRecursively(panel);

		JTextField numberField = findTextField(panel, "42");
		assertNotNull(numberField);
		assertTrue(numberField.getColumns() >= 8);
		assertTrue(numberField.getHeight() <= 42);
		assertEquals("42", panel.getTypedText());

		panel.setTypedText("-12.5e2");
		assertTrue(panel.isApplyEnabled());
		panel.applyValue();

		assertEquals("-12.5e2", jsonDocument.getEntry("/count"));
	}

	@Test
	public void numberTypedEditorStaysNearValueHeaderAndFooter() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/count", "42"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/count"), List.of(jsonDocument));
		panel.setSize(1000, 800);

		layoutRecursively(panel);

		JLabel valueLabel = findLabel(panel, "VALUE");
		JTextField numberField = findTextField(panel, "42");
		JButton applyButton = findButton(panel, "Apply Value");

		assertTrue(componentY(panel, numberField) - componentY(panel, valueLabel) <= 64);
		assertTrue(componentY(panel, applyButton) - componentBottomY(panel, numberField) <= 72);
	}

	@Test
	public void invalidNumberTypedInputDisablesApplyAndShowsError() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/count", "42"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/count"), List.of(jsonDocument));

		panel.setTypedText("01");

		assertFalse(panel.isApplyEnabled());
		assertEquals("Invalid JSON literal", panel.getStatusText());
	}

	@Test
	public void rightPanelSectionSeparatorsHaveStableHeight() {
		MessageBundle.loadResources();
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(resource(Map.of())));

		List<JSeparator> separators = findSeparators(panel).stream()
				.filter(separator -> separator.getOrientation() == JSeparator.HORIZONTAL)
				.toList();
		assertEquals(2, separators.size());
		for (JSeparator separator : separators) {
			assertEquals(JSeparator.HORIZONTAL, separator.getOrientation());
			assertTrue(separator.getPreferredSize().height >= 2);
			assertTrue(separator.getMinimumSize().height >= 2);
		}
	}

	@Test
	public void stringEditorUsesAvailableVerticalSpace() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/name", "\"Ada\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/name"), List.of(jsonDocument));
		panel.setSize(1000, 800);
		
		layoutRecursively(panel);
		
		JTextArea typedEditor = findTextArea(panel, "Ada");
		JButton applyButton = findButton(panel, "Apply Value");
		
		assertTrue(typedEditor.getParent().getHeight() >= 300);
		assertTrue(componentY(panel, applyButton) - componentBottomY(panel, typedEditor) <= 72);
	}
	
	@Test
	public void stringEditorUsesViewportHeightInEditorScrollPane() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/name", "\"Ada\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/name"), List.of(jsonDocument));
		JPanel resourcesPanel = new JScrollablePanel(true, false);
		resourcesPanel.setLayout(new BoxLayout(resourcesPanel, BoxLayout.Y_AXIS));
		resourcesPanel.add(panel);
		JScrollPane scrollPane = new JScrollPane(resourcesPanel);
		scrollPane.setSize(1000, 800);
		
		layoutRecursively(scrollPane);
		
		JTextArea typedEditor = findTextArea(panel, "Ada");
		assertTrue(typedEditor.getParent().getHeight() >= 300);
	}
	
	@Test
	public void rawEditorUsesAvailableVerticalSpace() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/name", "\"Ada\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/name"), List.of(jsonDocument));
		panel.setRawMode(true);
		panel.setSize(1000, 800);
		
		layoutRecursively(panel);
		
		JTextArea rawEditor = findTextArea(panel, "\"Ada\"");
		JButton applyButton = findButton(panel, "Apply Value");
		
		assertTrue(rawEditor.getParent().getHeight() >= 300);
		assertTrue(componentY(panel, applyButton) - componentBottomY(panel, rawEditor) <= 72);
	}
	
	@Test
	public void rawEditorUsesViewportHeightInEditorScrollPane() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/name", "\"Ada\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/name"), List.of(jsonDocument));
		panel.setRawMode(true);
		JPanel resourcesPanel = new JScrollablePanel(true, false);
		resourcesPanel.setLayout(new BoxLayout(resourcesPanel, BoxLayout.Y_AXIS));
		resourcesPanel.add(panel);
		JScrollPane scrollPane = new JScrollPane(resourcesPanel);
		scrollPane.setSize(1000, 800);
		
		layoutRecursively(scrollPane);
		
		JTextArea rawEditor = findTextArea(panel, "\"Ada\"");
		assertTrue(rawEditor.getParent().getHeight() >= 300);
	}

	@Test
	public void typedAndRawEditorScrollPanesUseQuietFocusStyle() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/name", "\"Ada\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/name"), List.of(jsonDocument));

		JScrollPane typedScrollPane = findScrollPaneForTextArea(panel, "Ada");
		panel.setRawMode(true);
		JScrollPane rawScrollPane = findScrollPaneForTextArea(panel, "\"Ada\"");

		assertStyleContains(typedScrollPane, "focusedBorderColor: #5f86b8");
		assertStyleContains(typedScrollPane, "focusWidth: 1");
		assertStyleContains(typedScrollPane, "innerFocusWidth: 0");
		assertStyleContains(rawScrollPane, "focusedBorderColor: #5f86b8");
		assertStyleContains(rawScrollPane, "focusWidth: 1");
		assertStyleContains(rawScrollPane, "innerFocusWidth: 0");
	}

	@Test
	public void wrapCheckboxControlsTypedAndRawTextAreaWrapping() {
		JsonDocument jsonDocument = resource(Map.of("/description", "\"A very long value\""));
		AtomicReference<Boolean> wrapPreference = new AtomicReference<>(false);
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument), false, wrapPreference::set);
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/description"), List.of(jsonDocument));

		JCheckBox wrapBox = findCheckBox(panel, MessageBundle.get("jsonvalue.wrap.title"));
		JTextArea typedEditor = findTextArea(panel, "A very long value");
		panel.setRawMode(true);
		JTextArea rawEditor = findTextArea(panel, "\"A very long value\"");

		assertFalse(wrapBox.isSelected());
		assertFalse(typedEditor.getLineWrap());
		assertFalse(rawEditor.getLineWrap());

		wrapBox.doClick();

		assertTrue(wrapBox.isSelected());
		assertTrue(typedEditor.getLineWrap());
		assertTrue(typedEditor.getWrapStyleWord());
		assertTrue(rawEditor.getLineWrap());
		assertTrue(rawEditor.getWrapStyleWord());
		assertEquals(Boolean.TRUE, wrapPreference.get());
	}

	@Test
	public void wrapPreferenceInitializesTextAreaWrapping() {
		JsonDocument jsonDocument = resource(Map.of("/description", "\"A very long value\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument), true, ignored -> {});
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/description"), List.of(jsonDocument));

		assertTrue(findCheckBox(panel, MessageBundle.get("jsonvalue.wrap.title")).isSelected());
		assertTrue(findTextArea(panel, "A very long value").getLineWrap());
	}

	@Test
	public void wrapCheckboxIsVisibleOnlyWhenActiveEditorIsTextArea() {
		JsonDocument jsonDocument = resource(Map.of("/name", "\"Ada\"", "/count", "42"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument), false, ignored -> {});
		JCheckBox wrapBox = findCheckBox(panel, MessageBundle.get("jsonvalue.wrap.title"));

		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/name"), List.of(jsonDocument));
		assertTrue(wrapBox.isVisible());

		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/count"), List.of(jsonDocument));
		assertFalse(wrapBox.isVisible());

		panel.setRawMode(true);
		assertTrue(wrapBox.isVisible());
	}

	@Test
	public void wrapCheckboxStaysAdjacentToModeControls() {
		JsonDocument jsonDocument = resource(Map.of("/name", "\"Ada\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument), false, ignored -> {});
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/name"), List.of(jsonDocument));
		panel.setSize(1000, 800);

		layoutRecursively(panel);

		JCheckBox wrapBox = findCheckBox(panel, MessageBundle.get("jsonvalue.wrap.title"));
		JToggleButton typedButton = findToggleButton(panel, "Typed");

		int gap = componentX(panel, typedButton) - componentRightX(panel, wrapBox);
		assertTrue(gap >= 0 && gap <= 18);
	}
	
	@Test
	public void footerButtonsUseDefaultShapeWithApplyAccent() {
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(resource(Map.of())));
		JButton applyButton = findButton(panel, "Apply Value");
		JButton revertButton = findButton(panel, "Revert");
		
		assertFalse(applyButton.isEnabled());
		assertFalse(revertButton.isEnabled());
		assertNull(applyButton.getClientProperty(FlatClientProperties.BUTTON_TYPE));
		assertNull(revertButton.getClientProperty(FlatClientProperties.BUTTON_TYPE));
		assertStyleContains(applyButton, "background: #178741");
		assertStyleContains(applyButton, "foreground: #ffffff");
		assertNull(revertButton.getClientProperty(FlatClientProperties.STYLE));
	}
	
	@Test
	public void valuePanelInstallsWithoutFlatLafSevereLogs() throws Exception {
		Logger rootLogger = Logger.getLogger("");
		List<LogRecord> severeRecords = new ArrayList<>();
		Handler handler = new Handler() {
			@Override
			public void publish(LogRecord record) {
				if (record.getLevel().intValue() >= Level.SEVERE.intValue()
						&& record.getLoggerName().startsWith("com.formdev.flatlaf")) {
					severeRecords.add(record);
				}
			}
			
			@Override public void flush() {}
			@Override public void close() {}
		};
		handler.setLevel(Level.ALL);
		rootLogger.addHandler(handler);
		try {
			UIManager.setLookAndFeel(new FlatLightLaf());
			JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(resource(Map.of())));
			SwingUtilities.updateComponentTreeUI(panel);
		} finally {
			rootLogger.removeHandler(handler);
		}
		
		assertTrue(severeRecords.isEmpty());
	}
	
	@Test
	public void panelDoesNotAddLargeOuterPadding() {
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(resource(Map.of())));
		EmptyBorder border = (EmptyBorder) panel.getBorder();
		
		assertTrue(border.getBorderInsets(panel).top <= 16);
		assertTrue(border.getBorderInsets(panel).left <= 16);
		assertTrue(border.getBorderInsets(panel).right <= 16);
	}
	
	@Test
	public void hasInvalidEditIsFalseWithNoNodeSelected() {
		MessageBundle.loadResources();
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(resource(Map.of())));
		assertFalse(panel.hasInvalidEdit());
	}

	@Test
	public void hasInvalidEditIsFalseForCleanNode() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/count", "1"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/count"), List.of(jsonDocument));
		assertFalse(panel.hasInvalidEdit());
	}

	@Test
	public void hasInvalidEditIsFalseForValidRawEdit() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/count", "1"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/count"), List.of(jsonDocument));
		panel.setRawMode(true);
		panel.setRawText("42");
		assertFalse(panel.hasInvalidEdit());
	}

	@Test
	public void hasInvalidEditIsTrueForInvalidRawJson() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/count", "1"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/count"), List.of(jsonDocument));
		panel.setRawMode(true);
		panel.setRawText("{broken");
		assertTrue(panel.hasInvalidEdit());
	}

	@Test
	public void invalidRawJsonEditIsDirty() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/count", "1"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/count"), List.of(jsonDocument));
		panel.setRawMode(true);
		panel.setRawText("{broken");

		assertTrue(panel.isDirty());
		assertTrue(panel.hasInvalidEdit());
	}

	@Test
	public void applyValueIsNoOpWhenEditIsInvalid() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/count", "1"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/count"), List.of(jsonDocument));
		panel.setRawMode(true);
		panel.setRawText("{broken");
		panel.applyValue();
		assertEquals("1", jsonDocument.getEntry("/count"));
		assertTrue(panel.hasInvalidEdit());
	}

	@Test
	public void rawModeInvalidInputDisablesApplyAndShowsError() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/count", "1"));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/count"), List.of(jsonDocument));
		
		panel.setRawMode(true);
		panel.setRawText("{broken");
		
		assertFalse(panel.isApplyEnabled());
		assertEquals("Invalid JSON literal", panel.getStatusText());
		
		panel.setRawText("2");
		assertTrue(panel.isApplyEnabled());
		panel.applyValue();
		assertEquals("2", jsonDocument.getEntry("/count"));
	}

	@Test
	public void noSelectionStateIsInertAndClean() {
		MessageBundle.loadResources();
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(resource(Map.of())));

		panel.setNode(null, List.of());

		assertFalse(panel.isDirty());
		assertEquals("", panel.getStatusText());
		assertEquals("", panel.getDisplayedPath());
		assertEquals("", panel.getCharCountText());
		assertEquals("", panel.getTypeBadgeText());
		assertFalse(panel.isApplyEnabled());
		assertFalse(findButton(panel, "Revert").isEnabled());
		assertFalse(findButton(panel, "Copy Path").isEnabled());
		assertFalse(findComboBox(panel).isEnabled());
		assertFalse(findToggleButton(panel, "Typed").isEnabled());
		assertFalse(findToggleButton(panel, "Raw").isEnabled());
		assertFalse(panel.isTypedEditorEditable());
		assertTrue(panel.getActionButtons().stream().noneMatch(JButton::isEnabled));
	}

	@Test
	public void switchingFromDirtyNodeToNoSelectionClearsDirtyState() {
		MessageBundle.loadResources();
		JsonDocument jsonDocument = resource(Map.of("/name", "\"Ada\""));
		JsonValueEditorPanel panel = new JsonValueEditorPanel(testActions(jsonDocument));
		panel.setNode(new JsonTreeModel(jsonDocument.getEntries()).getNodeByKey("/name"), List.of(jsonDocument));
		panel.setTypedText("Grace");
		assertTrue(panel.isDirty());

		panel.setNode(null, List.of());

		assertFalse(panel.isDirty());
		assertEquals("", panel.getStatusText());
		assertEquals("", panel.getCharCountText());
		assertFalse(panel.isApplyEnabled());
	}
	
	private static JsonDocument resource(Map<String,String> values) {
		JsonDocument jsonDocument = new JsonDocument(null);
		jsonDocument.setEntries(values);
		return jsonDocument;
	}
	
	private static JsonValueEditorPanel.Actions testActions(JsonDocument jsonDocument) {
		JsonResourceMutator mutator = new JsonResourceMutator(jsonDocument);
		return new JsonValueEditorPanel.Actions() {
			@Override
			public void setLiteral(String key, String literal) {
				mutator.setNodeLiteral(key, literal);
			}
			
			@Override
			public boolean changeType(String key, JsonNodeType type) {
				mutator.changeType(key, type);
				return true;
			}
			
			@Override
			public void addChild(String key) {}
			@Override
			public void rename(String key) {}
			@Override
			public void duplicate(String key) {}
			@Override
			public void delete(String key) {}
			@Override
			public void moveUp(String key) {}
			@Override
			public void moveDown(String key) {}
		};
	}

	private static Properties loadBundleProperties(String resourceName) throws IOException {
		Properties properties = new Properties();
		try (InputStream input = JsonValueEditorPanelTest.class.getClassLoader().getResourceAsStream(resourceName)) {
			assertNotNull(input, resourceName);
			properties.load(input);
		}
		return properties;
	}
	
	private static void layoutRecursively(Container container) {
		container.doLayout();
		for (Component component : container.getComponents()) {
			if (component instanceof Container child) {
				layoutRecursively(child);
			}
		}
	}
	
	private static int componentY(Component parent, Component child) {
		return SwingUtilities.convertPoint(child.getParent(), child.getLocation(), parent).y;
	}
	
	private static int componentBottomY(Component parent, Component child) {
		Rectangle bounds = SwingUtilities.convertRectangle(child.getParent(), child.getBounds(), parent);
		return bounds.y + bounds.height;
	}

	private static int componentX(Component parent, Component child) {
		return SwingUtilities.convertPoint(child.getParent(), child.getLocation(), parent).x;
	}

	private static int componentRightX(Component parent, Component child) {
		Rectangle bounds = SwingUtilities.convertRectangle(child.getParent(), child.getBounds(), parent);
		return bounds.x + bounds.width;
	}
	
	private static int horizontalGap(Component left, Component right) {
		return right.getX() - (left.getX() + left.getWidth());
	}
	
	private static void assertStyleContains(JComponent component, String expected) {
		Object style = component.getClientProperty(FlatClientProperties.STYLE);
		assertTrue(style != null && style.toString().contains(expected));
	}
	
	private static JLabel findLabel(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JLabel label && text.equals(label.getText())) {
				return label;
			}
			if (component instanceof Container child) {
				JLabel label = findLabel(child, text);
				if (label != null) {
					return label;
				}
			}
		}
		return null;
	}
	
	private static JRadioButton findRadioButton(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JRadioButton radioButton && text.equals(radioButton.getText())) {
				return radioButton;
			}
			if (component instanceof Container child) {
				JRadioButton radioButton = findRadioButton(child, text);
				if (radioButton != null) {
					return radioButton;
				}
			}
		}
		return null;
	}
	
	private static JTextArea findTextArea(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JTextArea textArea && text.equals(textArea.getText())) {
				return textArea;
			}
			if (component instanceof Container child) {
				JTextArea textArea = findTextArea(child, text);
				if (textArea != null) {
					return textArea;
				}
			}
		}
		return null;
	}

	private static JScrollPane findScrollPaneForTextArea(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JScrollPane scrollPane && findTextArea(scrollPane, text) != null) {
				return scrollPane;
			}
			if (component instanceof Container child) {
				JScrollPane scrollPane = findScrollPaneForTextArea(child, text);
				if (scrollPane != null) {
					return scrollPane;
				}
			}
		}
		return null;
	}

	private static JTextField findTextField(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JTextField textField && text.equals(textField.getText())) {
				return textField;
			}
			if (component instanceof Container child) {
				JTextField textField = findTextField(child, text);
				if (textField != null) {
					return textField;
				}
			}
		}
		return null;
	}

	private static JToggleButton findToggleButton(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JToggleButton toggleButton && text.equals(toggleButton.getText())) {
				return toggleButton;
			}
			if (component instanceof Container child) {
				JToggleButton toggleButton = findToggleButton(child, text);
				if (toggleButton != null) {
					return toggleButton;
				}
			}
		}
		return null;
	}

	private static JCheckBox findCheckBox(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JCheckBox checkBox && text.equals(checkBox.getText())) {
				return checkBox;
			}
			if (component instanceof Container child) {
				JCheckBox checkBox = findCheckBox(child, text);
				if (checkBox != null) {
					return checkBox;
				}
			}
		}
		return null;
	}

	private static JComboBox<?> findComboBox(Container container) {
		for (Component component : container.getComponents()) {
			if (component instanceof JComboBox<?> comboBox) {
				return comboBox;
			}
			if (component instanceof Container child) {
				JComboBox<?> comboBox = findComboBox(child);
				if (comboBox != null) {
					return comboBox;
				}
			}
		}
		return null;
	}

	private static List<JSeparator> findSeparators(Container container) {
		List<JSeparator> separators = new ArrayList<>();
		for (Component component : container.getComponents()) {
			if (component instanceof JSeparator separator) {
				separators.add(separator);
			}
			if (component instanceof Container child) {
				separators.addAll(findSeparators(child));
			}
		}
		return separators;
	}
	
	private static JButton findButton(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JButton button && text.equals(button.getText())) {
				return button;
			}
			if (component instanceof Container child) {
				JButton button = findButton(child, text);
				if (button != null) {
					return button;
				}
			}
		}
		return null;
	}
}
