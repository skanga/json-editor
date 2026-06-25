package com.skanga.jsoneditor.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds file drag-and-drop support to AWT/Swing components.
 */
public class JFileDrop {
	private static final String NORMAL_BORDER_PROPERTY = JFileDrop.class.getName() + ".normalBorder";
	private static final Object NULL_BORDER = new Object();
	private static final Color DEFAULT_BORDER_COLOR = new Color(0f, 0f, 1f, 0.25f);
	private static final String ZERO_CHAR_STRING = "" + (char) 0;
	private static final Logger log = LoggerFactory.getLogger(JFileDrop.class);

	private static Boolean supportsDnD;

	private transient DropTargetAdapter dropListener;

	public JFileDrop(final Component c, final Listener listener) {
		this(c, defaultBorder(), true, listener);
	}

	public JFileDrop(final Component c, final Border dragBorder,
			final boolean recursive, final Listener listener) {
		if (!supportsDnD()) {
			log.debug("FileDrop: Drag and drop is not supported with this JVM");
			return;
		}

		dropListener = createDropListener(c, dragBorder, listener);
		makeDropTarget(c, recursive);
	}

	private static Border defaultBorder() {
		return BorderFactory.createMatteBorder(2, 2, 2, 2, DEFAULT_BORDER_COLOR);
	}

	private DropTargetAdapter createDropListener(Component component, Border dragBorder, Listener listener) {
		return new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetDragEvent evt) {
				debug("FileDrop: dragEnter event.");
				if (isDragOk(evt)) {
					if (component instanceof JComponent jComponent) {
						saveNormalBorder(jComponent, dragBorder);
						debug("FileDrop: normal border saved.");
						debug("FileDrop: drag border set.");
					}
					evt.acceptDrag(DnDConstants.ACTION_COPY);
					debug("FileDrop: event accepted.");
				} else {
					evt.rejectDrag();
					debug("FileDrop: event rejected.");
				}
			}

			@Override
			public void drop(DropTargetDropEvent evt) {
				debug("FileDrop: drop event.");
				try {
					handleDrop(evt, listener);
				} catch (IOException e) {
					log.warn("FileDrop: IOException - abort", e);
					evt.rejectDrop();
				} catch (UnsupportedFlavorException e) {
					log.warn("FileDrop: UnsupportedFlavorException - abort", e);
					evt.rejectDrop();
				} finally {
					if (component instanceof JComponent jComponent) {
						restoreNormalBorder(jComponent);
						debug("FileDrop: normal border restored.");
					}
				}
			}

			@Override
			public void dragExit(DropTargetEvent evt) {
				debug("FileDrop: dragExit event.");
				if (component instanceof JComponent jComponent) {
					restoreNormalBorder(jComponent);
					debug("FileDrop: normal border restored.");
				}
			}

