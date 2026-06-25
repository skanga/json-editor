package com.skanga.jsoneditor.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * This class provides utility functions for retrieving GitHub Repository data.
 */
public final class GithubRepoUtil {
	private final static Logger log = LoggerFactory.getLogger(GithubRepoUtil.class);
	private final static Gson gson = new Gson();
	final static int HTTP_TIMEOUT_MILLIS = 10_000;
	private final static ExecutorService executor;
	
	static {
		executor = Executors.newCachedThreadPool(r -> {
			Thread t = new Thread(r, "github-version-check");
			t.setDaemon(true);
			return t;
		});
	}
	
	/**
	 * Gets the full GitHub Repository URL.
	 * 
	* @param 	username the GitHub Repository username.
	 * @param 	project the GitHub Repository project name.
	 * @return	the full GitHub Repository URL.
	 */
	public static String getURL(String username, String project) {
		return "https://github.com/" + username + "/" + project;
	}
	
	/**
	 * Gets the latest release data of a GitHub Repository.
	 * 
	 * @param 	username the GitHub Repository username.
	 * @param 	project the GitHub Repository project name.
	 * @return	the latest GitHub Repository release data.
	 */
	public static Future<GithubRepoReleaseData> getLatestRelease(String username, String project) {
		return executor.submit(() -> {
			HttpURLConnection connection = null;
			try {
				URL url = URI.create("https://api.github.com/repos/" + username + "/" + project + "/releases/latest").toURL();
				connection = (HttpURLConnection)url.openConnection();
				configureConnection(connection);
				try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
					return gson.fromJson(reader, GithubRepoReleaseData.class);
				}
			} catch (IOException e) {
				log.error("Unable to retrieve latest github release data", e);
				return null;
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
		});
	}

	static void configureConnection(HttpURLConnection connection) {
		connection.setConnectTimeout(HTTP_TIMEOUT_MILLIS);
		connection.setReadTimeout(HTTP_TIMEOUT_MILLIS);
	}
	
	/**
	 * This class represents GitHub Repository release data.
	 */
	public record GithubRepoReleaseData(
			@SerializedName("tag_name") String tagName,
			@SerializedName("html_url") String htmlUrl) {

		public String getTagName() {
			return tagName;
		}
		
		public String getHtmlUrl() {
			return htmlUrl;
		}
	}
}
