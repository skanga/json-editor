package com.skanga.jsoneditor.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.TreePath;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import com.skanga.jsoneditor.model.JsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.skanga.jsoneditor.Main;
import com.skanga.jsoneditor.io.ChecksumException;
import com.skanga.jsoneditor.io.DuplicateJsonKeyDetector;
import com.skanga.jsoneditor.swing.JFileDrop;
import com.skanga.jsoneditor.swing.JScrollablePanel;
import com.skanga.jsoneditor.swing.util.Dialogs;
import com.skanga.jsoneditor.util.Colors;
import com.skanga.jsoneditor.util.ExtendedProperties;
import com.skanga.jsoneditor.util.DuplicateJsonKey;
import com.skanga.jsoneditor.util.GithubRepoUtil;
import com.skanga.jsoneditor.util.GithubRepoUtil.GithubRepoReleaseData;
import com.skanga.jsoneditor.util.Images;
import com.skanga.jsoneditor.util.JsonLoadException;
import com.skanga.jsoneditor.util.JsonParseError;
import com.skanga.jsoneditor.util.MessageBundle;
import com.skanga.jsoneditor.util.ResourceKeys;
import com.skanga.jsoneditor.io.JsonIO;

/**
 * This class represents the main class of the editor.
 */
public class Editor extends JFrame {
    @Serial
    private final static long serialVersionUID = 1113029729495390082L;
    private final static Logger log = LoggerFactory.getLogger(Editor.class);
    private final static long LARGE_JSON_WARNING_BYTES = 10L * 1024L * 1024L;
    private final static int RESTORE_EXPANDED_NODE_LIMIT = 5000;

    public final static String TITLE = "JSON Editor";
    public final static String VERSION = "0.1";
    public final static String GITHUB_USER = "skanga";
    public final static String GITHUB_PROJECT = "json-editor";
    public final static String SETTINGS_FILE = ".json-editor";
    public final static String SETTINGS_DIR = System.getProperty("user.home");

    public final static Locale DEFAULT_LANGUAGE = Locale.ENGLISH;
    public final static List<Locale> SUPPORTED_LANGUAGES = new ArrayList<>(List.of(
            Locale.ENGLISH,
            Locale.of("nl"),
            Locale.of("pt", "BR"),
            Locale.of("es", "ES")));

    private EditorProject project;
    private final EditorSettings settings = new EditorSettings();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final UndoManager structuralUndoManager = new UndoManager();
    private boolean dirty;

    private EditorMenuBar editorMenu;
    private EditorToolBar editorToolBar;
    private JPanel projectPanel;
    private JSplitPane contentPane;
    private JLabel introText;
    private JTextField treeFilterField;
    private final JLabel filterMatchLabel = new JLabel();
    private JScrollPane resourcesScrollPane;
    private JsonTree jsonTree;
    private JsonKeyField jsonKeyField;
    private JPanel resourcesPanel;
    private JsonValueEditorPanel jsonValueEditorPanel;
    private transient DeleteConfirmation deleteConfirmation = this::confirmDeleteNode;
    private boolean duplicateKeyWarningDismissed;

    @FunctionalInterface
    interface ResourceWriter {
        void write(JsonDocument jsonDocument, boolean prettyPrinting, boolean flattenJson) throws IOException;
    }

    @FunctionalInterface
    interface DeleteConfirmation {
        boolean confirm(JsonTreeNode node);
    }

    public void importProject(Path file, boolean showEmptyProjectError) {
        if (!closeCurrentProject()) {
            return;
        }

        clearUI();
        duplicateKeyWarningDismissed = false;
        project = new EditorProject(file);
        project.setMinifyResources(settings.isMinifyResources());
        project.setFlattenJSON(settings.isFlattenJSON());

        Map<String, String> keys = new LinkedHashMap<>();

        try {
            JsonDocument jsonDocument = JsonIO.open(file);
            setupResource(jsonDocument);
            project.setResource(jsonDocument);
            scheduleDuplicateKeyScan(jsonDocument);
            keys.putAll(jsonDocument.getEntries());
        } catch (JsonLoadException ex) {
            log.error("Error importing resource file " + file, ex);
            project = null;
            showJsonLoadError(file, ex);
        } catch (IOException ex) {
            log.error("Error importing resource file " + file, ex);
            project = null;
            showImportProjectError(file, showEmptyProjectError, this::showError);
        }

        jsonTree.setModel(new JsonTreeModel(keys));
        if (jsonTree.getRowCount() > 0) {
            jsonTree.setSelectionRow(0);
        }

        updateTreeNodeStatuses();
        updateHistory();
        updateUI();
    }

