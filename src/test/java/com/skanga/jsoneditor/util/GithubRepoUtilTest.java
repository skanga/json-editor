package com.skanga.jsoneditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.jupiter.api.Test;

public class GithubRepoUtilTest {
	@Test
	public void configureConnectionSetsConnectAndReadTimeouts() throws IOException {
		TestHttpURLConnection connection = new TestHttpURLConnection();

		GithubRepoUtil.configureConnection(connection);

		assertEquals(GithubRepoUtil.HTTP_TIMEOUT_MILLIS, connection.getConnectTimeout());
		assertEquals(GithubRepoUtil.HTTP_TIMEOUT_MILLIS, connection.getReadTimeout());
	}

	private static class TestHttpURLConnection extends HttpURLConnection {
		private TestHttpURLConnection() throws IOException {
			super(URI.create("https://example.invalid/releases/latest").toURL());
		}

		@Override
		public void disconnect() {
		}

		@Override
		public boolean usingProxy() {
			return false;
		}

		@Override
		public void connect() {
		}
	}
}
