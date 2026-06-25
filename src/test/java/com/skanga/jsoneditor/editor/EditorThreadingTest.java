package com.skanga.jsoneditor.editor;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.skanga.jsoneditor.util.MessageBundle;

public class EditorThreadingTest {
	@Test
	public void importProjectErrorDialogIsReportedOnCallingThread() {
		MessageBundle.loadResources();
		Thread callingThread = Thread.currentThread();
		AtomicReference<Thread> reportingThread = new AtomicReference<>();
		AtomicReference<String> message = new AtomicReference<>();

		Editor.showImportProjectError(Path.of("missing.json"), true, error -> {
			reportingThread.set(Thread.currentThread());
			message.set(error);
		});

		assertSame(callingThread, reportingThread.get());
		assertTrue(message.get().contains("missing.json"));
	}
}