    public void importJsonFile(Path file, boolean showError) {
        try {
            if (!Files.isRegularFile(file)) throw new IllegalArgumentException();

            if (!closeCurrentProject()) {
                return;
            }

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if (showError && Files.size(file) > LARGE_JSON_WARNING_BYTES) {
                boolean open = Dialogs.showConfirmDialog(this,
                        MessageBundle.get("dialogs.file.large.title"),
                        MessageBundle.get("dialogs.file.large.text", file, Files.size(file) / (1024L * 1024L)),
                        JOptionPane.WARNING_MESSAGE);
                if (!open) {
                    setCursor(Cursor.getDefaultCursor());
                    return;
                }
            }
            clearUI();
            duplicateKeyWarningDismissed = false;
            project = new EditorProject(file);
            project.setMinifyResources(settings.isMinifyResources());
            project.setFlattenJSON(settings.isFlattenJSON());

            JsonDocument jsonDocument = new JsonDocument(file);
            JsonIO.load(jsonDocument);
            setupResource(jsonDocument);
            project.setResource(jsonDocument);
            scheduleDuplicateKeyScan(jsonDocument);
            jsonTree.setModel(new JsonTreeModel(jsonDocument.getEntries()));
            if (jsonTree.getRowCount() > 0) {
                jsonTree.setSelectionRow(0);
            }

            updateHistory();
            updateUI();
        } catch (IOException | IllegalArgumentException e) {
            log.error("Error opening JSON file " + file, e);
            project = null;
            if (showError) {
                if (e instanceof JsonLoadException jle) {
                    showJsonLoadError(file, jle);
                } else {
                    showError(MessageBundle.get("file.open.error.single", file));
                }
            }
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public void createJsonFile(Path file) {
        try {
            file = withJsonExtension(file);

            if (!closeCurrentProject()) {
                return;
            }

            if (Files.exists(file)) {
                boolean overwrite = Dialogs.showConfirmDialog(this,
                        MessageBundle.get("dialogs.file.new.conflict.title"),
                        MessageBundle.get("dialogs.file.new.conflict.text", file),
                        JOptionPane.WARNING_MESSAGE);
                if (!overwrite) {
                    return;
                }
            }

            clearUI();
            duplicateKeyWarningDismissed = false;
            project = new EditorProject(file);
            project.setMinifyResources(settings.isMinifyResources());
            project.setFlattenJSON(settings.isFlattenJSON());

            JsonDocument jsonDocument = createNewJsonResource(file);
            JsonIO.write(jsonDocument, false, false);
            setupResource(jsonDocument);
            project.setResource(jsonDocument);
            jsonTree.setModel(new JsonTreeModel(jsonDocument.getEntries()));
            jsonTree.setSelectionRow(0);

            updateHistory();
            updateUI();
        } catch (IOException e) {
            log.error("Error creating JSON file " + file, e);
            showError(MessageBundle.get("file.create.error"));
        }
    }

    static JsonDocument createNewJsonResource(Path file) {
        JsonDocument jsonDocument = new JsonDocument(file);
        jsonDocument.setEntries(Map.of("", JsonLiteralCodec.defaultLiteral(JsonNodeType.Object)));
        return jsonDocument;
    }

    public boolean saveProject() {
        if (jsonValueEditorPanel != null && jsonValueEditorPanel.hasInvalidEdit()) {
            boolean proceed = Dialogs.showConfirmDialog(this,
                    "Invalid Edit",
                    "The current edit for '" + jsonValueEditorPanel.getCurrentKey() + "' is not valid JSON and will not be saved.\nSave without it?",
                    JOptionPane.WARNING_MESSAGE);
            if (!proceed) return false;
        }
        if (jsonValueEditorPanel != null) {
            jsonValueEditorPanel.applyValue();
        }
        boolean error = false;
        if (project != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                if (!saveResource(project.getResource())) {
                    error = true;
                }
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
        if (dirty) {
            setDirty(error);
        }
        if (!error) {
            String currentTitle = getTitle();
            setTitle(currentTitle + "  - " + MessageBundle.get("editor.title.saved"));
            javax.swing.Timer clearTimer = new javax.swing.Timer(2500, ev -> setTitle(currentTitle));
            clearTimer.setRepeats(false);
            clearTimer.start();
        }
        return !error;
    }

    public void reloadProject() {
        if (project != null && project.hasResource()) {
            openPath(project.getResource().getPath(), true);
        }
    }

    public void removeSelectedKey() {
        JsonTreeNode node = jsonTree.getSelectionNode();
        if (node != null && !node.isRoot()) {
            if (!confirmDelete(node)) {
                return;
            }
            JsonTreeNode parent = (JsonTreeNode) node.getParent();
            if (parent != null && parent.getJsonType() == JsonNodeType.Array && isJsonProject()) {
                mutateJsonResources(mutator -> {
                    mutator.deleteArrayItem(node.getKey());
                    return parent.getKey();
                });
                return;
            }
            removeKey(node.getKey());
            jsonTree.setSelectionNode(parent);
        }
    }

    void setDeleteConfirmation(DeleteConfirmation deleteConfirmation) {
        this.deleteConfirmation = deleteConfirmation == null ? this::confirmDeleteNode : deleteConfirmation;
    }

    public void renameSelectedKey() {
        JsonTreeNode node = jsonTree.getSelectionNode();
        if (node != null && !node.isRoot()) {
            showRenameDialog(node.getKey());
        }
    }

    public void copySelectedKey() {
        JsonTreeNode node = jsonTree.getSelectionNode();
        if (node != null && !node.isRoot()) {
            String key = node.getKey();
            StringSelection selection = new StringSelection(key);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        }
    }

    public void duplicateSelectedKey() {
        JsonTreeNode node = jsonTree.getSelectionNode();
        if (node != null && !node.isRoot()) {
            showDuplicateDialog(node.getKey());
        }
    }

    public void changeSelectedType(JsonNodeType type) {
        JsonTreeNode node = jsonTree.getSelectionNode();
        if (node == null || node.isRoot() || !isJsonProject()) {
            return;
        }
        String key = node.getKey();
        if (requiresDestructiveTypeConfirmation(key, type) && !confirmDestructiveTypeChange(key)) {
            updateUI();
            return;
        }
        mutateJsonResources(mutator -> {
            mutator.changeType(key, type);
            return key;
        });
    }


    public boolean addKey(String key) {
        final String normalizedKey = normalizeKey(key);
        key = normalizedKey;
        if (!isValidKey(key)) {
            showError(MessageBundle.get("dialogs.jsonkey.key.error"));
            return false;
        }
        JsonTreeNode node = jsonTree.getNodeByKey(key);
        if (node != null) {
            jsonTree.setSelectionNode(node);
        } else if (!confirmNewKey(key)) {
            return false;
        }
        if (project != null) {
            project.getResource().storeEntry(normalizedKey, "");
        }
        jsonTree.addNodeByKey(key);
        return true;
    }

    public void removeKey(String key) {
        if (project != null) {
            project.getResource().removeEntry(key);
        }
        jsonTree.removeNodeByKey(key);
    }

    public boolean renameKey(String key, String newKey) {
        final String normalizedNewKey = normalizeKey(newKey);
        newKey = normalizedNewKey;
        if (!isValidKey(newKey)) {
            showError(MessageBundle.get("dialogs.jsonkey.key.error"));
            return false;
        }
        if (key.equals(newKey)) {
            return true;
        }
        if (!confirmNewKey(key, newKey)) {
            return false;
        }
        if (project != null) {
            project.getResource().renameEntry(key, normalizedNewKey);
        }
        jsonTree.renameNodeByKey(key, newKey);
        return true;
    }

    public boolean duplicateKey(String key, String newKey) {
        final String normalizedNewKey = normalizeKey(newKey);
        newKey = normalizedNewKey;
        if (!isValidKey(newKey)) {
            showError(MessageBundle.get("dialogs.jsonkey.key.error"));
            return false;
        }
        if (key.equals(newKey)) {
            return true;
        }
        if (!confirmNewKey(key, newKey)) {
            return false;
        }
        if (project != null) {
            project.getResource().duplicateEntry(key, normalizedNewKey);
        }
        jsonTree.duplicateNodeByKey(key, newKey);
        return true;
    }


    public EditorProject getProject() {
        return project;
    }

    public EditorSettings getSettings() {
        return settings;
    }

    /**
     * Switches the light/dark theme at runtime and applies it live to all open windows.
     * The preference is persisted with the rest of the editor state on exit.
     */
    public void applyTheme(boolean dark) {
        settings.setDarkTheme(dark);
        Main.setupLookAndFeel(dark);
        com.formdev.flatlaf.FlatLaf.updateUI();
    }

    public Locale getCurrentLocale() {
        Locale locale = settings.getEditorLanguage();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        updateTitle();
        editorMenu.setSavable(dirty);
        if (editorToolBar != null) {
            editorToolBar.setSavable(dirty);
        }
    }

    public void clearHistory() {
        settings.setHistory(new ArrayList<>());
        editorMenu.setRecentItems(new ArrayList<>());
    }

    public void showCreateJsonFileDialog() {
        String path = null;
        if (project != null) {
            path = project.getPath().toString();
        }
        JFileChooser fc = new JFileChooser(path);
        fc.setDialogTitle(MessageBundle.get("dialogs.file.new.title"));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fc.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            createJsonFile(Paths.get(fc.getSelectedFile().getPath()));
        } else {
            updateHistory();
            updateUI();
        }
    }

    public void showImportProjectDialog() {
        String path = null;
        if (project != null) {
            path = project.getPath().toString();
        }
        JFileChooser fc = new JFileChooser(path);
        fc.setDialogTitle(MessageBundle.get("dialogs.project.import.title"));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            importProject(fc.getSelectedFile().toPath(), true);
        }
    }


    public void showRenameDialog(String key) {
        String newKey = "";
        while (newKey != null) {
            newKey = Dialogs.showInputDialog(this,
                    MessageBundle.get("dialogs.jsonkey.rename.title"),
                    MessageBundle.get("dialogs.jsonkey.rename.text"),
                    MessageBundle.get("dialogs.jsonkey.add.help"),
                    JOptionPane.QUESTION_MESSAGE, key, new JsonKeyCaret());
            if (newKey != null) {
                boolean result = renameKey(key, newKey.trim());
                if (result) {
                    break;
                }
            }
        }
    }

    public void showDuplicateDialog(String key) {
        String newKey = "";
        while (newKey != null) {
            newKey = Dialogs.showInputDialog(this,
                    MessageBundle.get("dialogs.jsonkey.duplicate.title"),
                    MessageBundle.get("dialogs.jsonkey.duplicate.text"),
                    MessageBundle.get("dialogs.jsonkey.add.help"),
                    JOptionPane.QUESTION_MESSAGE, key, new JsonKeyCaret());
            if (newKey != null) {
                boolean result = duplicateKey(key, newKey.trim());
                if (result) {
                    break;
                }
            }
        }
    }

    public void showAddDialog(JsonTreeNode node) {
        String key = createAddInitialKey(project, node);
        String newKey = "";
        while (newKey != null) {
            newKey = Dialogs.showInputDialog(this,
                    MessageBundle.get("dialogs.jsonkey.add.title"),
                    MessageBundle.get("dialogs.jsonkey.add.text"),
                    MessageBundle.get("dialogs.jsonkey.add.help"),
                    JOptionPane.QUESTION_MESSAGE, key, new JsonKeyCaret());
            if (newKey != null) {
                boolean result = addKey(newKey.trim());
                if (result) {
                    break;
                }
            }
        }
    }

    static String createAddInitialKey(EditorProject project, JsonTreeNode node) {
        if (node == null || node.isRoot()) {
            return "";
        }
        return node.getKey() + "/";
    }

    static void showImportProjectError(Path file, boolean showEmptyProjectError, Consumer<String> errorReporter) {
        if (showEmptyProjectError) {
            errorReporter.accept(MessageBundle.get("file.open.error.single", file));
        }
    }

    public void showFindDialog() {
        String query = "";
        while (query != null) {
            query = Dialogs.showInputDialog(this,
                    MessageBundle.get("dialogs.jsonkey.find.title"),
                    MessageBundle.get("dialogs.jsonkey.find.text"),
                    null, JOptionPane.QUESTION_MESSAGE, query, new JsonKeyCaret());
            if (query != null) {
                JsonTreeNode node = findNode(query.trim());
                if (node == null) {
                    Dialogs.showWarningDialog(this,
                            MessageBundle.get("dialogs.jsonkey.find.title"),
                            MessageBundle.get("dialogs.jsonkey.find.error"));
                } else {
                    treeFilterField.setText("");
                    jsonTree.setSelectionNode(node);
                    break;
                }
            }
        }
    }

    public void showAboutDialog() {
        Dialogs.showHtmlDialog(this, MessageBundle.get("dialogs.about.title", TITLE),
                "<img src=\"" + Images.getClasspathURL("images/icon-48.png") + "\"><br>" +
                        "<span style=\"font-size:1.3em;\"><strong>" + TITLE + "</strong></span><br>" +
                        VERSION + "<br>");
    }

    public void showVersionDialog(boolean newVersionOnly) {
        executor.execute(() -> {
            GithubRepoReleaseData data;
            String content;
            try {
                data = GithubRepoUtil.getLatestRelease(Editor.GITHUB_USER, Editor.GITHUB_PROJECT).get(30, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                data = null;
            }
            if (data == null) {
                if (!newVersionOnly) {
                    content = "Could not check for updates. Please check your internet connection.";
                } else {
                    return;
                }
            } else if (isNewerVersion(VERSION, data.getTagName())) {
                content = MessageBundle.get("dialogs.version.new") + " " +
                        "<strong>" + data.getTagName() + "</strong><br>" +
                        "<a href=\"" + data.getHtmlUrl() + "\">" + MessageBundle.get("dialogs.version.link") + "</a>";
            } else if (!newVersionOnly) {
                content = MessageBundle.get("dialogs.version.uptodate");
            } else {
                return;
            }
            final String dialogContent = content;
            SwingUtilities.invokeLater(() -> Dialogs.showHtmlDialog(this,
                    MessageBundle.get("dialogs.version.title"), dialogContent));
        });
    }

    public boolean closeCurrentProject() {
        if (jsonValueEditorPanel != null && jsonValueEditorPanel.hasInvalidEdit() && !dirty) {
            boolean proceed = Dialogs.showConfirmDialog(this,
                    "Discard Edit",
                    "The current edit for '" + jsonValueEditorPanel.getCurrentKey() + "' is not valid JSON and will be discarded.",
                    JOptionPane.WARNING_MESSAGE);
            if (!proceed) return false;
        }
        if (jsonValueEditorPanel != null) {
            jsonValueEditorPanel.applyValue();
        }
        boolean result = true;
        if (dirty) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    MessageBundle.get("dialogs.save.text"),
                    MessageBundle.get("dialogs.save.title"),
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                result = saveProject();
                if (!result) {
                    JOptionPane.showMessageDialog(this,
                            "Failed to save the project. Please check the file and try again.",
                            "Save Failed", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                result = confirm != JOptionPane.CANCEL_OPTION;
            }
        }
        if (result && dirty) {
            setDirty(false);
        }
        return result;
    }

    public void closeFile() {
        if (!closeCurrentProject()) {
            return;
        }
        project = null;
        clearUI();
        updateHistory();
        updateUI();
    }

    public void openProjectDirectory() {
        if (project == null) return;
        try {
            Path path = project.getPath();
            Desktop.getDesktop().open((Files.isRegularFile(path) ? path.getParent() : path).toFile());
        } catch (IOException ex) {
            log.error("Unable to open project directory " + project.getPath(), ex);
        }
    }

    public void launch() {
        launch(Optional.empty());
    }

    public void launch(Optional<Path> startupJsonFile) {
        restoreEditorState();

        if (settings.getEditorLanguage() != null) {
            Locale.setDefault(settings.getEditorLanguage());
        } else {
            Locale.setDefault(DEFAULT_LANGUAGE);
        }

        MessageBundle.loadResources();

        setupUI();
        setupFileDrop();
        setupGlobalKeyEventDispatcher();

        setPreferredSize(new Dimension(settings.getWindowWidth(), settings.getWindowHeight()));
        setLocation(settings.getWindowPositionX(), settings.getWindowPositionY());
        contentPane.setDividerLocation(settings.getWindowDividerPosition());

        pack();
        setVisible(true);

        if (startupJsonFile.isPresent()) {
            importJsonFile(startupJsonFile.get(), true);
        } else {
            List<String> dirs = settings.getHistory();
            if (!dirs.isEmpty()) {
                String lastDir = dirs.getLast();
                Path path = Paths.get(lastDir);
                if (Files.exists(path)) {
                    openPath(path, false);
                }
            }
        }

        if (project == null) {
            updateHistory();
        }
        if (project != null && project.hasResource()) {
            restoreTreeState();
            // Restore last selected node
            String selectedKey = settings.getLastSelectedNode();
            JsonTreeNode selectedNode = jsonTree.getNodeByKey(selectedKey);
            if (selectedNode != null) {
                jsonTree.setSelectionNode(selectedNode);
            }
        }

        if (settings.isCheckVersionOnStartup()) {
            showVersionDialog(true);
        }
    }

    public void updateUI() {
        JsonTreeNode selectedNode = jsonTree.getSelectionNode();
        boolean jsonProject = isJsonProject();

        if (jsonProject) {
            jsonValueEditorPanel.setNode(selectedNode,
                    !project.hasResource() ? List.of() : List.of(project.getResource()));
        }
        boolean resourcesPanelStructureChanged = rebuildResourcesPanelIfNeeded(jsonProject);

        boolean containerStructureChanged = updateProjectContainer();
        if (project != null) {
            editorMenu.setEnabled(true);
            editorMenu.setEditable(project.hasResource());
            if (editorToolBar != null) {
                editorToolBar.setProjectOpen(true);
            }
            jsonKeyField.setEditable(project.hasResource());
        } else {
            editorMenu.setEnabled(false);
            editorMenu.setEditable(false);
            if (editorToolBar != null) {
                editorToolBar.setProjectOpen(false);
            }
            jsonKeyField.setEditable(false);
        }

        updateUndoRedoState();

        jsonTree.setToggleClickCount(settings.isDoubleClickTreeToggling() ? 2 : 1);

        updateTitle();
        if (resourcesPanelStructureChanged || containerStructureChanged) {
            validate();
            repaint();
        }
    }

    private boolean rebuildResourcesPanelIfNeeded(boolean jsonProject) {
        resourcesPanel.removeAll();
        if (jsonProject) {
            if (project != null && project.hasResource() && project.getResource().hasDuplicateKeys()
                    && !duplicateKeyWarningDismissed) {
                resourcesPanel.add(createDuplicateKeyWarningPanel(project.getResource(), () -> {
                    duplicateKeyWarningDismissed = true;
                    updateUI();
                }));
            }
            resourcesPanel.add(jsonValueEditorPanel);
        }
        return true;
    }

    private boolean updateProjectContainer() {
        Container container = getContentPane();
        boolean structureChanged = false;
        if (project != null) {
            if (projectPanel.getParent() != container) {
                container.add(projectPanel, BorderLayout.CENTER);
                structureChanged = true;
            }
            if (introText.getParent() == container) {
                container.remove(introText);
                structureChanged = true;
            }
        } else {
            if (introText.getParent() != container) {
                container.add(introText, BorderLayout.CENTER);
                structureChanged = true;
            }
            if (projectPanel.getParent() == container) {
                container.remove(projectPanel);
                structureChanged = true;
            }
        }
        return structureChanged;
    }

    private boolean confirmNewKey(String oldKey, String newKey) {
        JsonTreeNode newNode = jsonTree.getNodeByKey(newKey);
        JsonTreeNode oldNode = jsonTree.getNodeByKey(oldKey);
        if (newNode != null) {
            boolean isReplace = isReplaceConflict(oldNode, newNode);
            boolean confirm = Dialogs.showConfirmDialog(this,
                    MessageBundle.get("dialogs.jsonkey.conflict.title"),
                    MessageBundle.get("dialogs.jsonkey.conflict.text." + (isReplace ? "replace" : "merge")),
                    JOptionPane.WARNING_MESSAGE);
            if (!confirm) {
                return false;
            }
        }
        return confirmNewKey(newKey);
    }

    private JsonTreeNode findNode(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        JsonTreeModel model = jsonTree.getSourceModel();
        JsonTreeNode exact = model.getNodeByKey(query);
        if (exact != null) {
            return exact;
        }
        String normalized = normalizeKey(query);
        exact = model.getNodeByKey(normalized);
        if (exact != null) {
            return exact;
        }
        return model.findNode(query, key -> project == null || !project.hasResource() ? null :
                project.getResource().getEntry(key));
    }

    private void restoreTreeState() {
        JsonTreeModel model = jsonTree.getSourceModel();
        if (model.getNodeCount() > RESTORE_EXPANDED_NODE_LIMIT) {
            return;
        }
        List<String> expandedKeys = settings.getLastExpandedNodes();
        List<JsonTreeNode> expandedNodes = expandedKeys.stream()
                .map(jsonTree::getNodeByKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        jsonTree.expand(expandedNodes);
    }

    private String normalizeKey(String key) {
        key = key == null ? "" : key.trim();
        if (project != null) {
            if (key.isEmpty() || ResourceKeys.isJsonPath(key)) {
                return key;
            }
            return ResourceKeys.createJsonPath("", key);
        }
        return key;
    }

    private boolean isValidKey(String key) {
        return !key.isEmpty() && ResourceKeys.isJsonPath(key);
    }

    private boolean confirmNewKey(String key) {
        if (project == null) {
            return true;
        }
        // Check if there is an existing leaf node in the key path with one or more values
        key = ResourceKeys.withoutLastPart(key);
        while (!(key == null || key.isEmpty())) {
            JsonTreeNode node = jsonTree.getNodeByKey(key);
            if (node != null && !node.isRoot() && node.isLeaf()) {
                String tv = project.getResource().getEntry(node.getKey());
                boolean hasValue = project.hasResource() && !(tv == null || tv.isEmpty());
                if (hasValue) {
                    return Dialogs.showConfirmDialog(this,
                            MessageBundle.get("dialogs.jsonkey.overwrite.title"),
                            MessageBundle.get("dialogs.jsonkey.overwrite.text", node.getKey()),
                            JOptionPane.WARNING_MESSAGE);
                }
            }
            key = ResourceKeys.withoutLastPart(key);
        }
        return true;
    }

    private void clearUI() {
        structuralUndoManager.discardAllEdits();
        jsonKeyField.clear();
        jsonTree.clear();
        if (jsonValueEditorPanel != null) {
            jsonValueEditorPanel.setNode(null, List.of());
        }
        updateUI();
    }

    private void setupUI() {
        Color borderColor = Colors.scale(UIManager.getColor("Panel.background"), .8f);

        setTitle(TITLE);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new EditorWindowListener());

        setIconImages(new ArrayList<>(List.of("512", "256", "128", "64", "48", "32", "24", "20", "16")).stream()
                .map(size -> Images.loadFromClasspath("images/icon-" + size + ".png").getImage())
                .collect(Collectors.toList()));

        jsonTree = new JsonTree();
        jsonTree.addTreeSelectionListener(new JsonTreeNodeSelectionListener());
        jsonTree.addMouseListener(new JsonTreeMouseListener());

        jsonKeyField = new JsonKeyField();
        jsonKeyField.addKeyListener(new JsonKeyFieldKeyListener());
        jsonKeyField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor),
                ((CompoundBorder) jsonKeyField.getBorder()).getInsideBorder()));

