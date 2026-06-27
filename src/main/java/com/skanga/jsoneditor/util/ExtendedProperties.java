package com.skanga.jsoneditor.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends {@link Properties}.
 * 
 * <p>This implementation adds the ability to load and store properties from a given file path  
 * and adds support to retrieve and store {@code Integer} and {@code List} values.</p>
 * 
 */
public class ExtendedProperties extends Properties {
	@Serial
    private final static long serialVersionUID = 6042931434040718478L;
	private final static Logger log = LoggerFactory.getLogger(ExtendedProperties.class);
	private final String listSeparator;
	
	/**
     * Creates an empty property list with no default values and "," as list separator.
     */
	public ExtendedProperties() {
		this(null, ",");
	}
	
	/**
     * Creates an empty property list with no default values and the specified list separator.
     * 
     * @param	separator the separator used for storing list values.
     */
	public ExtendedProperties(String separator) {
		this(null, separator);
	}
	
	/**
     * Creates an empty property list with the specified defaults and "," as list separator.
     * 
     * @param	defaults the defaults.
     */
    public ExtendedProperties(Properties defaults) {
        this(defaults, ",");
    }
	
	/**
     * Creates an empty property list with the specified defaults and list separator.
     * 
     * @param	defaults the defaults.
     * @param	separator the separator used for storing list values.
     */
    public ExtendedProperties(Properties defaults, String separator) {
        super(defaults);
        this.listSeparator = separator;
    }
	
	/**
	 * Reads a property list from the given file path.
	 * 
	 * <p>Any {@code IOException} will be ignored.</p>
	 * 
	 * @param 	path the path to the property file.
	 */
	public void load(Path path) {
		try (InputStream in = Files.newInputStream(path)) {
			load(in);
		} catch (IOException e) {
			log.error("Unable to load properties from " + path, e);
		}
	}
	
	/**
	 * Writes the property list to the given file path.
	 * 
	 * <p>Any {@code IOException} will be ignored.</p>
	 * 
	 * @param 	path the path to the property file.
	 */
	public void store(Path path) {
		try (OutputStream out = new OutputStreamWrapper(Files.newOutputStream(path))) {
			store(out, null);
		} catch (IOException e) {
			log.error("Unable to store properties to " + path, e);
		}
	}
	
	/**
	 * Sets a value in the property list. The list of values will be converted
	 * to a single string separated by a listSeparator
	 * 
	 * @param 	key the key to be placed in this property list.
	 * @param 	values the value corresponding to {@code key}.
	 */
	public void setProperty(String key, List<String> values) {
		setProperty(key, String.join(listSeparator, values));
	}
	
	/**
	 * Sets a value in the property list. The {@code Integer} value will be 
	 * stored as a {@code String} value.
	 * 
	 * @param 	key the key to be placed in this property list.
	 * @param 	value the value corresponding to {@code key}.
	 */
	public void setProperty(String key, Integer value) {
		setProperty(key, value == null ? null : value.toString());
	}
	
	/**
	 * Sets a value in the property list. The {@code boolean} value will be 
	 * stored as a {@code String} value; "1" for {@code true}, "0" for {@code false}.
	 * 
	 * @param 	key the key to be placed in this property list.
	 * @param 	value the value corresponding to {@code key}.
	 */
	public void setProperty(String key, Boolean value) {
		setProperty(key, value == null ? null : (value ? 1 : 0));
	}

	/**
	 * Sets a value in the property list. The {@code Locale} value will be 
	 * stored as a {@code String} value.
	 * 
	 * @param 	key the key to be placed in this property list.
	 * @param 	locale the value corresponding to {@code key}.
	 */
	public void setProperty(String key, Locale locale) {
		setProperty(key, locale.toString());
	}
	
	/**
	 * Gets a value from the property list as a {@code List}. This method should be used
	 * to retrieve a value previously stored by {@link #setProperty(String, List)}.
	 * 
	 * @param 	key the property key.
	 * @return 	the value in this property list with the specified key value or an empty list
	 * 			if no such key exists.
	 */
	public List<String> getListProperty(String key) {
		String value = getProperty(key);
		return value == null || value.isEmpty()
				? new LinkedList<>()
				: new LinkedList<>(Arrays.asList(value.split(listSeparator)));
	}
	
