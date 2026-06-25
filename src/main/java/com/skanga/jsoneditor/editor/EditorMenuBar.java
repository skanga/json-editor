package com.skanga.jsoneditor.editor;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import com.skanga.jsoneditor.util.GithubRepoUtil;
import com.skanga.jsoneditor.util.MessageBundle;

/**
 * This class represents the top bar menu of the editor.
 */
public class EditorMenuBar extends JMenuBar {
	@Serial
    private final static long serialVersionUID = -101788804096708514L;
	
	private final Editor editor;
	private final JsonTree tree;
	private JMenuItem saveMenuItem;
	private JMenuItem reloadMenuItem;
	private JMenuItem closeMenuItem;
	private JMenuItem addKeyMenuItem;
	private JMenuItem findKeyMenuItem;
	private JMenuItem renameKeyMenuItem;
	private JMenuItem copyKeyMenuItem;
	private JMenuItem duplicateKeyMenuItem;
	private JMenuItem undoMenuItem;
	private JMenuItem redoMenuItem;
	private JMenuItem removeKeyMenuItem;
	private JMenuItem expandAllMenuItem;
	private JMenuItem collapseAllMenuItem;
	private JMenuItem openContainingFolderMenuItem;
	private JMenuItem projectSettingsMenuItem;
	private JMenuItem editorSettingsMenuItem;
	private JMenu openRecentMenuItem;
	private JMenu editMenu;
	private JMenu viewMenu;
	private JMenu settingsMenu;
	private Boolean menuEnabled = null;
	private final JSeparator settingsMenuSeparator = new JSeparator();
	
	public EditorMenuBar(Editor editor, JsonTree tree) {
		super();
		this.editor = editor;
		this.tree = tree;
		setupUI();
		setEnabled(false);
		setSavable(false);
		setEditable(false);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		if (menuEnabled != null && menuEnabled.equals(enabled)) return;
		menuEnabled = enabled;
		reloadMenuItem.setEnabled(enabled);
		closeMenuItem.setEnabled(enabled);
		openContainingFolderMenuItem.setEnabled(enabled);
		expandAllMenuItem.setEnabled(enabled);
		collapseAllMenuItem.setEnabled(enabled);
		editMenu.setEnabled(enabled);
		viewMenu.setEnabled(enabled);
		settingsMenu.removeAll();
		if (enabled) {
			settingsMenu.add(projectSettingsMenuItem);
			settingsMenu.add(settingsMenuSeparator);
			settingsMenu.add(editorSettingsMenuItem);
		} else {
			settingsMenu.add(editorSettingsMenuItem);
		}
	}
	
	public void setSavable(boolean savable) {
		saveMenuItem.setEnabled(savable);
	}
	
	public void setEditable(boolean editable) {
		addKeyMenuItem.setEnabled(editable);
		findKeyMenuItem.setEnabled(editable);
	}

	public void setUndoRedoEnabled(boolean canUndo, boolean canRedo) {
		undoMenuItem.setEnabled(canUndo);
		redoMenuItem.setEnabled(canRedo);
	}
	
	public void setRecentItems(List<String> items) {
		openRecentMenuItem.removeAll();
     	if (items.isEmpty()) {
     		openRecentMenuItem.setEnabled(false);
     	} else {
     		openRecentMenuItem.setEnabled(true);
     		for (int i = 0; i < items.size(); i++) {
     			int n = i + 1;
     			JMenuItem menuItem = new JMenuItem(n + ": " + items.get(i), Character.forDigit(n, 10));
     			menuItem.putClientProperty("recentPath", items.get(i));
     			menuItem.addActionListener(e -> {
     				String storedPath = (String) ((JMenuItem) e.getSource()).getClientProperty("recentPath");
     				if (storedPath != null) {
     					editor.openPath(Paths.get(storedPath), true);
     				}
     			});
     			openRecentMenuItem.add(menuItem);
     		}
     		JMenuItem clearMenuItem = new JMenuItem(MessageBundle.get("menu.file.recent.clear.title"));
     		clearMenuItem.addActionListener(e -> editor.clearHistory());
     		openRecentMenuItem.addSeparator();
     		openRecentMenuItem.add(clearMenuItem);
     	}
	}
	
