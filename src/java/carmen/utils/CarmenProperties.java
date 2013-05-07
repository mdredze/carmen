// Copyright 2012-2013 Mark Dredze. All rights reserved.
// This software is released under the 2-clause BSD license.
// Mark Dredze, mdredze@cs.jhu.edu

// This class is based on CarmenProperties in https://github.com/vandurme/Carmen

package carmen.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


public class CarmenProperties {

  private static final Logger logger = Logger.getLogger(CarmenProperties.class);

  static Properties properties;
  static boolean isLoaded = false;
  static Hashtable<String, String> variables;

  static Pattern variablePattern = Pattern.compile("\\{[^\\\\}]+\\}");

  private static String parsePropertyValue(String value) throws IOException {
    String group, replacement;
    Matcher m = variablePattern.matcher(value);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      group = m.group();
      group = group.substring(1, group.length() - 1);
      replacement = CarmenProperties.getString(group, null);
      if (replacement != null)
        m.appendReplacement(sb, replacement);
      else {
        logger
            .warn("Cannot parse property [" + value + "], as [" + group + "] does not resolve");
        return null;
      }
    }
    m.appendTail(sb);
    return sb.toString();
  }

  /**
   * Calls {@link #load(String) load()} with the property
   * CarmenProperties.filename as the argument.
   * 
   * @throws IOException
   */
  public static void load() throws IOException {
    String filename = System.getProperty("carmen.properties");
    if (filename == null)
    	filename = CarmenProperties.class.getClassLoader().getResource("carmen.properties").getPath();
    logger.info("Loading properties file: " + filename);
    if (filename != null) 
      load(filename);

    isLoaded = true;
  }

  /**
   * Loads a static {@link Properties} object.
   * 
   * @param filename - the name of the file to be loaded in the properties object
   * @throws IOException
   */
  public static void load(String filename) throws IOException {
    logger.info("Reading CarmenProperty file [" + filename + "]");
    if (properties == null) properties = new java.util.Properties();
    properties.load(new InputStreamReader(new FileInputStream(new File(filename)), "UTF-8"));
    isLoaded = true;
  }

  public static double getDouble(String key) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null)
      throw new IOException("Key not found in property specification: [" + key + "]");
    if ((value = parsePropertyValue(value)) == null)
      throw new IOException("Key not resolvable in property specification: [" + key + "]");

    else
      return Double.parseDouble(value);
  }

  public static double getDouble(String key, double defaultValue) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null) {
      logger.info("Returning default value for " + key + " : " + defaultValue);
      return defaultValue;
    }
    if ((value = parsePropertyValue(value)) == null) {
      logger.info("Key not fully resolvable, returning default value for " + key + " : "
          + defaultValue);
      return defaultValue;
    } else
      return Double.parseDouble(value);
  }

  public static long getLong(String key) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null)
      throw new IOException("Key not found in property specification: [" + key + "]");
    if ((value = parsePropertyValue(value)) == null)
      throw new IOException("Key not resolvable in property specification: [" + key + "]");

    return Long.parseLong(value);
  }

  public static long getLong(String key, long defaultValue) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null) {
      logger.info("Returning default value for " + key + " : " + defaultValue);
      return defaultValue;
    }
    if ((value = parsePropertyValue(value)) == null) {
      logger.info("Key not fully resolvable, returning default value for " + key + " : "
          + defaultValue);
      return defaultValue;
    } else
      return Long.parseLong(value);
  }

  public static int getInt(String key) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null)
      throw new IOException("Key not found in property specification: [" + key + "]");
    if ((value = parsePropertyValue(value)) == null)
      throw new IOException("Key not resolvable in property specification: [" + key + "]");

    logger.info("Returning value for " + key + " : " + value);
    return Integer.parseInt(value);
  }

  public static int getInt(String key, int defaultValue) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null) {
      logger.info("Returning default value for " + key + " : " + defaultValue);
      return defaultValue;
    }
    if ((value = parsePropertyValue(value)) == null) {
      logger.info("Key not fully resolvable, returning default value for " + key + " : "
          + defaultValue);
      return defaultValue;
    } else
      return Integer.parseInt(value);
  }

  public static String getString(String key, String defaultValue) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null) {
      logger.info("Returning default value for " + key + " : " + defaultValue);
      return defaultValue;
    }
    if ((value = parsePropertyValue(value)) == null) {
      logger.info("Key not fully resolvable, returning default value for " + key + " : "
          + defaultValue);
      return defaultValue;
    } else
      return value;
  }

  public static String getString(String key) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null)
      throw new IOException("Key not found in property specification: [" + key + "]");
    if ((value = parsePropertyValue(value)) == null)
      throw new IOException("Key not resolvable in property specification: [" + key + "]");

    return value;
  }

  public static String[] getStrings(String key, String[] defaultValue) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null) {
      logger.info("Returning default value for " + key + " : " + Arrays.toString(defaultValue));
      return defaultValue;
    }
    if ((value = parsePropertyValue(value)) == null) {
      logger.info("Key not fully resolvable, returning default value for " + key + " : "
          + Arrays.toString(defaultValue));
      return defaultValue;
    } else
      return value.split("\\s");
  }

  public static String[] getStrings(String key) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null)
      throw new IOException("Key not found in property specification: [" + key + "]");
    if ((value = parsePropertyValue(value)) == null)
      throw new IOException("Key not resolvable in property specification: [" + key + "]");

    return value.split("\\s");
  }

  public static boolean getBoolean(String key, boolean defaultValue) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null) {
      logger.info("Returning default value for " + key + " : " + defaultValue);
      return defaultValue;
    }
    if ((value = parsePropertyValue(value)) == null) {
      logger.info("Key not fully resolvable, returning default value for " + key + " : "
          + defaultValue);
      return defaultValue;
    } else
      return Boolean.valueOf(value);
  }

  public static boolean getBoolean(String key) throws IOException {
    if (!isLoaded) load();

    String value = System.getProperty(key);
    if (value == null && properties != null) value = properties.getProperty(key);
    if (value == null)
      throw new IOException("Key not found in property specification: [" + key + "]");
    if ((value = parsePropertyValue(value)) == null)
      throw new IOException("Key not resolvable in property specification: [" + key + "]");
    return Boolean.valueOf(value);
  }
}