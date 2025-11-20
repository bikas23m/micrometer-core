package com.orosoft.ntp.monitoring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class for Health Monitoring Module
 *
 * @author bikas
 */
public class HealthConfig {

    private static final Logger logger = LoggerFactory.getLogger(HealthConfig.class);

    private static final String DEFAULT_CONFIG_FILE = "health-monitor.properties";

    // Default values (Convention over Configuration)
    private int serverPort = 8081;
    private String metricsEndpoint = "/metrics";
    private String healthEndpoint = "/health";
    private boolean enableServer = true;
    private boolean enableJvmMetrics = true;
    private boolean enableGcMetrics = true;
    private boolean enableThreadMetrics = true;
    private boolean enableMemoryMetrics = true;
    private boolean enableClassLoaderMetrics = true;
    private boolean enableProcessorMetrics = true;
    private String applicationName = "java-app";

    public HealthConfig() {
        loadConfiguration();
    }

    public static String getProperty(String baseKey, String defaultValue, Properties props) {
        String[] variants = {
                baseKey, // original: health.monitor.server.port
                baseKey.replace('.', '_').toUpperCase(), // HEALTH_MONITOR_SERVER_PORT
                baseKey.replace('.', '_'), // health_monitor_server_port
                toCamelCase(baseKey), // healthMonitorServerPort
        };

        for (String key : variants) {
            String val = System.getProperty(key);
            if (val != null && !val.trim().isEmpty()) return val;
        }
        for (String key : variants) {
            String val = System.getenv(key);
            if (val != null && !val.trim().isEmpty()) return val;
        }
        for (String key : variants) {
            String val = props.getProperty(key);
            if (val != null && !val.trim().isEmpty()) return val;
        }
        return defaultValue;
    }

    // Helper to convert "health.monitor.server.port" to "healthMonitorServerPort"
    static String toCamelCase(String key) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : key.toCharArray()) {
            if (c == '.' || c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void loadConfiguration() {
        // 1. Load from properties file (if exists)
        Properties prop = loadFromPropertiesFile();
        enableServer = Boolean.parseBoolean(prop.getProperty(PropertyKeys.SERVER_ENABLED,
                String.valueOf(enableServer)));
        applicationName = prop.getProperty(PropertyKeys.APPLICATION_NAME, applicationName);
        serverPort = Integer.parseInt(getProperty(PropertyKeys.SERVER_PORT, String.valueOf(serverPort), prop));
        metricsEndpoint = getProperty(PropertyKeys.METRICS_ENDPOINT, metricsEndpoint, prop);
        healthEndpoint = getProperty(PropertyKeys.HEALTH_ENDPOINT, healthEndpoint, prop);

        // Metric toggles
        enableJvmMetrics = Boolean.parseBoolean(getProperty(
                PropertyKeys.JVM_METRICS_ENABLED, String.valueOf(enableJvmMetrics), prop));
        enableGcMetrics = Boolean.parseBoolean(getProperty(
                PropertyKeys.GC_METRICS_ENABLED, String.valueOf(enableGcMetrics), prop));
        enableThreadMetrics = Boolean.parseBoolean(getProperty(
                PropertyKeys.THREAD_METRICS_ENABLED, String.valueOf(enableThreadMetrics), prop));
        enableMemoryMetrics = Boolean.parseBoolean(getProperty(
                PropertyKeys.MEMORY_METRICS_ENABLED, String.valueOf(enableMemoryMetrics), prop));
        enableClassLoaderMetrics = Boolean.parseBoolean(getProperty(
                PropertyKeys.CLASSLOADER_METRICS_ENABLED, String.valueOf(enableClassLoaderMetrics), prop));
        enableProcessorMetrics = Boolean.parseBoolean(getProperty(
                PropertyKeys.PROCESSOR_METRICS_ENABLED, String.valueOf(enableProcessorMetrics), prop));
    }

    private Properties loadFromPropertiesFile() {
        Properties prop = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (input != null) {
                prop.load(input);
            }
        } catch (IOException ex) {
            // Properties file not found - use defaults
            logger.debug("Health monitor properties file not found, using defaults");
        }
        return prop;
    }

    // Fluent setters for programmatic configuration
    public HealthConfig serverPort(int port) {
        this.serverPort = port;
        return this;
    }

    public HealthConfig metricsEndpoint(String endpoint) {
        this.metricsEndpoint = endpoint;
        return this;
    }

    public HealthConfig healthEndpoint(String endpoint) {
        this.healthEndpoint = endpoint;
        return this;
    }

    public HealthConfig enableServer(boolean enable) {
        this.enableServer = enable;
        return this;
    }

    public HealthConfig applicationName(String name) {
        this.applicationName = name;
        return this;
    }

    public HealthConfig enableJvmMetrics(boolean enable) {
        this.enableJvmMetrics = enable;
        return this;
    }

    public HealthConfig enableGcMetrics(boolean enable) {
        this.enableGcMetrics = enable;
        return this;
    }

    // Getters
    public int getServerPort() {
        return serverPort;
    }

    public String getMetricsEndpoint() {
        return metricsEndpoint;
    }

    public String getHealthEndpoint() {
        return healthEndpoint;
    }

    public boolean isEnableServer() {
        return enableServer;
    }

    public boolean isEnableJvmMetrics() {
        return enableJvmMetrics;
    }

    public boolean isEnableGcMetrics() {
        return enableGcMetrics;
    }

    public boolean isEnableThreadMetrics() {
        return enableThreadMetrics;
    }

    public boolean isEnableMemoryMetrics() {
        return enableMemoryMetrics;
    }

    public boolean isEnableClassLoaderMetrics() {
        return enableClassLoaderMetrics;
    }

    public boolean isEnableProcessorMetrics() {
        return enableProcessorMetrics;
    }

    public String getApplicationName() {
        return applicationName;
    }
}
