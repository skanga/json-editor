package com.skanga.jsoneditor.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import org.junit.jupiter.api.Test;

public class JFileDropJsonDocumentTest {
	@Test
	public void readerFlavorDropClosesTransferReader() throws IOException, UnsupportedFlavorException {
		AtomicBoolean closed = new AtomicBoolean();
		DataFlavor flavor = new DataFlavor("text/uri-list;class=java.io.Reader", "URI list");
		StringReader reader = new StringReader("file:///tmp/data.json") {
			@Override
			public void close() {
				closed.set(true);
				super.close();
			}
		};
		Transferable transferable = new Transferable() {
			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] { flavor };
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor candidate) {
				return flavor.equals(candidate);
			}

			@Override
			public Object getTransferData(DataFlavor candidate) {
				return reader;
			}
		};

		File[] files = JFileDrop.createFileArray(flavor, transferable);

		assertEquals(new File("/tmp/data.json"), files[0]);
		assertTrue(closed.get(), "reader-flavor drops must close the transfer reader");
	}

	@Test
	public void nestedDropTargetsRestoreTheirOwnOriginalBorders() {
		JPanel parent = new JPanel();
		JPanel child = new JPanel();
		Border parentBorder = new MatteBorder(1, 1, 1, 1, Color.RED);
		Border childBorder = new MatteBorder(2, 2, 2, 2, Color.GREEN);
		Border dragBorder = new MatteBorder(3, 3, 3, 3, Color.BLUE);
		parent.setBorder(parentBorder);
		child.setBorder(childBorder);

		JFileDrop.saveNormalBorder(parent, dragBorder);
		JFileDrop.saveNormalBorder(child, dragBorder);
		JFileDrop.restoreNormalBorder(parent);
		JFileDrop.restoreNormalBorder(child);

		assertSame(parentBorder, parent.getBorder());
		assertSame(childBorder, child.getBorder());
	}
}
