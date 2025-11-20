package com.orosoft.ntp.monitoring.config;

class PropertyKeys {
    // Server Config
    public static final String SERVER_PORT = "health.monitor.server.port";
    public static final String SERVER_ENABLED = "health.monitor.server.enabled";
    public static final String METRICS_ENDPOINT = "health.monitor.metrics.endpoint";
    public static final String HEALTH_ENDPOINT = "health.monitor.health.endpoint";

    // Application
    public static final String APPLICATION_NAME = "health.monitor.application.name";

    // Metrics Toggles
    public static final String JVM_METRICS_ENABLED = "health.monitor.metrics.jvm.enabled";
    public static final String GC_METRICS_ENABLED = "health.monitor.metrics.gc.enabled";
    public static final String THREAD_METRICS_ENABLED = "health.monitor.metrics.thread.enabled";
    public static final String MEMORY_METRICS_ENABLED = "health.monitor.metrics.memory.enabled";
    public static final String CLASSLOADER_METRICS_ENABLED = "health.monitor.metrics.classloader.enabled";
    public static final String PROCESSOR_METRICS_ENABLED = "health.monitor.metrics.processor.enabled";

    private PropertyKeys() { } // Prevent instantiation
}
