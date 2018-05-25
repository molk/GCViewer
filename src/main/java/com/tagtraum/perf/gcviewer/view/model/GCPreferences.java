package com.tagtraum.perf.gcviewer.view.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

/**
 * Holds preferences of GCViewer and can load / store them from / in a file.
 * 
 * @author <a href="mailto:gcviewer@gmx.ch">Joerg Wuethrich</a>
 * <p>created on: 20.11.2011</p>
 */
public class GCPreferences {

    private static File preferencesFile() {
        return new File(System.getProperty("user.home"),  ".gcviewer.properties");
    }

    public static final String FULL_GC_LINES = "fullgclines";
    public static final String INC_GC_LINES = "incgclines";
    public static final String GC_TIMES_LINE = "gctimesline";
    public static final String GC_TIMES_RECTANGLES = "gctimesrectangles";
    public static final String TOTAL_MEMORY = "totalmemory";
    public static final String USED_MEMORY = "usedmemory";
    public static final String USED_YOUNG_MEMORY = "usedyoungmemory";
    public static final String USED_TENURED_MEMORY = "usedtenuredmemory";
    public static final String TENURED_MEMORY = "tenuredmemory";
    public static final String YOUNG_MEMORY = "youngmemory";
    public static final String INITIAL_MARK_LEVEL = "initialmarklevel";
    public static final String CONCURRENT_COLLECTION_BEGIN_END = "concurrentcollectionbeginend";
    public static final String ANTI_ALIAS = "antialias";
    
    private static final String SHOW_DATA_PANEL = "showdatapanel";
    public static final String SHOW_DATE_STAMP = "showdatestamp";
    public static final String SHOW_MODEL_METRICS_PANEL = "showmodelmetricspanel";
    
    private static final String GC_LINE_PREFIX = "view.";
    
    private static final String WINDOW_WIDTH = "window.width";
    private static final String WINDOW_HEIGHT = "window.height";
    private static final String WINDOW_X = "window.x";
    private static final String WINDOW_Y = "window.y";
    private static final String LASTFILE = "lastfile";
    private static final String RECENT_FILE_PREFIX = "recent.";
    
    private static final int WINDOW_WIDTH_DEFAULT = 800;
    private static final int WINDOW_HEIGHT_DEFAULT = 600;
    private static final int WINDOW_X_DEFAULT = 0;
    private static final int WINDOW_Y_DEFAULT = 0;
    
    private static final Logger LOGGER = Logger.getLogger(GCPreferences.class.getName());

    private final Properties properties = new Properties();

    private boolean propertiesLoaded = false;
    
    /**
     * Save properties to a file.
     */
    public void store() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(preferencesFile()))) {
            properties.store(writer, "GCViewer preferences");
        }
        catch (IOException e) {
			LOGGER.warning(() ->"could not store preferences (" + e + ")");
        }
    }
    
    /**
     * Loads properties from a file.
     */
    public GCPreferences load() {
        if (preferencesFile().exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(preferencesFile()))) {
                properties.load(reader);
                propertiesLoaded = true;
            }
            catch (IOException e) {
				LOGGER.warning(() -> "could not load preferences (" + e + ")");
            }
        }
        return this;
    }
    
    public boolean isPropertiesLoaded() {
        return propertiesLoaded;
    }
    
    /**
     * Replace all currently held preferences with preferences stored in <code>gcPreferences</code>.
     * @param gcPreferences set of preferences to copy from
     */
    public void setTo(GCPreferences gcPreferences) {
        this.properties.clear();
        this.properties.putAll(gcPreferences.properties);
    }
    
    private boolean getBooleanProperty(String key) {
        return getBooleanValue(key, true);
    }
    
    public void setBooleanProperty(String key, boolean value) {
        properties.setProperty(key, Boolean.toString(value));
    }
    
    public int getWindowWidth() {
        return getIntValue(WINDOW_WIDTH, WINDOW_WIDTH_DEFAULT);
    }
    
    public void setWindowWidth(int value) {
        properties.setProperty(WINDOW_WIDTH, Integer.toString(value));
    }
    
    public int getWindowHeight() {
        return getIntValue(WINDOW_HEIGHT, WINDOW_HEIGHT_DEFAULT);
    }
    
    public void setWindowHeight(int value) {
        properties.setProperty(WINDOW_HEIGHT, Integer.toString(value));
    }
    
    public int getWindowX() {
        return getIntValue(WINDOW_X, WINDOW_X_DEFAULT);
    }
    
    public void setWindowX(int value) {
        properties.setProperty(WINDOW_X, Integer.toString(value));
    }
    
    public int getWindowY() {
        return getIntValue(WINDOW_Y, WINDOW_Y_DEFAULT);
    }
    
    public void setWindowY(int value) {
        properties.setProperty(WINDOW_Y, Integer.toString(value));
    }
    
    public void setLastFile(String filename) {
        properties.setProperty(LASTFILE, filename);
    }
    
    public String getLastFile() {
        return properties.getProperty(LASTFILE);
    }
    
    public void setRecentFiles(List<String> fileNames) {
		range(0, fileNames.size()).forEach(i -> properties.setProperty(RECENT_FILE_PREFIX + i, fileNames.get(i)));
    }
    
    public List<String> getRecentFiles() {
		return properties.entrySet().stream()
			.filter(e -> String.valueOf(e.getKey()).startsWith(RECENT_FILE_PREFIX))
			.map(e -> String.valueOf(e.getValue()))
			.collect(toList());
    }
    
    public boolean getGcLineProperty(String key) {
        return getBooleanValue(GC_LINE_PREFIX + key, true);
    }
    
    public boolean getGcLineProperty(String key, boolean defaultValue) {
        return getBooleanValue(GC_LINE_PREFIX + key, defaultValue);
    }

    public void setGcLineProperty(String key, boolean value) {
        properties.setProperty(GC_LINE_PREFIX + key, Boolean.toString(value));
    }
    
    private boolean getBooleanValue(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(defaultValue)));
    }
    
    private int getIntValue(String key, int defaultValue) {
        int result = defaultValue;
        try {
            result = Integer.parseInt(properties.getProperty(key));
        }
        catch (NumberFormatException e) {
            LOGGER.info(() -> "could not read property '" + key + "' from " + preferencesFile().getAbsolutePath() + "; using default: " + defaultValue);
        }
        
        return result;
    }

    public void setShowDataPanel(boolean showDataPanel) {
        setBooleanProperty(GC_LINE_PREFIX + SHOW_DATA_PANEL, showDataPanel);
    }

    public boolean isShowDataPanel() {
        return getBooleanProperty(GC_LINE_PREFIX + SHOW_DATA_PANEL);
    }

    public boolean isShowDateStamp() {
        return getBooleanProperty(GC_LINE_PREFIX + SHOW_DATE_STAMP);
    }

    public void setShowDateStamp(boolean showDateStamp) {
        setBooleanProperty(GC_LINE_PREFIX + SHOW_DATE_STAMP, showDateStamp);
    }

    public void setShowModelMetricsPanel(boolean showModelMetricsPanel) {
        setBooleanProperty(GC_LINE_PREFIX + SHOW_MODEL_METRICS_PANEL, showModelMetricsPanel);
    }
    
    public boolean isShowModelMetricsPanel() {
        return getBooleanProperty(GC_LINE_PREFIX + SHOW_MODEL_METRICS_PANEL);
    }

}