			@Override
			public void dropActionChanged(DropTargetDragEvent evt) {
				debug("FileDrop: dropActionChanged event.");
				if (isDragOk(evt)) {
					evt.acceptDrag(DnDConstants.ACTION_COPY);
					debug("FileDrop: event accepted.");
				} else {
					evt.rejectDrag();
					debug("FileDrop: event rejected.");
				}
			}
		};
	}

	private static void handleDrop(DropTargetDropEvent evt, Listener listener)
			throws UnsupportedFlavorException, IOException {
		Transferable transferable = evt.getTransferable();
		if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			evt.acceptDrop(DnDConstants.ACTION_COPY);
			debug("FileDrop: file list accepted.");
			if (listener != null) {
				listener.filesDropped(toFileArray((List<?>) transferable.getTransferData(DataFlavor.javaFileListFlavor)));
			}
			evt.getDropTargetContext().dropComplete(true);
			debug("FileDrop: drop complete.");
			return;
		}

		for (DataFlavor flavor : transferable.getTransferDataFlavors()) {
			if (flavor.isRepresentationClassReader()) {
				evt.acceptDrop(DnDConstants.ACTION_COPY);
				debug("FileDrop: reader accepted.");
				if (listener != null) {
					listener.filesDropped(createFileArray(flavor, transferable));
				}
				evt.getDropTargetContext().dropComplete(true);
				debug("FileDrop: drop complete.");
				return;
			}
		}

		debug("FileDrop: not a file list or reader - abort.");
		evt.rejectDrop();
	}

	private static File[] toFileArray(List<?> fileList) {
		return fileList.stream()
				.filter(File.class::isInstance)
				.map(File.class::cast)
				.toArray(File[]::new);
	}

	static void saveNormalBorder(JComponent component, Border dragBorder) {
		if (component.getClientProperty(NORMAL_BORDER_PROPERTY) == null) {
			Border border = component.getBorder();
			component.putClientProperty(NORMAL_BORDER_PROPERTY, border == null ? NULL_BORDER : border);
		}
		component.setBorder(dragBorder);
	}

	static void restoreNormalBorder(JComponent component) {
		Object border = component.getClientProperty(NORMAL_BORDER_PROPERTY);
		if (border == null) {
			return;
		}
		component.setBorder(border == NULL_BORDER ? null : (Border) border);
		component.putClientProperty(NORMAL_BORDER_PROPERTY, null);
	}

	private static boolean supportsDnD() {
		if (supportsDnD == null) {
			try {
				Class.forName("java.awt.dnd.DnDConstants");
				supportsDnD = Boolean.TRUE;
			} catch (ClassNotFoundException e) {
				supportsDnD = Boolean.FALSE;
			}
		}
		return supportsDnD;
	}

	static File[] createFileArray(DataFlavor flavor, Transferable transferable)
			throws UnsupportedFlavorException, IOException {
		try (BufferedReader br = new BufferedReader(flavor.getReaderForText(transferable))) {
			return createFileArray(br);
		}
	}

	static File[] createFileArray(BufferedReader reader) {
		try {
			List<File> files = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || ZERO_CHAR_STRING.equals(line)) {
					continue;
				}

				File file = toFile(line);
				if (file != null) {
					files.add(file);
				} else {
					log.debug("Error with dropped path: {}", line);
				}
			}
			return files.toArray(File[]::new);
		} catch (IOException e) {
			log.debug("FileDrop: IOException", e);
			return new File[0];
		}
	}

	/**
	 * Converts a text/uri-list line or plain path into a file.
	 * Handles file URIs with unencoded characters such as spaces.
	 */
	static File toFile(String line) {
		if (line == null) {
			return null;
		}
		try {
			if (line.startsWith("file:")) {
				return toUriFile(line);
			}
			return new File(line);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static File toUriFile(String line) {
		try {
			return new File(new URI(line));
		} catch (URISyntaxException | IllegalArgumentException e) {
			String path = line.substring("file:".length());
			if (path.startsWith("//")) {
				int slash = path.indexOf('/', 2);
				path = slash >= 0 ? path.substring(slash) : path.substring(2);
			}
			return new File(URLDecoder.decode(path, StandardCharsets.UTF_8));
		}
	}

	private void makeDropTarget(Component component, boolean recursive) {
		installDropTarget(component);
		component.addHierarchyListener(evt -> {
			debug("FileDrop: Hierarchy changed.");
			if (component.getParent() == null) {
				component.setDropTarget(null);
				debug("FileDrop: Drop target cleared from component.");
			} else {
				installDropTarget(component);
				debug("FileDrop: Drop target added to component.");
			}
		});

		if (recursive && component instanceof Container container) {
			for (Component child : container.getComponents()) {
				makeDropTarget(child, true);
			}
		}
	}

	private void installDropTarget(Component component) {
		try {
			new DropTarget(component, dropListener);
		} catch (RuntimeException e) {
			log.warn("FileDrop: Drop will not work due to previous error. Do you have another listener attached?", e);
		}
	}

	private boolean isDragOk(DropTargetDragEvent evt) {
		DataFlavor[] flavors = evt.getCurrentDataFlavors();
		boolean ok = false;
		for (DataFlavor flavor : flavors) {
			if (DataFlavor.javaFileListFlavor.equals(flavor) || flavor.isRepresentationClassReader()) {
				ok = true;
				break;
			}
		}

		if (log.isDebugEnabled()) {
			if (flavors.length == 0) {
				debug("FileDrop: no data flavors.");
			}
			for (DataFlavor flavor : flavors) {
				debug(flavor.toString());
			}
		}
		return ok;
	}

	private static void debug(String message) {
		log.debug(message);
	}

	@FunctionalInterface
	public interface Listener {
		void filesDropped(File[] files);
	}
}
