package com.skanga.jsoneditor.io;

import java.io.IOException;
import java.io.Serial;

public class ChecksumException extends IOException {
	@Serial
    private final static long serialVersionUID = -5164866588227844439L;

	/**
     * Constructs an {@code ChecksumException} with {@code null}
     * as its error detail message.
     */
    public ChecksumException() {
        super();
    }

    /**
     * Constructs an {@code ChecksumException} with the specified detail message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method)
     */
    public ChecksumException(String message) {
        super(message);
    }
}