	/**
	 * Gets a value from the property list as an {@code Integer}. This method should be used 
	 * to retrieve a value previously stored by {@link #setProperty(String, Integer)}.
	 * 
	 * @param 	key the property key.
	 * @return 	the value in this property list with the specified key value or {@code null}
	 * 			if no such key exists or the value is not a valid {@code Integer}.
	 */
	public Integer getIntegerProperty(String key) {
		String value = getProperty(key);
		if (!(value == null || value.isEmpty())) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				log.debug("Unable to parse integer property value " + value);
			}
		}
		return null;
	}
	
	/**
	 * See {@link #getIntegerProperty(String)}. This method returns {@code defaultValue} when
	 * there is no value in the property list with the specified {@code key} or when 
	 * the value is not a valid {@code Integer}.
	 * 
	 * @param	key the property key.
	 * @param	defaultValue the default value to return when there is no value for the specified key
	 * @return 	the value in this property list with the specified key value or {@code defaultValue}
	 * 			if no such key exists or the value is not a valid {@code Integer}.
	 */
	public Integer getIntegerProperty(String key, Integer defaultValue) {
		Integer value = getIntegerProperty(key); 
		return value != null ? value : defaultValue;
	}
	
	/**
	 * Gets a value from the property list as an {@code Boolean}. This method should be used 
	 * to retrieve a value previously stored by {@link #setProperty(String, Boolean)}.
	 * 
	 * @param 	key the property key.
	 * @return 	the value in this property list with the specified key value or {@code null}
	 * 			if no such key exists.
	 */
	public Boolean getBooleanProperty(String key) {
		Integer value = getIntegerProperty(key);
		return value != null ? (value != 0) : null;
	}
	
	/**
	 * See {@link #getBooleanProperty(String)}. This method returns {@code defaultValue} when
	 * there is no value in the property list with the specified {@code key}.
	 * 
	 * @param 	key the property key.
	 * @param	defaultValue the default value to return when there is no value for the specified key
	 * @return 	the value in this property list with the specified key value or {@code defaultValue}
	 * 			if no such key exists.
	 */
	public Boolean getBooleanProperty(String key, boolean defaultValue) {
		Boolean value = getBooleanProperty(key);
		return value != null ? value : defaultValue;
	}

	/**
	 * Gets a value from the property list as an {@code Locale}. This method should be used 
	 * to retrieve a value previously stored by {@link #setProperty(String, Locale)}.
	 * 
	 * @param 	key the property key.
	 * @return 	the value in this property list with the specified key value or {@code null}
	 * 			if no such key exists.
	 */
	public Locale getLocaleProperty(String key) {
		String value = getProperty(key);
		if (value == null || value.isEmpty()) return null;
		String[] parts = value.split("[_-]", 3);
		if (parts.length == 1) return Locale.of(parts[0]);
		if (parts.length == 2) return Locale.of(parts[0], parts[1]);
		return Locale.of(parts[0], parts[1], parts[2]);
	}
	
	/**
	 * This method returns {@code defaultValue} when there is no value in the property list with the
	 * specified {@code key}.
	 * 
	 * @param 	key the property key.
	 * @param	defaultValue the default value to return when there is no value for the specified key
	 * @return 	the value in this property list with the specified key value or {@code defaultValue}
	 * 			if no such key exists.
	 */
	public Locale getLocaleProperty(String key, Locale defaultValue) {
		Locale value = getLocaleProperty(key);
		return value != null ? value : defaultValue;
	}

	@Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(new TreeSet<>(super.keySet()));
    }
	
	private static class OutputStreamWrapper extends FilterOutputStream {
        private boolean firstlineseen = false;
        
        public OutputStreamWrapper(OutputStream out) {
            super(out);
        }
        
        @Override
        public void write(int b) throws IOException {
            if (firstlineseen) {
                super.write(b);
            } else if (b == '\n') {
                firstlineseen = true;
            }
        }
    }
}