	private void setupUI() {
		int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		configureBorder(this);
		
		// File menu
     	JMenu fileMenu = new JMenu(MessageBundle.get("menu.file.title"));
     	fileMenu.setMnemonic(MessageBundle.getMnemonic("menu.file.vk"));
        
     	JMenuItem createMenuItem = new JMenuItem(EditorCommand.NEW.text());
     	EditorCommand.NEW.applyTo(createMenuItem);
     	createMenuItem.setMnemonic(MessageBundle.getMnemonic("menu.file.project.new.vk"));
     	createMenuItem.addActionListener(e -> editor.showCreateJsonFileDialog());
     	createMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, keyMask));
     	
        JMenuItem importMenuItem = new JMenuItem(EditorCommand.OPEN.text());
        EditorCommand.OPEN.applyTo(importMenuItem);
        importMenuItem.setMnemonic(MessageBundle.getMnemonic("menu.file.project.import.vk"));
        importMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, keyMask));
        importMenuItem.addActionListener(e -> editor.showImportProjectDialog());

        openContainingFolderMenuItem = new JMenuItem(MessageBundle.get("menu.file.folder.title"));
        openContainingFolderMenuItem.setMnemonic(KeyEvent.VK_F);
        openContainingFolderMenuItem.addActionListener(e -> editor.openProjectDirectory());
        
        openRecentMenuItem = new JMenu(MessageBundle.get("menu.file.recent.title"));
        openRecentMenuItem.setMnemonic(MessageBundle.getMnemonic("menu.file.recent.vk"));
        
        saveMenuItem = new JMenuItem(EditorCommand.SAVE.text(), MessageBundle.getMnemonic("menu.file.save.vk"));
        EditorCommand.SAVE.applyTo(saveMenuItem);
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, keyMask));
        saveMenuItem.addActionListener(e -> editor.saveProject());
        
        reloadMenuItem = new JMenuItem(MessageBundle.get("menu.file.reload.title"), MessageBundle.getMnemonic("menu.file.reload.vk"));
        reloadMenuItem.addActionListener(e -> editor.reloadProject());

        closeMenuItem = new JMenuItem(EditorCommand.CLOSE.text());
        EditorCommand.CLOSE.applyTo(closeMenuItem);
        closeMenuItem.setMnemonic(KeyEvent.VK_C);
        closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, keyMask));
        closeMenuItem.addActionListener(e -> editor.closeFile());
        
        JMenuItem exitMenuItem = new JMenuItem(MessageBundle.get("menu.file.exit.title"), MessageBundle.getMnemonic("menu.file.exit.vk"));
        exitMenuItem.addActionListener(e -> editor.dispatchEvent(new WindowEvent(editor, WindowEvent.WINDOW_CLOSING)));
        
        fileMenu.add(createMenuItem);
        fileMenu.add(importMenuItem);
        if (Desktop.isDesktopSupported()) {
	        fileMenu.add(openContainingFolderMenuItem);
		}
        fileMenu.add(openRecentMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(saveMenuItem);
        fileMenu.add(reloadMenuItem);
        fileMenu.add(closeMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        
        // Edit menu
     	editMenu = new JMenu(MessageBundle.get("menu.edit.title"));
     	editMenu.setMnemonic(MessageBundle.getMnemonic("menu.edit.vk"));
     	
        addKeyMenuItem = EditorMenuItems.addKey(editor, tree, false);
        findKeyMenuItem = EditorMenuItems.findKey(editor, false);
        removeKeyMenuItem = EditorMenuItems.removeKey(editor, false);
        duplicateKeyMenuItem = EditorMenuItems.duplicateKey(editor, false);
        renameKeyMenuItem = EditorMenuItems.renameKey(editor, false, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        copyKeyMenuItem = EditorMenuItems.copyKey(editor, false);
        
        undoMenuItem = new JMenuItem(EditorCommand.UNDO.text());
        EditorCommand.UNDO.applyTo(undoMenuItem);
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, keyMask));
        undoMenuItem.addActionListener(e -> { if (editor != null) editor.undo(e); });

        redoMenuItem = new JMenuItem(EditorCommand.REDO.text());
        EditorCommand.REDO.applyTo(redoMenuItem);
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, keyMask | InputEvent.SHIFT_DOWN_MASK));
        redoMenuItem.addActionListener(e -> { if (editor != null) editor.redo(e); });

        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem);
        editMenu.addSeparator();
        editMenu.add(addKeyMenuItem);
        editMenu.add(findKeyMenuItem);
        editMenu.addSeparator();
        editMenu.add(renameKeyMenuItem);
        editMenu.add(duplicateKeyMenuItem);
        editMenu.add(removeKeyMenuItem);
        editMenu.add(copyKeyMenuItem);
        
        // View menu
        viewMenu = new JMenu(MessageBundle.get("menu.view.title"));
        viewMenu.setMnemonic(MessageBundle.getMnemonic("menu.view.vk"));
        expandAllMenuItem = EditorMenuItems.expandAll(tree);
        collapseAllMenuItem = EditorMenuItems.collapseAll(tree);
        viewMenu.add(expandAllMenuItem);
        viewMenu.add(collapseAllMenuItem);
        
        // Settings menu
        settingsMenu = new JMenu(MessageBundle.get("menu.settings.title"));
        settingsMenu.setMnemonic(MessageBundle.getMnemonic("menu.settings.vk"));
        
        editorSettingsMenuItem = new JMenuItem(MessageBundle.get("menu.settings.preferences.editor.title"));
        editorSettingsMenuItem.setMnemonic(KeyEvent.VK_E);
        editorSettingsMenuItem.addActionListener(e -> {
        	EditorSettingsPane pane = new EditorSettingsPane(editor);
        	int result = JOptionPane.showConfirmDialog(editor,
        			pane,
        			MessageBundle.get("dialogs.preferences.editor.title"),
        			JOptionPane.OK_CANCEL_OPTION,
        			JOptionPane.PLAIN_MESSAGE);
        	if (result == JOptionPane.OK_OPTION) {
        		pane.applyTo(editor);
        	}
        });
        
        projectSettingsMenuItem = new JMenuItem(MessageBundle.get("menu.settings.preferences.project.title"));
        projectSettingsMenuItem.setMnemonic(KeyEvent.VK_P);
        projectSettingsMenuItem.addActionListener(e -> {
        	EditorProject project = editor.getProject();
        	// Snapshot current project values before showing dialog
        	boolean snapProjectMinify = project.isMinifyResources();
        	boolean snapProjectFlattenJSON = project.isFlattenJSON();

        	EditorProjectSettingsPane pane = new EditorProjectSettingsPane(editor);
        	int result = JOptionPane.showConfirmDialog(editor,
        			pane,
        			MessageBundle.get("dialogs.preferences.project.title"),
        			JOptionPane.OK_CANCEL_OPTION,
        			JOptionPane.PLAIN_MESSAGE);
        	if (result != JOptionPane.OK_OPTION) {
        		// Restore all snapshotted project values
        		project.setMinifyResources(snapProjectMinify);
        		project.setFlattenJSON(snapProjectFlattenJSON);
        	}
        });
        
        settingsMenu.add(editorSettingsMenuItem);
        
        // Help menu
     	JMenu helpMenu = new JMenu(MessageBundle.get("menu.help.title"));
     	helpMenu.setMnemonic(MessageBundle.getMnemonic("menu.help.vk"));
     	
     	JMenuItem versionMenuItem = new JMenuItem(MessageBundle.get("menu.help.version.title"));
     	versionMenuItem.setMnemonic(KeyEvent.VK_V);
     	versionMenuItem.addActionListener(e -> editor.showVersionDialog(false));

     	JMenuItem homeMenuItem = new JMenuItem(MessageBundle.get("menu.help.home.title", Editor.TITLE));
     	homeMenuItem.setMnemonic(KeyEvent.VK_H);
     	homeMenuItem.addActionListener(e -> openHomePage());
     	
     	JMenuItem aboutMenuItem = new JMenuItem(MessageBundle.get("menu.help.about.title", Editor.TITLE));
     	aboutMenuItem.setMnemonic(KeyEvent.VK_A);
     	aboutMenuItem.addActionListener(e -> editor.showAboutDialog());
     	
     	helpMenu.add(versionMenuItem);
     	helpMenu.addSeparator();
     	helpMenu.add(homeMenuItem);
     	helpMenu.add(aboutMenuItem);
     	
     	add(fileMenu);
     	add(editMenu);
     	add(viewMenu);
     	add(settingsMenu);
     	add(helpMenu);

     	tree.addTreeSelectionListener(e -> {
     		JsonTreeNode node = tree.getSelectionNode();
     		boolean enabled = node != null && !node.isRoot();
			renameKeyMenuItem.setEnabled(enabled);
			copyKeyMenuItem.setEnabled(enabled);
			duplicateKeyMenuItem.setEnabled(enabled);
			removeKeyMenuItem.setEnabled(enabled);
     	});
	}
	
	/** Opens the project home page; if no browser can be launched, copies the URL and tells the user. */
	private void openHomePage() {
		String url = GithubRepoUtil.getURL(Editor.GITHUB_USER, Editor.GITHUB_PROJECT);
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(URI.create(url));
				return;
			}
		} catch (IOException | RuntimeException ex) {
			// fall through to the clipboard fallback below
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
		JOptionPane.showMessageDialog(this,
				MessageBundle.get("menu.help.home.error", url),
				MessageBundle.get("dialogs.error.title"),
				JOptionPane.INFORMATION_MESSAGE);
	}

	static void configureBorder(JMenuBar menuBar) {
		menuBar.setBorder(null);
		menuBar.setBorderPainted(false);
	}
}
