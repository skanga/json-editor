package com.skanga.jsoneditor.editor;

import java.util.List;
import java.util.Locale;

/**
 * This class represents the editor settings.
 */
public class EditorSettings {
	private int windowPositionX;
	private int windowPositionY;
	private int windowDividerPosition;
	private int windowWidth;
	private int windowHeight;
	private boolean minifyResources;
	private boolean flattenJSON;
	private List<String> history;
	private List<String> lastExpandedNodes;
	private String lastSelectedNode;
	private boolean checkVersionOnStartup;
	private boolean doubleClickTreeToggling;
	private boolean wrapLongTextValues;
	private boolean darkTheme;
	private Locale editorLanguage;

	public int getWindowPositionX() {
		return windowPositionX;
	}
	
	public void setWindowPositionX(int windowPositionX) {
		this.windowPositionX = windowPositionX;
	}
	
	public int getWindowPositionY() {
		return windowPositionY;
	}
	
	public void setWindowPositionY(int windowPositionY) {
		this.windowPositionY = windowPositionY;
	}
	
	public int getWindowDividerPosition() {
		return windowDividerPosition;
	}
	
	public void setWindowDividerPosition(int dividerPosition) {
		this.windowDividerPosition = dividerPosition;
	}
	
	public int getWindowWidth() {
		return windowWidth;
	}
	
	public void setWindowWidth(int width) {
		this.windowWidth = width;
	}
	
	public int getWindowHeight() {
		return windowHeight;
	}
	
	public void setWindowHeight(int height) {
		this.windowHeight = height;
	}

	public List<String> getHistory() {
		return history;
	}

	public void setHistory(List<String> history) {
		this.history = history;
	}
	
	public List<String> getLastExpandedNodes() {
		return lastExpandedNodes;
	}

	public void setLastExpandedNodes(List<String> lastExpandedNodes) {
		this.lastExpandedNodes = lastExpandedNodes;
	}

	public String getLastSelectedNode() {
		return lastSelectedNode;
	}

	public void setLastSelectedNode(String lastSelectedNode) {
		this.lastSelectedNode = lastSelectedNode;
	}

	public boolean isMinifyResources() {
		return minifyResources;
	}

	public void setMinifyResources(boolean minifyResources) {
		this.minifyResources = minifyResources;
	}

	public boolean isCheckVersionOnStartup() {
		return checkVersionOnStartup;
	}

	public void setCheckVersionOnStartup(boolean checkVersionOnStartup) {
		this.checkVersionOnStartup = checkVersionOnStartup;
	}

	public boolean isDoubleClickTreeToggling() {
		return doubleClickTreeToggling;
	}

	public void setDoubleClickTreeToggling(boolean doubleClickTreeToggling) {
		this.doubleClickTreeToggling = doubleClickTreeToggling;
	}

	public boolean isWrapLongTextValues() {
		return wrapLongTextValues;
	}

	public void setWrapLongTextValues(boolean wrapLongTextValues) {
		this.wrapLongTextValues = wrapLongTextValues;
	}
	
	public boolean isFlattenJSON() {
		return flattenJSON;
	}

	public void setFlattenJSON(boolean flattenJSON) {
		this.flattenJSON = flattenJSON;
	}

	public boolean isDarkTheme() {
		return darkTheme;
	}

	public void setDarkTheme(boolean darkTheme) {
		this.darkTheme = darkTheme;
	}

	public Locale getEditorLanguage() {
		return editorLanguage;
	}

	public void setEditorLanguage(Locale editorLanguage) {
		this.editorLanguage = editorLanguage;
	}
}
