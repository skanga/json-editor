package com.skanga.jsoneditor.swing.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.text.Caret;

import com.skanga.jsoneditor.swing.JHelpLabel;
import com.skanga.jsoneditor.swing.JHtmlPane;
import com.skanga.jsoneditor.swing.JTextField;
import com.skanga.jsoneditor.swing.event.RequestInitialFocusListener;
import com.skanga.jsoneditor.swing.text.BlinkCaret;

/**
 * This class provides utility functions for dialogs using {@link JOptionPane}.
 */
public final class Dialogs {
	
	public static void showErrorDialog(Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
	}

	public static void showDetailedErrorDialog(Component parent, String title, String summary, String monospaceDetail) {
		JPanel content = new JPanel(new BorderLayout(0, 10));
		content.add(new JLabel(summary), BorderLayout.NORTH);

		if (monospaceDetail != null && !monospaceDetail.isEmpty()) {
			JTextArea detail = new JTextArea(monospaceDetail);
			detail.setEditable(false);
			detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, detail.getFont().getSize()));
			JScrollPane scroll = new JScrollPane(detail);
			scroll.setPreferredSize(new Dimension(480, 120));
			content.add(scroll, BorderLayout.CENTER);
		}

		JOptionPane.showMessageDialog(parent, content, title, JOptionPane.ERROR_MESSAGE);
	}

	public static void showWarningDialog(Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
	}
	
	public static void showInfoDialog(Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
	}
	
	public static void showMessageDialog(Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.PLAIN_MESSAGE);
	}
	
	public static void showComponentDialog(Component parent, String title, Component component) {
		JOptionPane.showMessageDialog(parent, component, title, JOptionPane.PLAIN_MESSAGE);
	}
	
	public static void showHtmlDialog(Component parent, String title, String body) {
		Font font = parent.getFont();
		JHtmlPane pane = new JHtmlPane(parent, "<html><body style=\"font-family:" + 
				font.getFamily() + ";text-align:center;\">" + body + "</body></html>");
		pane.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
		showComponentDialog(parent, title, pane);	
	}
	
	public static boolean showConfirmDialog(Component parent, String title, String message, int type) {
		String yes = UIManager.getString("OptionPane.yesButtonText");
		String no = UIManager.getString("OptionPane.noButtonText");
		Object[] options = { yes, no };
		int result = JOptionPane.showOptionDialog(parent, message, title, JOptionPane.YES_NO_OPTION, type, null, options, no);
		return result == JOptionPane.YES_OPTION;
	}
	
	public static String showInputDialog(Component parent, String title, String label, String help, int type) {
		return showInputDialog(parent, title, label, help, type, null, new BlinkCaret());
	}
	
	public static String showInputDialog(Component parent, String title, String label, String help, int type, String initialText, Caret caret) {
		JPanel content = new JPanel(new GridLayout(0, 1));

		JLabel promptLabel = null;
		if (!(label == null || label.isEmpty())) {
			promptLabel = new JLabel(label);
			content.add(promptLabel);
		}

		JTextField field =  new JTextField(initialText);
		if (promptLabel != null) {
			promptLabel.setLabelFor(field);
		}
		field.addAncestorListener(new RequestInitialFocusListener());
		field.setCaret(caret);
		if (initialText != null) {
			field.selectAll();
		}
		content.add(field);
		
		if (!(help == null || help.isEmpty())) {
			content.add(new JHelpLabel(help));
		}
		
		JPanel container = new JPanel(new GridBagLayout());
		container.add(content);
		
		int result = JOptionPane.showConfirmDialog(parent, container, title, JOptionPane.OK_CANCEL_OPTION, type);
		return result == JOptionPane.OK_OPTION ? field.getText() : null;
	}
}