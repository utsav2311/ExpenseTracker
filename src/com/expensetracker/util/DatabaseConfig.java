package com.expensetracker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads database configuration from db.properties file or environment variables.
 * Demonstrates configuration management and sensible defaults.
 */
public final class DatabaseConfig {

    private static final String DEFAULT_STORAGE_MODE = "mysql";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/expense_tracker_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    private static String storageMode = DEFAULT_STORAGE_MODE;
    private static String url = DEFAULT_URL;
    private static String user = DEFAULT_USER;
    private static String password = DEFAULT_PASSWORD;
    private static String driver = DEFAULT_DRIVER;

    static {
        loadProperties();
    }

    private DatabaseConfig() {
    }

    public static void reload() {
        loadProperties();
    }

    private static void loadProperties() {
        Properties props = new Properties();
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("[DatabaseConfig] Could not read db.properties, using defaults: " + e.getMessage());
            }
        }

        storageMode = getEnvOrProp(props, "storage.mode", "STORAGE_MODE", DEFAULT_STORAGE_MODE);
        url = getEnvOrProp(props, "db.url", "DB_URL", DEFAULT_URL);
        user = getEnvOrProp(props, "db.user", "DB_USER", DEFAULT_USER);
        if (props.containsKey("db.username")) {
            user = props.getProperty("db.username");
        }
        password = getEnvOrProp(props, "db.password", "DB_PASSWORD", DEFAULT_PASSWORD);
        driver = getEnvOrProp(props, "db.driver", "DB_DRIVER", DEFAULT_DRIVER);
    }

    private static String getEnvOrProp(Properties props, String propKey, String envKey, String defVal) {
        String env = System.getenv(envKey);
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        String val = props.getProperty(propKey);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        return defVal;
    }

    public static String getStorageMode() {
        return storageMode;
    }

    public static boolean isMemoryModeExplicitlyRequested() {
        return "memory".equalsIgnoreCase(storageMode);
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }

    public static String getPassword() {
        return password;
    }

    public static String getDriver() {
        return driver;
    }
}