        treeFilterField = createTreeFilterField(borderColor);
        treeFilterField.getDocument().addDocumentListener(new TreeFilterDocumentListener());

        JScrollPane jsonTreeScrollPane = createJsonTreeScrollPane(jsonTree);

        JPanel jsonTreePanel = createJsonTreePanel(treeFilterField, jsonTree, jsonTreeScrollPane, borderColor, filterMatchLabel);

        resourcesPanel = new JScrollablePanel(true, false);
        resourcesPanel.setLayout(new BoxLayout(resourcesPanel, BoxLayout.Y_AXIS));
        resourcesPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        resourcesPanel.setOpaque(false);
        jsonValueEditorPanel = new JsonValueEditorPanel(createJsonPanelActions(),
                settings.isWrapLongTextValues(), settings::setWrapLongTextValues);
        jsonValueEditorPanel.setDirtyChangeCallback(() -> {
            boolean dirty = jsonValueEditorPanel.isDirty();
            jsonTree.setDirtyNode(dirty ? jsonValueEditorPanel.getCurrentKey() : null);
        });

        resourcesScrollPane = new JScrollPane(resourcesPanel);
        resourcesScrollPane.getViewport().setOpaque(false);
        resourcesScrollPane.setOpaque(false);
        resourcesScrollPane.setBorder(null);

        contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, jsonTreePanel, resourcesScrollPane);
        contentPane.setBorder(null);
        contentPane.setDividerSize(10);
        editorToolBar = new EditorToolBar(this, jsonTree);
        projectPanel = createProjectPanel(contentPane, jsonKeyField);

        // Style the split pane divider if possible
        SplitPaneUI splitPaneUI = contentPane.getUI();
        if (splitPaneUI instanceof BasicSplitPaneUI) {
            BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPaneUI).getDivider();
            divider.setBorder(null);
            resourcesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 20));
        }

        introText = new JLabel("<html><body style=\"text-align:center; padding:30px;\">" +
                MessageBundle.get("core.intro.text") + "</body></html>");
        introText.setOpaque(true);
        introText.setFont(introText.getFont().deriveFont(28f));
        introText.setHorizontalTextPosition(JLabel.CENTER);
        introText.setVerticalTextPosition(JLabel.BOTTOM);
        introText.setHorizontalAlignment(JLabel.CENTER);
        introText.setVerticalAlignment(JLabel.CENTER);
        Color introTextColor = UIManager.getColor("Label.foreground");
        introText.setForeground(Colors.muted(introTextColor, getBackground()));
        introText.setIcon(Images.loadFromClasspath("images/icon-intro.png"));

        Container container = getContentPane();
        container.add(editorToolBar, BorderLayout.NORTH);
        container.add(introText, BorderLayout.CENTER);

        editorMenu = new EditorMenuBar(this, jsonTree);
        setJMenuBar(editorMenu);
    }

    static JPanel createProjectPanel(JSplitPane contentPane, JsonKeyField jsonKeyField) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPane, BorderLayout.CENTER);
        panel.add(jsonKeyField, BorderLayout.SOUTH);
        return panel;
    }

    static JPanel createJsonTreePanel(JTextField filterField, JsonTree jsonTree,
                                      JScrollPane jsonTreeScrollPane, Color borderColor) {
        return createJsonTreePanel(filterField, jsonTree, jsonTreeScrollPane, borderColor, null);
    }

    static JPanel createJsonTreePanel(JTextField filterField, JsonTree jsonTree,
                                      JScrollPane jsonTreeScrollPane, Color borderColor, JLabel matchLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel fixedPanel = new JPanel(new BorderLayout());
        fixedPanel.add(createTreeFilterPanel(filterField, matchLabel), BorderLayout.NORTH);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, borderColor));
        panel.add(fixedPanel, BorderLayout.NORTH);
        panel.add(jsonTreeScrollPane, BorderLayout.CENTER);
        return panel;
    }

    static JScrollPane createJsonTreeScrollPane(JsonTree jsonTree) {
        JScrollPane scrollPane = new JScrollPane(jsonTree);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(null);
        return scrollPane;
    }

    static JPanel createTreeFilterPanel(JTextField filterField) {
        return createTreeFilterPanel(filterField, null);
    }

    static JPanel createTreeFilterPanel(JTextField filterField, JLabel matchLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 10, 20));
        JLabel label = new JLabel("Find");
        label.setPreferredSize(new Dimension(56, 34));
        Dimension filterSize = filterField.getPreferredSize();
        filterField.setPreferredSize(new Dimension(filterSize.width, 34));
        panel.add(label, BorderLayout.WEST);
        panel.add(filterField, BorderLayout.CENTER);
        if (matchLabel != null) {
            matchLabel.setFont(matchLabel.getFont().deriveFont(11f));
            Color matchColor = UIManager.getColor("Label.disabledForeground");
            matchLabel.setForeground(matchColor != null ? matchColor : new Color(100, 100, 100));
            matchLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
            panel.add(matchLabel, BorderLayout.EAST);
        }
        return panel;
    }

    static JTextField createTreeFilterField(Color borderColor) {
        JTextField filterField = new TreeFilterField();
        filterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, borderColor),
                BorderFactory.createEmptyBorder(5, 8, 5, 24)));
        return filterField;
    }

    static boolean isReplaceConflict(JsonTreeNode oldNode, JsonTreeNode newNode) {
        return (newNode != null && newNode.isLeaf()) || (oldNode != null && oldNode.isLeaf());
    }

    static boolean isNewerVersion(String currentVersion, String latestVersion) {
        Integer semanticComparison = compareSemanticVersions(currentVersion, latestVersion);
        if (semanticComparison != null) {
            return semanticComparison < 0;
        }
        return normalizeVersion(currentVersion).compareToIgnoreCase(normalizeVersion(latestVersion)) < 0;
    }

    private static Integer compareSemanticVersions(String currentVersion, String latestVersion) {
        SemanticVersion current = SemanticVersion.parse(currentVersion);
        SemanticVersion latest = SemanticVersion.parse(latestVersion);
        if (current == null || latest == null) {
            return null;
        }
        for (int i = 0; i < Math.max(current.parts.length, latest.parts.length); i++) {
            int currentPart = i < current.parts.length ? current.parts[i] : 0;
            int latestPart = i < latest.parts.length ? latest.parts[i] : 0;
            if (currentPart != latestPart) {
                return Integer.compare(currentPart, latestPart);
            }
        }
        if (current.preRelease == null && latest.preRelease == null) {
            return 0;
        }
        if (current.preRelease == null) {
            return 1;
        }
        if (latest.preRelease == null) {
            return -1;
        }
        return comparePreRelease(current.preRelease, latest.preRelease);
    }

    private static int comparePreRelease(String a, String b) {
        String[] aParts = a.split("\\.");
        String[] bParts = b.split("\\.");
        for (int i = 0; i < Math.max(aParts.length, bParts.length); i++) {
            String ap = i < aParts.length ? aParts[i] : "";
            String bp = i < bParts.length ? bParts[i] : "";
            Integer an = tryParseInt(ap);
            Integer bn = tryParseInt(bp);
            int cmp = (an != null && bn != null) ? Integer.compare(an, bn) : ap.compareToIgnoreCase(bp);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeVersion(String version) {
        String normalized = version == null ? "" : version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            return normalized.substring(1);
        }
        return normalized;
    }

    private static class SemanticVersion {
        private final int[] parts;
        private final String preRelease;

        private SemanticVersion(int[] parts, String preRelease) {
            this.parts = parts;
            this.preRelease = preRelease;
        }

        private static SemanticVersion parse(String version) {
            String normalized = normalizeVersion(version);
            String[] versionParts = normalized.split("-", 2);
            String[] coreParts = versionParts[0].split("\\.");
            int[] parts = new int[coreParts.length];
            for (int i = 0; i < coreParts.length; i++) {
                if (!coreParts[i].matches("[0-9]+")) {
                    return null;
                }
                try {
                    parts[i] = Integer.parseInt(coreParts[i]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return new SemanticVersion(parts, versionParts.length == 2 ? versionParts[1] : null);
        }
    }

    private void setupFileDrop() {
        new JFileDrop(getContentPane(), null, false, files -> {
            try {
                Path path = Paths.get(files[0].getCanonicalPath());
                openPath(path, true);
            } catch (IOException e) {
                log.error("Error importing resources via file drop", e);
                showError(MessageBundle.get("file.open.error.multiple"));
            }
        });
    }

    public void openPath(Path path, boolean showError) {
        if (Files.isRegularFile(path)) {
            importJsonFile(path, showError);
        } else if (Files.isDirectory(path)) {
            importProject(path, showError);
        } else {
            removeFromHistory(path);
            if (showError) {
                showError(MessageBundle.get("file.open.error.single", path));
            }
        }
    }

    private void removeFromHistory(Path path) {
        List<String> recentDirs = settings.getHistory();
        if (recentDirs.remove(path.toString())) {
            settings.setHistory(recentDirs);
            editorMenu.setRecentItems(recentDirs.reversed());
        }
    }

    private Path withJsonExtension(Path file) {
        String name = file.getFileName().toString();
        if (name.toLowerCase().endsWith(".json")) {
            return file;
        }
        return file.resolveSibling(name + ".json");
    }

    private void setupGlobalKeyEventDispatcher() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac");
            if (e.getID() != KeyEvent.KEY_PRESSED || !e.isAltDown() ||
                    (isMac && !e.isMetaDown()) ||
                    (!isMac && !e.isControlDown())) {
                return false;
            }
            TreePath selected = jsonTree.getSelectionPath();
            if (selected == null) {
                return false;
            }
            boolean result = false;
            int row = jsonTree.getRowForPath(selected);
            switch (e.getKeyCode()) {
                case KeyEvent.VK_RIGHT:
                    if (!jsonTree.isExpanded(row)) {
                        jsonTree.expandRow(row);
                    }
                    result = true;
                    break;
                case KeyEvent.VK_LEFT:
                    if (jsonTree.isCollapsed(row)) {
                        TreePath parentPath = selected.getParentPath();
                        if (parentPath != null) {
                            jsonTree.setSelectionPath(parentPath);
                        }
                    } else {
                        jsonTree.collapseRow(row);
                    }
                    result = true;
                    break;
                case KeyEvent.VK_UP:
                    TreePath prev = jsonTree.getPathForRow(Math.max(0, row - 1));
                    if (prev != null) {
                        jsonTree.setSelectionPath(prev);
                    }
                    result = true;
                    break;
                case KeyEvent.VK_DOWN:
                    TreePath next = jsonTree.getPathForRow(row + 1);
                    if (next != null) {
                        jsonTree.setSelectionPath(next);
                    }
                    result = true;
                    break;
            }
            return result;
        });
    }

    private void setupResource(JsonDocument jsonDocument) {
        jsonDocument.addListener(e -> setDirty(true));
    }

    private void scheduleDuplicateKeyScan(JsonDocument jsonDocument) {
        jsonDocument.setDuplicateKeyScanPending(true);
        executor.submit(() -> {
            List<DuplicateJsonKey> duplicates = List.of();
            try {
                duplicates = scanDuplicateKeys(jsonDocument);
            } catch (IOException e) {
                log.warn("Could not scan duplicate JSON keys for " + jsonDocument.getPath(), e);
            }
            List<DuplicateJsonKey> result = duplicates;
            SwingUtilities.invokeLater(() -> {
                if (project != null && project.hasResource() && project.getResource() == jsonDocument
                        && jsonDocument.isDuplicateKeyScanPending()) {
                    jsonDocument.setDuplicateKeys(result);
                    jsonDocument.setDuplicateKeyScanPending(false);
                    updateUI();
                }
            });
        });
    }

    private static List<DuplicateJsonKey> scanDuplicateKeys(JsonDocument jsonDocument) throws IOException {
        return DuplicateJsonKeyDetector.findDuplicates(
                Files.readString(jsonDocument.getPath(), StandardCharsets.UTF_8));
    }

    private JsonValueEditorPanel.Actions createJsonPanelActions() {
        return new JsonValueEditorPanel.Actions() {
            @Override
            public void setLiteral(String key, String literal) {
                mutateJsonResources(mutator -> {
                    mutator.setNodeLiteral(key, literal);
                    return key;
                });
            }

            @Override
            public boolean changeType(String key, JsonNodeType type) {
                if (requiresDestructiveTypeConfirmation(key, type) && !confirmDestructiveTypeChange(key)) {
                    updateUI();
                    return false;
                }
                mutateJsonResources(mutator -> {
                    mutator.changeType(key, type);
                    return key;
                });
                return true;
            }

            @Override
            public void addChild(String key) {
                JsonTreeNode node = jsonTree.getNodeByKey(key);
                if (node == null) {
                    return;
                }
                if (node.getJsonType() == JsonNodeType.Array) {
                    mutateJsonResources(mutator -> mutator.appendArrayItem(key));
                    return;
                }
                if (node.getJsonType() != JsonNodeType.Object) {
                    return;
                }
                String name = Dialogs.showInputDialog(Editor.this,
                        "Add property", "Property name", null, JOptionPane.QUESTION_MESSAGE);
                if (!(name == null || name.isEmpty())) {
                    final String propertyName = name.trim();
                    String childKey = ResourceKeys.createJsonPath(key, propertyName);
                    if (!confirmNewKey(null, childKey)) {
                        return;
                    }
                    mutateJsonResources(mutator -> mutator.addObjectProperty(key, propertyName));
                }
            }

            @Override
            public void rename(String key) {
                JsonTreeNode node = jsonTree.getNodeByKey(key);
                JsonTreeNode parent = node == null ? null : (JsonTreeNode) node.getParent();
                if (parent != null && parent.getJsonType() == JsonNodeType.Object) {
                    String name = Dialogs.showInputDialog(Editor.this,
                            MessageBundle.get("dialogs.jsonkey.rename.title"),
                            "Property name", null, JOptionPane.QUESTION_MESSAGE, node.getName(), new JsonKeyCaret());
                    if (!(name == null || name.isEmpty())) {
                        final String propertyName = name.trim();
                        String newKey = ResourceKeys.createJsonPath(parent.getKey(), propertyName);
                        if (!confirmNewKey(key, newKey)) {
                            return;
                        }
                        mutateJsonResources(mutator -> mutator.renameObjectProperty(key, propertyName));
                    }
                } else {
                    renameSelectedKey();
                }
            }

            @Override
            public void duplicate(String key) {
                JsonTreeNode node = jsonTree.getNodeByKey(key);
                JsonTreeNode parent = node == null ? null : (JsonTreeNode) node.getParent();
                if (parent != null && parent.getJsonType() == JsonNodeType.Array) {
                    mutateJsonResources(mutator -> mutator.duplicateArrayItem(key));
                } else {
                    duplicateSelectedKey();
                }
            }

            @Override
            public void delete(String key) {
                removeSelectedKey();
            }

            @Override
            public void moveUp(String key) {
                moveJsonNode(key, -1);
            }

            @Override
            public void moveDown(String key) {
                moveJsonNode(key, 1);
            }
        };
    }

    private boolean isJsonProject() {
        return project != null;
    }

    private void mutateJsonResources(Function<JsonResourceMutator, String> mutation) {
        if (!isJsonProject()) {
            return;
        }
        JsonDocument jsonDocument = project.getResource();
        Map<String, String> before = new LinkedHashMap<>(jsonDocument.getEntries());
        String selectedKey = mutation.apply(new JsonResourceMutator(jsonDocument));
        Map<String, String> after = new LinkedHashMap<>(jsonDocument.getEntries());
        if (!before.equals(after)) {
            structuralUndoManager.addEdit(new StructuralEdit(jsonDocument, before, after, selectedKey));
        }
        refreshJsonTree(selectedKey);
        updateUndoRedoState();
    }

    /**
     * Performs a global undo. Text-field undo (in-place typing) takes priority when the
     * focused component supports it; otherwise a structural undo is performed.
     */
    public void undo(ActionEvent event) {
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (isUndoableTextComponent(focused, "undo")) {
            EditorTextActions.performTextAction(focused, event, "undo");
            return;
        }
        if (structuralUndoManager.canUndo()) {
            structuralUndoManager.undo();
            updateUndoRedoState();
        }
    }

    /**
     * Performs a global redo. Text-field redo (in-place typing) takes priority when the
     * focused component supports it; otherwise a structural redo is performed.
     */
    public void redo(ActionEvent event) {
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (isUndoableTextComponent(focused, "redo")) {
            EditorTextActions.performTextAction(focused, event, "redo");
            return;
        }
        if (structuralUndoManager.canRedo()) {
            structuralUndoManager.redo();
            updateUndoRedoState();
        }
    }

    private static boolean isUndoableTextComponent(Component focused, String actionName) {
        if (!(focused instanceof javax.swing.text.JTextComponent textComponent)) {
            return false;
        }
        javax.swing.undo.UndoManager manager = (javax.swing.undo.UndoManager)
                textComponent.getClientProperty("undoManager");
        if (manager != null) {
            return "undo".equals(actionName) ? manager.canUndo() : manager.canRedo();
        }
        return textComponent.getActionMap().get(actionName) != null;
    }

    private void updateUndoRedoState() {
        if (editorMenu != null) {
            editorMenu.setUndoRedoEnabled(structuralUndoManager.canUndo(), structuralUndoManager.canRedo());
        }
        if (editorToolBar != null) {
            editorToolBar.setUndoRedoEnabled(structuralUndoManager.canUndo(), structuralUndoManager.canRedo());
        }
    }

    private void applyStructuralSnapshot(JsonDocument jsonDocument, Map<String, String> snapshot, String selectedKey) {
        jsonDocument.replaceEntries(new LinkedHashMap<>(snapshot));
        refreshJsonTree(selectedKey);
    }

    private class StructuralEdit extends AbstractUndoableEdit {
        @Serial
        private static final long serialVersionUID = 1L;
        private final transient JsonDocument jsonDocument;
        private final transient Map<String, String> before;
        private final transient Map<String, String> after;
        private final transient String selectedKey;

        StructuralEdit(JsonDocument jsonDocument, Map<String, String> before, Map<String, String> after, String selectedKey) {
            this.jsonDocument = jsonDocument;
            this.before = before;
            this.after = after;
            this.selectedKey = selectedKey;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            applyStructuralSnapshot(jsonDocument, before, selectedKey);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            applyStructuralSnapshot(jsonDocument, after, selectedKey);
        }
    }

    private void refreshJsonTree(String selectedKey) {
        if (project == null) {
            return;
        }
        List<String> expandedKeys = jsonTree.getExpandedNodes().stream()
                .map(JsonTreeNode::getKey)
                .toList();
        Map<String, String> keys = new LinkedHashMap<>(project.getResource().getEntries());
        jsonTree.setModel(new JsonTreeModel(keys));
        List<JsonTreeNode> expandedNodes = expandedKeys.stream()
                .map(jsonTree::getNodeByKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        jsonTree.expand(expandedNodes);
        updateTreeNodeStatuses();
        if (selectedKey != null) {
            JsonTreeNode selectedNode = jsonTree.getNodeByKey(selectedKey);
            if (selectedNode == null) {
                selectedNode = jsonTree.getSourceModel().getClosestParentNodeByKey(selectedKey);
            }
            jsonTree.setSelectionNode(selectedNode);
        }
        updateUI();
    }

    private boolean requiresDestructiveTypeConfirmation(String key, JsonNodeType newType) {
        JsonTreeNode node = jsonTree.getNodeByKey(key);
        if (node == null || node.getJsonChildCount() == 0) {
            return false;
        }
        boolean fromContainer = node.getJsonType() == JsonNodeType.Object || node.getJsonType() == JsonNodeType.Array;
        boolean toPrimitive = newType == JsonNodeType.String || newType == JsonNodeType.Number
                || newType == JsonNodeType.Boolean || newType == JsonNodeType.Null;
        return fromContainer && toPrimitive;
    }

    private boolean confirmDestructiveTypeChange(String key) {
        return Dialogs.showConfirmDialog(this,
                "Change JSON type",
                "Changing " + key + " to a primitive value will delete its child nodes.",
                JOptionPane.WARNING_MESSAGE);
    }

    private boolean confirmDelete(JsonTreeNode node) {
        if (deleteConfirmation == null) {
            deleteConfirmation = this::confirmDeleteNode;
        }
        return deleteConfirmation.confirm(node);
    }

    private boolean confirmDeleteNode(JsonTreeNode node) {
        return Dialogs.showConfirmDialog(this,
                "Delete JSON node",
                deleteConfirmationMessage(node),
                JOptionPane.WARNING_MESSAGE);
    }

    static String deleteConfirmationMessage(JsonTreeNode node) {
        JsonTreeNode parent = node == null ? null : (JsonTreeNode) node.getParent();
        if (parent != null && parent.getJsonType() == JsonNodeType.Array) {
            return "Delete item " + node.getName() + " from \"" + parent.getName() + "\"?";
        }
        int descendants = descendantCount(node);
        String name = "\"" + node.getName() + "\"";
        if (descendants > 0) {
            return "Delete " + name + " and its " + descendants + " child "
                    + (descendants == 1 ? "node" : "nodes") + "?";
        }
        return "Delete " + name + "?";
    }

    private static int descendantCount(JsonTreeNode node) {
        if (node == null) {
            return 0;
        }
        int count = 0;
        for (JsonTreeNode child : node.getChildren()) {
            count += 1 + descendantCount(child);
        }
        return count;
    }

    private void moveJsonNode(String key, int delta) {
        JsonTreeNode node = jsonTree.getNodeByKey(key);
        JsonTreeNode parent = node == null ? null : (JsonTreeNode) node.getParent();
        if (parent == null) {
            return;
        }
        if (parent.getJsonType() == JsonNodeType.Array) {
            mutateJsonResources(mutator -> mutator.moveArrayItem(key, delta));
        } else if (parent.getJsonType() == JsonNodeType.Object) {
            mutateJsonResources(mutator -> {
                mutator.moveObjectProperty(key, delta);
                return key;
            });
        }
    }

    private void updateHistory() {
        List<String> recentDirs = settings.getHistory();
        if (project != null) {
            String path = project.getPath().toString();
            recentDirs.remove(path);
            recentDirs.add(path);
            if (recentDirs.size() > 10) {
                recentDirs.removeFirst();
            }
            settings.setHistory(recentDirs);
        }
        editorMenu.setRecentItems(recentDirs.reversed());
    }

    private void updateTitle() {
        if (project == null) {
            setTitle(TITLE);
            return;
        }
        String filename = project.getPath().getFileName().toString();
        String dirtyMark = dirty ? " *" : "";
        setTitle(TITLE + " - " + filename + dirtyMark);
    }

    private void showError(String message) {
        Dialogs.showErrorDialog(this, MessageBundle.get("dialogs.error.title"), message);
    }

    private void showJsonLoadError(Path file, JsonLoadException ex) {
        JsonParseError err = ex.getParseError();
        StringBuilder summary = new StringBuilder(MessageBundle.get("file.open.error.malformed.summary", file));
        StringBuilder extra = new StringBuilder();
        if (err.line() > 0) {
            extra.append("<br><br>").append(MessageBundle.get("file.open.error.malformed.locationLine",
                    err.line(), err.column()));
        }
        if (!err.path().isEmpty()) {
            extra.append("<br>").append(MessageBundle.get("file.open.error.malformed.path", err.path()));
        }
        // Inject the location/path inside the single <html> document so the JLabel renders it.
        int close = summary.lastIndexOf("</html>");
        if (close >= 0 && !extra.isEmpty()) {
            summary.insert(close, extra);
        }
        String detail = err.message();
        if (!err.snippet().isEmpty()) {
            detail = err.message() + "\n\n" + err.snippet();
        }
        Dialogs.showDetailedErrorDialog(this, MessageBundle.get("dialogs.error.title"),
                summary.toString(), detail);
    }

    static JPanel createDuplicateKeyWarningPanel(JsonDocument jsonDocument, Runnable dismissAction) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                JsonValueEditorPanel.sectionBoxBorder(),
                BorderFactory.createEmptyBorder(10, 12, 10, 10)));
        panel.setOpaque(false);
        JLabel label = new JLabel(MessageBundle.get("dialogs.duplicatekeys.warning.text",
                duplicateKeySummary(jsonDocument)));
        label.setForeground(UIManager.getColor("Label.foreground"));
        label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() * 1.08f));
        panel.add(label, BorderLayout.CENTER);
        JButton close = new JButton("X");
        close.setToolTipText(MessageBundle.get("dialogs.duplicatekeys.dismiss.tooltip"));
        close.setFocusPainted(false);
        close.setMargin(new Insets(1, 6, 1, 6));
        close.setPreferredSize(new Dimension(32, 28));
        close.addActionListener(e -> dismissAction.run());
        JPanel closeHolder = new JPanel(new BorderLayout());
        closeHolder.setOpaque(false);
        closeHolder.add(close, BorderLayout.NORTH);
        panel.add(closeHolder, BorderLayout.EAST);
        return panel;
    }

    private static String duplicateKeySummary(JsonDocument jsonDocument) {
        List<DuplicateJsonKey> duplicates = jsonDocument.getDuplicateKeys();
        if (duplicates.isEmpty()) {
            return "";
        }
        DuplicateJsonKey first = duplicates.getFirst();
        if (duplicates.size() == 1) {
            return first.path();
        }
        return first.path() + " +" + (duplicates.size() - 1);
    }

    private void updateTreeNodeStatuses() {
        // single resource — no cross-resource completeness check needed
    }

    private boolean saveResource(JsonDocument jsonDocument) {
        if (project != null) {
            completeDuplicateKeyScanBeforeSave(jsonDocument);
            return saveResource(jsonDocument, !project.isMinifyResources(), project.isFlattenJSON(),
                    JsonIO::write,
                    () -> Dialogs.showConfirmDialog(this,
                            MessageBundle.get("dialogs.save.checksum.title"),
                            MessageBundle.get("dialogs.save.checksum.text", jsonDocument.getPath()),
                            JOptionPane.WARNING_MESSAGE),
                    () -> Dialogs.showConfirmDialog(this,
                            MessageBundle.get("dialogs.save.duplicatekeys.title"),
                            MessageBundle.get("dialogs.save.duplicatekeys.text", duplicateKeySummary(jsonDocument)),
                            JOptionPane.WARNING_MESSAGE),
                    e -> {
                        log.error("Error saving resource file " + jsonDocument.getPath(), e);
                        showError(MessageBundle.get("file.write.error.single", jsonDocument.getPath().toString()));
                    });
        }
        return true;
    }

    private void completeDuplicateKeyScanBeforeSave(JsonDocument jsonDocument) {
        if (!jsonDocument.isDuplicateKeyScanPending()) {
            return;
        }
        try {
            jsonDocument.setDuplicateKeys(scanDuplicateKeys(jsonDocument));
        } catch (IOException e) {
            log.warn("Could not complete duplicate JSON key scan before save for " + jsonDocument.getPath(), e);
        } finally {
            jsonDocument.setDuplicateKeyScanPending(false);
        }
    }

    static boolean saveResource(JsonDocument jsonDocument, boolean prettyPrinting, boolean flattenJson,
                                ResourceWriter writer, BooleanSupplier confirmChecksumOverwrite, Consumer<IOException> ioErrorHandler) {
        return saveResource(jsonDocument, prettyPrinting, flattenJson, writer,
                confirmChecksumOverwrite, () -> true, ioErrorHandler);
    }

    static boolean saveResource(JsonDocument jsonDocument, boolean prettyPrinting, boolean flattenJson,
                                ResourceWriter writer, BooleanSupplier confirmChecksumOverwrite,
                                BooleanSupplier confirmDuplicateKeySave, Consumer<IOException> ioErrorHandler) {
        if (jsonDocument.hasDuplicateKeys() && !confirmDuplicateKeySave.getAsBoolean()) {
            return false;
        }
        try {
            writer.write(jsonDocument, prettyPrinting, flattenJson);
            jsonDocument.clearDuplicateKeys();
            return true;
        } catch (ChecksumException e) {
            if (!confirmChecksumOverwrite.getAsBoolean()) {
                return false;
            }
            jsonDocument.setChecksum(null);
            return saveResource(jsonDocument, prettyPrinting, flattenJson,
                    writer, confirmChecksumOverwrite, ioErrorHandler);
        } catch (IOException e) {
            ioErrorHandler.accept(e);
            return false;
        }
    }

    private void storeEditorState() {
        ExtendedProperties props = new ExtendedProperties();
        props.setProperty("window_width", getWidth());
        props.setProperty("window_height", getHeight());
        props.setProperty("window_pos_x", getX());
        props.setProperty("window_pos_y", getY());
        props.setProperty("window_div_pos", contentPane.getDividerLocation());
        props.setProperty("minify_resources", settings.isMinifyResources());
        props.setProperty("flatten_json", settings.isFlattenJSON());
        props.setProperty("check_version", settings.isCheckVersionOnStartup());
        props.setProperty("double_click_tree_toggling", settings.isDoubleClickTreeToggling());
        props.setProperty("wrap_long_text_values", settings.isWrapLongTextValues());
        props.setProperty("dark_theme", settings.isDarkTheme());
        if (settings.getEditorLanguage() != null) {
            props.setProperty("editor_language", settings.getEditorLanguage());
        }
        if (!settings.getHistory().isEmpty()) {
            props.setProperty("history", settings.getHistory());
        }
        if (project != null) {
            // Store keys of expanded nodes (temporarily clear filter so all nodes are visible)
            String savedFilter = jsonTree.getFilterText();
            if (!savedFilter.isEmpty()) {
                jsonTree.setFilterText("");
            }
            List<String> expandedNodeKeys = jsonTree.getExpandedNodes().stream()
                    .map(JsonTreeNode::getKey)
                    .collect(Collectors.toList());
            if (!savedFilter.isEmpty()) {
                jsonTree.setFilterText(savedFilter);
            }
            props.setProperty("last_expanded", expandedNodeKeys);
            // Store key of selected node
            JsonTreeNode selectedNode = jsonTree.getSelectionNode();
            props.setProperty("last_selected", selectedNode == null ? "" : selectedNode.getKey());
        }
        props.store(Paths.get(SETTINGS_DIR, SETTINGS_FILE));
    }

    private void restoreEditorState() {
        ExtendedProperties props = new ExtendedProperties();
        props.load(Paths.get(SETTINGS_DIR, SETTINGS_FILE));
        settings.setWindowWidth(props.getIntegerProperty("window_width", 1024));
        settings.setWindowHeight(props.getIntegerProperty("window_height", 768));
        settings.setWindowPositionX(props.getIntegerProperty("window_pos_x", 0));
        settings.setWindowPositionY(props.getIntegerProperty("window_pos_y", 0));
        settings.setWindowDividerPosition(props.getIntegerProperty("window_div_pos", 250));
        settings.setCheckVersionOnStartup(props.getBooleanProperty("check_version", true));
        settings.setDoubleClickTreeToggling(props.getBooleanProperty("double_click_tree_toggling", false));
        settings.setWrapLongTextValues(props.getBooleanProperty("wrap_long_text_values", false));
        settings.setDarkTheme(props.getBooleanProperty("dark_theme", false));
        settings.setMinifyResources(props.getBooleanProperty("minify_resources", false));
        settings.setFlattenJSON(props.getBooleanProperty("flatten_json", false));
        settings.setHistory(props.getListProperty("history"));
        settings.setLastExpandedNodes(props.getListProperty("last_expanded"));
        settings.setLastSelectedNode(props.getProperty("last_selected"));
        settings.setEditorLanguage(props.getLocaleProperty("editor_language"));
    }

    private class JsonTreeMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            showPopupMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showPopupMenu(e);
        }

        private void showPopupMenu(MouseEvent e) {
            if (!e.isPopupTrigger() || project == null || !project.hasResource()) {
                return;
            }
            TreePath path = jsonTree.getPathForLocation(e.getX(), e.getY());
            if (path == null) {
                JsonTreeMenu menu = new JsonTreeMenu(Editor.this, jsonTree);
                menu.show(e.getComponent(), e.getX(), e.getY());
            } else {
                jsonTree.setSelectionPath(path);
                JsonTreeNode node = jsonTree.getSelectionNode();
                JsonTreeNodeMenu menu = new JsonTreeNodeMenu(Editor.this, node);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private class JsonTreeNodeSelectionListener implements TreeSelectionListener {
        private boolean restoringSelection = false;

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (restoringSelection) return;
            if (jsonValueEditorPanel != null && jsonValueEditorPanel.hasInvalidEdit()) {
                boolean discard = Dialogs.showConfirmDialog(Editor.this,
                        "Invalid Edit",
                        "The current edit for '" + jsonValueEditorPanel.getCurrentKey() + "' is not valid JSON.\nDiscard and continue?",
                        JOptionPane.WARNING_MESSAGE);
                if (!discard) {
                    restoringSelection = true;
                    try {
                        TreePath oldPath = e.getOldLeadSelectionPath();
                        if (oldPath != null) jsonTree.setSelectionPath(oldPath);
                        else jsonTree.clearSelection();
                    } finally {
                        restoringSelection = false;
                    }
                    return;
                }
            }
            assert jsonValueEditorPanel != null;
            jsonValueEditorPanel.applyValue();
            JsonTreeNode node = jsonTree.getSelectionNode();

            if (node != null) {
                // Store scroll position
                int scrollValue = resourcesScrollPane.getVerticalScrollBar().getValue();

                // Update UI values
                String key = node.getKey();
                jsonKeyField.setValue(key);
                if (isJsonProject()) {
                    jsonValueEditorPanel.setNode(node,
                            project.hasResource() ? List.of(project.getResource()) : List.of());
                }

                // Restore scroll position and focus
                SwingUtilities.invokeLater(() -> resourcesScrollPane.getVerticalScrollBar().setValue(scrollValue));
            }
        }
    }

    private class JsonKeyFieldKeyListener extends KeyAdapter {
        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                JsonKeyField field = (JsonKeyField) e.getSource();
                String value = field.getValue();
                if (value.isEmpty()) return;
                addKey(value);
            }
        }
    }

    private class TreeFilterDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            applyFilter();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            applyFilter();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            applyFilter();
        }

        private void applyFilter() {
            jsonTree.setFilterText(treeFilterField.getText());
            String filter = treeFilterField.getText();
            if (filter.isEmpty()) {
                filterMatchLabel.setText("");
            } else {
                int count = jsonTree.getVisibleLeafCount();
                filterMatchLabel.setText(count + (count == 1 ? " match" : " matches"));
            }
        }
    }

    private class EditorWindowListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            if (closeCurrentProject()) {
                storeEditorState();
                System.exit(0);
            }
        }
    }
}
